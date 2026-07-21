package com.company.daizhang.common.crypto.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 加密配置属性 (P4.1)
 * <p>
 * 通过 {@code app.crypto.*} 注入:
 * <ul>
 *     <li>{@code secret-key}:AES-GCM 密钥,32 字节(256 位)的 base64 编码字符串,
 *         必须通过环境变量 CRYPTO_SECRET_KEY 注入,缺省启动失败。</li>
 *     <li>{@code enabled}:是否启用敏感字段加密,默认 true。
 *         关闭后 TypeHandler 透传明文,仅用于数据迁移/调试场景。</li>
 * </ul>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    /**
     * AES-GCM 密钥(base64 编码,解码后必须为 32 字节)。
     * 缺省空字符串,启动校验失败。
     */
    private String secretKey;

    /**
     * 是否启用加密。默认 true,数据迁移/调试时可显式关闭。
     */
    private boolean enabled = true;

    /**
     * 启动时校验密钥配置。
     * <p>
     * enabled=true 时,secretKey 必须非空且 base64 解码后恰好 32 字节,否则启动失败。
     * 这是为了防止生产环境误用空密钥导致加密形同虚设(明文存储)。
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            log.warn("==========================================================");
            log.warn("警告: app.crypto.enabled=false,敏感字段加密已禁用!");
            log.warn("敏感数据将以明文存储,仅适用于数据迁移/调试场景,生产环境必须启用。");
            log.warn("==========================================================");
            return;
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException(
                    "app.crypto.secret-key 未配置,请通过环境变量 CRYPTO_SECRET_KEY 注入 32 字节 base64 编码的 AES-GCM 密钥");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.crypto.secret-key 不是合法的 base64 字符串", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.crypto.secret-key 解码后必须为 32 字节(AES-256),实际长度: " + keyBytes.length);
        }
        log.info("AES-GCM 加密密钥校验通过(长度 {} 字节)", keyBytes.length);
    }
}
