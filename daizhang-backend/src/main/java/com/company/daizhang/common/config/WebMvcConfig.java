package com.company.daizhang.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 允许的跨域来源,逗号分隔。
     * 生产环境应通过环境变量 CORS_ALLOWED_ORIGINS 显式指定受信前端域名。
     * 默认仅放开本地开发常用端口,避免"任意源+携带凭证"的CSRF/凭证泄露风险。
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 原 allowedOriginPatterns("*") + allowCredentials(true) 等同于完全放开 CORS,
        // 任意站点都能携带cookie/Authorization凭证跨域访问,存在CSRF与凭证泄露风险。
        // 改为白名单制,仅允许配置的受信源。
        String[] origins = allowedOrigins.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 原 allowedHeaders("*") 收敛为实际使用的请求头
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
