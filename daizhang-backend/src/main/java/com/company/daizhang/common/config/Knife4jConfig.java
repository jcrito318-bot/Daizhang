package com.company.daizhang.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Knife4j配置
 * <p>
 * B-021 修复:仅 dev/test profile 加载此配置类。
 * 生产环境默认不暴露 API 文档(/doc.html、/v3/api-docs),防止接口结构泄露。
 * 若生产需临时开启,请通过环境变量 KNIFE4J_ENABLED=true 显式启用,并配合 SPRING_PROFILES_ACTIVE=dev。
 */
@Profile({"dev", "test"})
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("代账系统API")
                        .version("1.0.0")
                        .description("代账系统接口文档")
                        .contact(new Contact()
                                .name("代账系统")
                                .email("admin@daizhang.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
