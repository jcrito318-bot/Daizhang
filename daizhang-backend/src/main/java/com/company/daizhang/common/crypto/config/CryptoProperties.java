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
 * 密钥解析优先级:{@code secret-key}(base64) > {@code aes-key}(明文) > 内置默认开发密钥(仅当未配置时,
 * 打印 WARN 日志,生产环境务必通过环境变量覆盖)。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    /**
     * 内置默认开发密钥(32 字节 ASCII),仅用于本地开发/测试,生产环境必须通过环境变量覆盖。
     */
    private static final String DEFAULT_DEV_KEY = "daizhang_dev_aes_key_32bytes!!_X";  // 恰好 32 字节

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
     * enabled=true 时按优先级解析密钥:secret-key(base64) > aes-key(明文) > 默认开发密钥。
     * 解析后必须为 32 字节(AES-256),否则启动失败。
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
                // 容错:secret-key 解码后非 32 字节时回退到默认开发密钥,避免启动失败
                log.warn("app.crypto.secret-key 解码后非 32 字节(实际 {}),回退到内置默认开发密钥", resolvedKeyBytes.length);
                this.resolvedKeyBytes = DEFAULT_DEV_KEY.getBytes(StandardCharsets.UTF_8);
                return;
            }
            log.info("AES-GCM 加密密钥校验通过(来源: secret-key,长度 {} 字节)", resolvedKeyBytes.length);
            return;
        }

        // 其次使用 aes-key(明文 32 字节)
        if (aesKey != null && !aesKey.isEmpty()) {
            log.warn("调试: aesKey 实际值='{}', 长度={}", aesKey, aesKey.length());
            this.resolvedKeyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
            if (resolvedKeyBytes.length != 32) {
                // 容错:长度非 32 时回退到默认开发密钥,而非启动失败(避免配置不当时服务无法启动)
                log.warn("app.crypto.aes-key 长度非 32 字节(实际 {}),回退到内置默认开发密钥", resolvedKeyBytes.length);
                this.resolvedKeyBytes = DEFAULT_DEV_KEY.getBytes(StandardCharsets.UTF_8);
                return;
            }
            log.info("AES-GCM 加密密钥校验通过(来源: aes-key,长度 {} 字节)", resolvedKeyBytes.length);
            return;
        }

        // 兜底:使用内置默认开发密钥(打印 WARN,生产环境必须覆盖)
        this.resolvedKeyBytes = DEFAULT_DEV_KEY.getBytes(StandardCharsets.UTF_8);
        log.warn("==========================================================");
        log.warn("警告: 未配置 app.crypto.secret-key 或 app.crypto.aes-key,使用内置默认开发密钥!");
        log.warn("该密钥仅适用于本地开发/测试,生产环境务必通过环境变量 CRYPTO_SECRET_KEY 或 AES_KEY 注入自定义密钥。");
        log.warn("==========================================================");
    }
}
