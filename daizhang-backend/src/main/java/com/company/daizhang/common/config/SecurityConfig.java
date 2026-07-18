package com.company.daizhang.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Spring Security配置
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * CORS 允许来源,与 WebMvcConfig 读取同一属性 cors.allowed-origins。
     * 此处仅做启动期安全校验,实际跨域生效在 WebMvcConfig#addCorsMappings。
     * 默认仅放开本地开发常用端口;生产环境应通过环境变量 CORS_ALLOWED_ORIGINS 显式指定受信域名。
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @PostConstruct
    public void validateCorsConfig() {
        // 防止环境变量 CORS_ALLOWED_ORIGINS 误配(通配符*/空值):
        // 配合 allowCredentials(true),"*" 等同于完全放开跨域,任意站点可携带凭证访问,存在CSRF/凭证泄露风险。
        if (!StringUtils.hasText(allowedOrigins)) {
            log.warn("CORS配置 cors.allowed-origins 为空,跨域请求将被拒绝;请检查环境变量 CORS_ALLOWED_ORIGINS");
            return;
        }
        for (String origin : allowedOrigins.split(",")) {
            String o = origin.trim();
            if ("*".equals(o)) {
                log.warn("==========================================================");
                log.warn("警告: CORS配置包含通配符 *,配合 allowCredentials(true) 等同于完全放开跨域,");
                log.warn("存在CSRF与凭证泄露风险,生产环境必须限制为具体受信域名!");
                log.warn("==========================================================");
            } else if (!StringUtils.hasText(o)) {
                log.warn("CORS配置 cors.allowed-origins 存在空值(连续逗号或首尾逗号),请检查环境变量 CORS_ALLOWED_ORIGINS");
            }
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 仅允许同源iframe,防止点击劫持(原全局disable过宽)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**"
                        ).permitAll()
                        // B-021 修复:不再 permitAll Knife4j/Springdoc 文档端点。
                        // knife4j.enable=false 在 4.3.0 实测无法完全阻断 /doc.html 与 /v3/api-docs,
                        // 必须在 Security 层兜底。即使是开发环境,要求 token 访问也是可接受的
                        // (Knife4j UI 自带 Authorize 按钮可填入 JWT)。
                        // H2控制台/Druid监控台同样不放开:含敏感信息且可执行SQL。
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
