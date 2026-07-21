package com.company.daizhang.common.crypto.util;

import com.company.daizhang.common.crypto.config.CryptoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 加密工具 (P4.1)
 * <p>
 * 算法参数:
 * <ul>
 *     <li>算法:AES/GCM/NoPadding</li>
 *     <li>密钥:32 字节(256 位),由 {@link CryptoProperties#getSecretKey()} 提供</li>
 *     <li>IV(初始化向量):12 字节,每次加密随机生成(符合 NIST SP 800-38D 推荐)</li>
 *     <li>认证标签(GCM Tag):16 字节(128 位),提供完整性认证</li>
 * </ul>
 * <p>
 * 密文格式:{@code base64(IV(12B) || ciphertext || tag(16B))},解密时按相同顺序拆分。
 * <p>
 * GCM 模式相比 CBC 的优势:
 * <ol>
 *     <li>同时提供机密性与完整性认证,防止密文被篡改(如位翻转攻击)</li>
 *     <li>无需填充,无 Padding Oracle 风险</li>
 *     <li>支持并行计算,性能更优</li>
 * </ol>
 */
@Slf4j
@Component
public class AesGcmEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    /** GCM 推荐 IV 长度:12 字节(96 位) */
    private static final int IV_LENGTH = 12;
    /** GCM 认证标签长度:128 位(16 字节) */
    private static final int TAG_LENGTH_BITS = 128;

    private final CryptoProperties cryptoProperties;
    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public AesGcmEncryptor(CryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
        // 启动时密钥已通过 CryptoProperties.validate() 校验长度为 32 字节,
        // 此处直接构造 SecretKeySpec;若 enabled=false 则密钥为空,所有加解密方法会直接透传
        if (cryptoProperties.isEnabled() && cryptoProperties.getSecretKey() != null) {
            byte[] keyBytes = Base64.getDecoder().decode(cryptoProperties.getSecretKey());
            this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        } else {
            this.keySpec = null;
        }
    }

    /**
     * 加密明文字符串。
     * <p>
     * 若加密功能被禁用(enabled=false),直接返回原明文(用于数据迁移/调试场景)。
     *
     * @param plaintext 明文
     * @return base64(IV + ciphertext + tag);输入 null 返回 null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (!cryptoProperties.isEnabled() || keySpec == null) {
            // 加密禁用:透传明文(仅用于数据迁移/调试)
            return plaintext;
        }
        try {
            // 每次加密生成随机 IV,防止相同明文产生相同密文
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + ciphertext(含 tag)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("AES-GCM 加密失败", e);
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /**
     * 解密密文字符串。
     * <p>
     * 若加密功能被禁用(enabled=false),直接返回原值(用于数据迁移/调试场景)。
     *
     * @param ciphertext base64(IV + ciphertext + tag)
     * @return 明文;输入 null 返回 null
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (!cryptoProperties.isEnabled() || keySpec == null) {
            return ciphertext;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded.length <= IV_LENGTH) {
                // 长度不足,可能是未加密的旧数据,直接返回原值
                // (数据迁移场景:旧数据为明文,需由 DataMigrationService 处理)
                return ciphertext;
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // base64 解码失败:旧数据可能是明文,直接返回
            log.debug("解密失败,可能是未加密的旧数据,直接返回原值: {}", e.getMessage());
            return ciphertext;
        } catch (Exception e) {
            log.error("AES-GCM 解密失败", e);
            // 解密失败不抛异常,返回原值,避免单条坏数据导致整页查询失败
            // (生产环境应由数据迁移工具预先清洗)
            return ciphertext;
        }
    }

    /**
     * 判断字符串是否为加密格式(base64 解码后长度大于 IV_LENGTH)。
     * 用于数据迁移工具区分明文与密文。
     *
     * @param value 待检测字符串
     * @return true=疑似密文,false=疑似明文
     */
    public boolean looksEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
