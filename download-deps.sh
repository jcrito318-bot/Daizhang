#!/bin/bash
# 批量下载Maven依赖
set -e

MVN_REPO="$HOME/.m2/repository"
BASE="https://repo.maven.apache.org/maven2"

dl() {
    local path="$1"
    local dir="$MVN_REPO/$(dirname "$path")"
    local target="$MVN_REPO/$path"
    
    if [ -f "$target" ] && [ "$(wc -c < "$target")" -gt 100 ]; then
        return 0
    fi
    
    mkdir -p "$dir"
    curl -sL --connect-timeout 10 --max-time 60 -o "$target" "$BASE/$path" || {
        echo "FAILED: $path"
        rm -f "$target"
        return 1
    }
}

echo "=== 下载Spring Boot核心依赖 ==="

# Spring Boot Dependencies POM
dl "org/springframework/boot/spring-boot-dependencies/3.2.1/spring-boot-dependencies-3.2.1.pom"

# Spring Boot starter POMs
dl "org/springframework/boot/spring-boot-starter/3.2.1/spring-boot-starter-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-web/3.2.1/spring-boot-starter-web-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-validation/3.2.1/spring-boot-starter-validation-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-security/3.2.1/spring-boot-starter-security-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-data-redis/3.2.1/spring-boot-starter-data-redis-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-aop/3.2.1/spring-boot-starter-aop-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-test/3.2.1/spring-boot-starter-test-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-tomcat/3.2.1/spring-boot-starter-tomcat-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-json/3.2.1/spring-boot-starter-json-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-logging/3.2.1/spring-boot-starter-logging-3.2.1.pom"
dl "org/springframework/boot/spring-boot-autoconfigure/3.2.1/spring-boot-autoconfigure-3.2.1.pom"
dl "org/springframework/boot/spring-boot/3.2.1/spring-boot-3.2.1.pom"
dl "org/springframework/boot/spring-boot-starter-jdbc/3.2.1/spring-boot-starter-jdbc-3.2.1.pom"

# Spring Framework
dl "org/springframework/spring-framework-bom/6.1.2/spring-framework-bom-6.1.2.pom"
dl "org/springframework/spring-web/6.1.2/spring-web-6.1.2.pom"
dl "org/springframework/spring-webmvc/6.1.2/spring-webmvc-6.1.2.pom"
dl "org/springframework/spring-context/6.1.2/spring-context-6.1.2.pom"
dl "org/springframework/spring-core/6.1.2/spring-core-6.1.2.pom"
dl "org/springframework/spring-beans/6.1.2/spring-beans-6.1.2.pom"
dl "org/springframework/spring-aop/6.1.2/spring-aop-6.1.2.pom"
dl "org/springframework/spring-expression/6.1.2/spring-expression-6.1.2.pom"
dl "org/springframework/spring-tx/6.1.2/spring-tx-6.1.2.pom"
dl "org/springframework/spring-jdbc/6.1.2/spring-jdbc-6.1.2.pom"
dl "org/springframework/spring-security-core/6.2.1/spring-security-core-6.2.1.pom"
dl "org/springframework/spring-security-web/6.2.1/spring-security-web-6.2.1.pom"
dl "org/springframework/spring-security-config/6.2.1/spring-security-config-6.2.1.pom"
dl "org/springframework/data/spring-data-redis/3.2.1/spring-data-redis-3.2.1.pom"
dl "org/springframework/data/spring-data-commons/3.2.1/spring-data-commons-3.2.1.pom"

# MyBatis Plus
dl "com/baomidou/mybatis-plus-spring-boot3-starter/3.5.5/mybatis-plus-spring-boot3-starter-3.5.5.pom"
dl "com/baomidou/mybatis-plus-spring-boot3-starter/3.5.5/mybatis-plus-spring-boot3-starter-3.5.5.jar"
dl "com/baomidou/mybatis-plus-core/3.5.5/mybatis-plus-core-3.5.5.pom"
dl "com/baomidou/mybatis-plus-core/3.5.5/mybatis-plus-core-3.5.5.jar"
dl "com/baomidou/mybatis-plus-extension/3.5.5/mybatis-plus-extension-3.5.5.pom"
dl "com/baomidou/mybatis-plus-extension/3.5.5/mybatis-plus-extension-3.5.5.jar"
dl "com/baomidou/mybatis-plus-annotation/3.5.5/mybatis-plus-annotation-3.5.5.pom"
dl "com/baomidou/mybatis-plus-annotation/3.5.5/mybatis-plus-annotation-3.5.5.jar"

# Druid
dl "com/alibaba/druid-spring-boot-3-starter/1.2.21/druid-spring-boot-3-starter-1.2.21.pom"
dl "com/alibaba/druid-spring-boot-3-starter/1.2.21/druid-spring-boot-3-starter-1.2.21.jar"
dl "com/alibaba/druid/1.2.21/druid-1.2.21.pom"
dl "com/alibaba/druid/1.2.21/druid-1.2.21.jar"

# Hutool
dl "cn/hutool/hutool-all/5.8.25/hutool-all-5.8.25.pom"
dl "cn/hutool/hutool-all/5.8.25/hutool-all-5.8.25.jar"

# Knife4j
dl "com/github/xiaoymin/knife4j-openapi3-jakarta-spring-boot-starter/4.3.0/knife4j-openapi3-jakarta-spring-boot-starter-4.3.0.pom"
dl "com/github/xiaoymin/knife4j-openapi3-jakarta-spring-boot-starter/4.3.0/knife4j-openapi3-jakarta-spring-boot-starter-4.3.0.jar"

# JWT
dl "io/jsonwebtoken/jjwt-api/0.12.3/jjwt-api-0.12.3.pom"
dl "io/jsonwebtoken/jjwt-api/0.12.3/jjwt-api-0.12.3.jar"
dl "io/jsonwebtoken/jjwt-impl/0.12.3/jjwt-impl-0.12.3.pom"
dl "io/jsonwebtoken/jjwt-impl/0.12.3/jjwt-impl-0.12.3.jar"
dl "io/jsonwebtoken/jjwt-jackson/0.12.3/jjwt-jackson-0.12.3.pom"
dl "io/jsonwebtoken/jjwt-jackson/0.12.3/jjwt-jackson-0.12.3.jar"

# MapStruct
dl "org/mapstruct/mapstruct/1.5.5.Final/mapstruct-1.5.5.Final.pom"
dl "org/mapstruct/mapstruct/1.5.5.Final/mapstruct-1.5.5.Final.jar"

# Lombok
dl "org/projectlombok/lombok/1.18.30/lombok-1.18.30.pom"
dl "org/projectlombok/lombok/1.18.30/lombok-1.18.30.jar"

echo "=== 下载完成 ==="
