package com.company.daizhang.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.glm")
public class AiConfig {

    /**
     * GLM API Key
     */
    private String apiKey;

    /**
     * GLM API Secret
     */
    private String apiSecret;

    /**
     * GLM API基础URL
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";

    /**
     * 票据识别模型
     */
    private String ocrModel = "glm-4v";

    /**
     * 智能记账模型
     */
    private String chatModel = "glm-4-flash";

    /**
     * 请求超时时间（秒）
     */
    private Integer timeout = 30;

    /**
     * 是否启用AI功能
     */
    private Boolean enabled = true;
}
