package com.company.daizhang.common.crypto.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密配置属性 (P4.1)
 * <p>
 * 通过 {@code app.crypto.*} 注入:
 * <ul>
 *     <li>{@code enabled}:是否启用敏感字段加密,默认 true。关闭后 TypeHandler 透传明文,仅用于数据迁移/调试。</li>
 *     <li>{@code aes-key}:AES-GCM 密钥(明文字符串,必须 32 字节/256 位)。便于开发环境配置。</li>
 *     <li>{@code secret-key}:AES-GCM 密钥(base64 编码,解码后必须 32 字节)。生产环境推荐,优先级高于 aes-key。</li>
 *     <li>{@code iv-length}:GCM IV 长度(字节),推荐 12,默认 12。</li>
 *     <li>{@code tag-length}:GCM 认证标签长度(位),推荐 128,默认 128。</li>
 * </ul>
 * <p>
 * 密钥解析优先级:{@code secret-key}(base64) > {@code aes-key}(明文)。
 * 未配置任何密钥或密钥长度非法时,启动直接失败(fail-fast),不回退到默认密钥。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    /**
     * 是否启用加密。默认 true,数据迁移/调试时可显式关闭。
     */
    private boolean enabled = true;

    /**
     * AES-GCM 密钥(明文字符串,必须 32 字节)。开发环境友好,优先级低于 secret-key。
     */
    private String aesKey;

    /**
     * AES-GCM 密钥(base64 编码,解码后必须 32 字节)。生产环境推荐,优先级高于 aes-key。
     */
    private String secretKey;

    /**
     * GCM IV 长度(字节),NIST SP 800-38D 推荐 12 字节,默认 12。
     */
    private int ivLength = 12;

    /**
     * GCM 认证标签长度(位),默认 128(16 字节)。
     */
    private int tagLength = 128;

    /**
     * 解析后的 32 字节 AES 密钥,供 {@link com.company.daizhang.common.crypto.util.AesGcmEncryptor} 使用。
     * enabled=false 时为 null。
     */
    private byte[] resolvedKeyBytes;

    /**
     * 启动时校验密钥配置并解析最终密钥。
     * <p>
     * enabled=true 时按优先级解析密钥:secret-key(base64) > aes-key(明文)。
     * 密钥缺失或长度非法时直接抛异常拒绝启动(fail-fast),不回退到默认密钥。
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            log.warn("==========================================================");
            log.warn("警告: app.crypto.enabled=false,敏感字段加密已禁用!");
            log.warn("敏感数据将以明文存储,仅适用于数据迁移/调试场景,生产环境必须启用。");
            log.warn("==========================================================");
            this.resolvedKeyBytes = null;
            return;
        }

        // 优先使用 secret-key(base64)
        if (secretKey != null && !secretKey.isEmpty()) {
            try {
                this.resolvedKeyBytes = Base64.getDecoder().decode(secretKey);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("app.crypto.secret-key 不是合法的 base64 字符串", e);
            }
            if (resolvedKeyBytes.length != 32) {
                // fail-fast:密钥长度非法直接拒绝启动,不回退到默认密钥
                throw new IllegalStateException(
                        "app.crypto.secret-key 解码后必须为 32 字节(AES-256),实际长度: " + resolvedKeyBytes.length);
            }
            log.info("AES-GCM 加密密钥校验通过(来源: secret-key,长度 {} 字节)", resolvedKeyBytes.length);
            return;
        }

        // 其次使用 aes-key(明文 32 字节)
        if (aesKey != null && !aesKey.isEmpty()) {
            this.resolvedKeyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            if (resolvedKeyBytes.length != 32) {
                // fail-fast:密钥长度非法直接拒绝启动
                throw new IllegalStateException(
                        "app.crypto.aes-key 必须为 32 字节(AES-256),实际长度: " + resolvedKeyBytes.length);
            }
            log.info("AES-GCM 加密密钥校验通过(来源: aes-key,长度 {} 字节)", resolvedKeyBytes.length);
            return;
        }

        // fail-fast:未配置任何密钥,拒绝启动
        throw new IllegalStateException(
                "app.crypto.secret-key 或 app.crypto.aes-key 未配置。" +
                "请通过环境变量 CRYPTO_SECRET_KEY(base64) 或 AES_KEY(明文) 注入 32 字节 AES-256 密钥。");
    }
}
