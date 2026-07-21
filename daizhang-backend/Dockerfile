# ============================================
# 代账系统后端 Dockerfile (多阶段构建)
# ============================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制 pom.xml,利用 Docker 层缓存预下载依赖
COPY pom.xml .
COPY settings.xml .
RUN mvn -s settings.xml dependency:go-offline -B

# 复制源码并构建 (跳过测试)
COPY src ./src
RUN mvn -s settings.xml clean package -DskipTests -B

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 复制构建产物
COPY --from=builder /build/target/daizhang-backend-1.0.0.jar app.jar

# 创建数据目录 (H2 文件数据库 + 上传文件 + 日志)
RUN mkdir -p /data/uploads /data/logs

EXPOSE 8080

# JVM 参数 (可通过环境变量覆盖)
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# 应用环境变量 (与 docker-compose.yml / application.yml 配合)
# JWT_SECRET 必须设置,否则启动时 validateSecret 会拒绝启动
ENV JWT_SECRET=""
ENV COOKIE_SECURE=false
ENV TOKEN_BLACKLIST_TYPE=memory

# H2 数据库文件路径 (docker volume 挂载到 /data)
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/data/daizhang;MODE=MySQL;AUTO_SERVER=TRUE"
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
