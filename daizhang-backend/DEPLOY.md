# 代账系统后端 - 部署文档

## 1. 系统要求

### 1.1 基础环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 必须，Spring Boot 3.2 要求 |
| Maven | 3.8+ | 构建时需要 |
| MySQL | 8.0+ | 必须 |
| Redis | 6.0+ | 必须，用于缓存和会话管理 |

### 1.2 推荐配置

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境 | 2核 | 4GB | 20GB |
| 测试环境 | 4核 | 8GB | 50GB |
| 生产环境 | 8核+ | 16GB+ | 100GB+ |

### 1.3 操作系统

- Linux (CentOS 7+, Ubuntu 18.04+, Debian 10+)
- macOS 11+
- Windows Server 2019+

---

## 2. 安装依赖

### 2.1 安装 JDK 17

**Linux (CentOS/RHEL):**
```bash
# 使用 Adoptium (Eclipse Temurin)
sudo yum install -y wget
wget https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz
sudo mkdir -p /usr/local/java
sudo tar -xzf OpenJDK17U-jdk_x64_linux_hotspot_17.0.9_9.tar.gz -C /usr/local/java/

# 配置环境变量
cat >> /etc/profile.d/java.sh << 'EOF'
export JAVA_HOME=/usr/local/java/jdk-17.0.9+9
export PATH=$JAVA_HOME/bin:$PATH
EOF
source /etc/profile.d/java.sh

# 验证
java -version
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
```

**macOS:**
```bash
brew install openjdk@17
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
java -version
```

### 2.2 安装 MySQL 8.0

**Linux (CentOS/RHEL):**
```bash
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
sudo mysql_secure_installation
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
sudo mysql_secure_installation
```

### 2.3 安装 Redis

**Linux (CentOS/RHEL):**
```bash
sudo yum install -y epel-release
sudo yum install -y redis
sudo systemctl start redis
sudo systemctl enable redis
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install -y redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

**macOS:**
```bash
brew install redis
brew services start redis
```

---

## 3. 数据库初始化

### 3.1 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE daizhang DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 创建应用用户（生产环境建议单独创建用户）
CREATE USER 'daizhang'@'%' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON daizhang.* TO 'daizhang'@'%';
FLUSH PRIVILEGES;

# 退出
exit;
```

### 3.2 执行初始化脚本

```bash
# 执行数据库初始化脚本
mysql -u daizhang -p daizhang < src/main/resources/db/init.sql
```

初始化脚本将创建以下核心表：

| 模块 | 表名 | 说明 |
|------|------|------|
| 系统管理 | sys_user | 用户表 |
| 系统管理 | sys_role | 角色表 |
| 系统管理 | sys_menu | 菜单表 |
| 系统管理 | sys_user_role | 用户角色关联表 |
| 系统管理 | sys_role_menu | 角色菜单关联表 |
| 系统管理 | sys_operation_log | 操作日志表 |
| 账套管理 | account_set | 账套表 |
| 账套管理 | account_period | 会计期间表 |
| 账套管理 | account_balance | 科目余额表 |
| 科目管理 | subject | 会计科目表 |
| 凭证管理 | voucher | 凭证主表 |
| 凭证管理 | voucher_detail | 凭证明细表 |
| 凭证管理 | voucher_word | 凭证字表 |
| 账簿管理 | (通过Service层动态生成) | 总账/明细账/日记账 |
| 客户管理 | customer | 客户表 |
| 客户管理 | service_contract | 服务合同表 |
| 客户管理 | payment_record | 收款记录表 |
| 固定资产 | fixed_asset | 固定资产表 |
| 固定资产 | asset_category | 资产类别表 |
| 固定资产 | depreciation_record | 折旧记录表 |
| 薪资管理 | employee | 员工表 |
| 薪资管理 | salary_item | 薪资项目表 |
| 薪资管理 | salary_sheet | 工资表 |
| 税务管理 | tax_calculation | 税费计算表 |
| 税务管理 | tax_declaration | 纳税申报记录表 |
| 银行对账 | bank_transaction | 银行流水表 |
| 银行对账 | bank_reconciliation | 银行对账单表 |
| 档案管理 | document | 档案表 |

---

## 4. 配置文件说明

### 4.1 主配置文件

配置文件路径：`src/main/resources/application.yml`

#### 服务器配置
```yaml
server:
  port: 8080                    # 服务端口
  servlet:
    context-path: /api          # 上下文路径
```

#### 数据源配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/daizhang    # 数据库地址
    username: root                                # 数据库用户名
    password: root                                # 数据库密码
    druid:
      initial-size: 5          # 初始连接数
      min-idle: 5              # 最小空闲连接
      max-active: 20           # 最大活跃连接
      max-wait: 60000          # 获取连接最大等待时间(ms)
```

#### Redis配置
```yaml
spring:
  data:
    redis:
      host: localhost          # Redis地址
      port: 6379               # Redis端口
      password:                # Redis密码（无密码留空）
      database: 0              # Redis数据库编号
```

#### JWT配置
```yaml
jwt:
  secret: your-secret-key      # JWT密钥（生产环境务必修改）
  expiration: 7200000          # Token有效期：2小时(ms)
  refresh-expiration: 604800000 # 刷新Token有效期：7天(ms)
```

### 4.2 多环境配置

支持通过 `spring.profiles.active` 切换环境：

```bash
# 开发环境
java -jar daizhang-backend-1.0.0.jar --spring.profiles.active=dev

# 测试环境
java -jar daizhang-backend-1.0.0.jar --spring.profiles.active=test

# 生产环境
java -jar daizhang-backend-1.0.0.jar --spring.profiles.active=prod
```

可创建对应的配置文件：
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

### 4.3 环境变量覆盖

启动脚本支持通过环境变量覆盖配置：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 激活的配置文件 | default |
| `SERVER_PORT` | 服务端口 | 8080 |
| `DB_HOST` | 数据库主机 | localhost |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | root |
| `REDIS_HOST` | Redis主机 | localhost |
| `REDIS_PORT` | Redis端口 | 6379 |
| `REDIS_PASSWORD` | Redis密码 | (空) |

---

## 5. 构建与部署

### 5.1 构建项目

```bash
cd daizhang-backend

# 编译打包（跳过测试）
mvn clean package -DskipTests

# 如有settings.xml（私有仓库）
mvn clean package -DskipTests -s settings.xml

# 构建产物位于
# target/daizhang-backend-1.0.0.jar
```

### 5.2 Linux/Mac 部署

#### 方式一：使用启动脚本（推荐）

```bash
# 赋予执行权限
chmod +x start.sh stop.sh

# 启动应用
./start.sh

# 查看状态
./start.sh status

# 查看日志
tail -f logs/daizhang-backend.log

# 停止应用
./stop.sh

# 重启应用
./start.sh restart
```

#### 方式二：前台运行（调试用）

```bash
./start.sh run
```

#### 方式三：直接运行

```bash
java -jar target/daizhang-backend-1.0.0.jar
```

### 5.3 Windows 部署

```cmd
:: 启动应用
start.bat

:: 停止应用（通过任务管理器或以下命令）
for /f "tokens=2" %p in ('tasklist /fi "imagename eq java.exe" /fo list ^| findstr "PID:"') do taskkill /PID %p /F
```

### 5.4 Systemd 服务部署（生产推荐）

创建服务文件 `/etc/systemd/system/daizhang.service`：

```ini
[Unit]
Description=代账系统后端服务
After=network.target mysql.service redis.service

[Service]
Type=simple
User=daizhang
Group=daizhang
WorkingDirectory=/opt/daizhang-backend
ExecStart=/usr/bin/java -server -Xms512m -Xmx1024m -jar /opt/daizhang-backend/daizhang-backend-1.0.0.jar --spring.profiles.active=prod
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/daizhang-backend/logs/daizhang-backend.log
StandardError=append:/opt/daizhang-backend/logs/daizhang-backend.log

[Install]
WantedBy=multi-user.target
```

```bash
# 部署JAR文件
sudo mkdir -p /opt/daizhang-backend
sudo cp target/daizhang-backend-1.0.0.jar /opt/daizhang-backend/
sudo cp -r logs /opt/daizhang-backend/

# 创建运行用户
sudo useradd -r -s /sbin/nologin daizhang
sudo chown -R daizhang:daizhang /opt/daizhang-backend

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable daizhang
sudo systemctl start daizhang
sudo systemctl status daizhang

# 查看日志
sudo journalctl -u daizhang -f
```

### 5.5 Docker 部署（可选）

创建 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/daizhang-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-server", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

```bash
# 构建镜像
docker build -t daizhang-backend:1.0.0 .

# 运行容器
docker run -d \
  --name daizhang-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=host.docker.internal \
  -e DB_USERNAME=daizhang \
  -e DB_PASSWORD=your_password \
  -e REDIS_HOST=host.docker.internal \
  daizhang-backend:1.0.0
```

---

## 6. 验证部署

### 6.1 健康检查

```bash
# 检查服务是否启动
curl -s http://localhost:8080/api/

# 预期返回（未认证时返回401是正常的）
# HTTP 401 或 200
```

### 6.2 访问API文档

浏览器打开：http://localhost:8080/api/doc.html

Knife4j 接口文档页面将展示所有可用API。

### 6.3 访问Druid监控

浏览器打开：http://localhost:8080/api/druid/

- 用户名：admin
- 密码：admin

> **注意**：生产环境请务必修改Druid监控的默认用户名和密码。

---

## 7. 常见问题排查

### 7.1 启动失败

**问题：端口被占用**
```
Error: Port 8080 is already in use
```
解决：
```bash
# 查找占用端口的进程
lsof -i :8080
# 或
ss -tlnp | grep 8080

# 停止占用端口的进程，或修改配置使用其他端口
./start.sh  # 启动前会自动检测端口冲突
```

**问题：Java版本不兼容**
```
Unsupported class file major version 61
```
解决：确认使用JDK 17+，`java -version` 检查版本。

**问题：数据库连接失败**
```
Communications link failure / Access denied for user
```
排查步骤：
1. 确认MySQL服务已启动：`systemctl status mysqld`
2. 确认数据库已创建：`mysql -u root -p -e "SHOW DATABASES;"`
3. 确认用户名密码正确
4. 确认MySQL允许远程连接（如非localhost）

**问题：Redis连接失败**
```
Unable to connect to Redis
```
排查步骤：
1. 确认Redis服务已启动：`systemctl status redis` 或 `redis-cli ping`
2. 确认Redis地址和端口正确
3. 如设置了密码，确认配置文件中密码正确

### 7.2 运行异常

**问题：内存溢出 (OOM)**
```
java.lang.OutOfMemoryError: Java heap space
```
解决：增大JVM堆内存
```bash
# 修改start.sh中的JAVA_OPTS
JAVA_OPTS="${JAVA_OPTS} -Xms1024m -Xmx2048m"
```

**问题：数据库连接池耗尽**
```
GetConnectionTimeoutException
```
排查：
1. 检查是否存在慢SQL：查看Druid监控页面
2. 增大连接池大小：修改 `max-active` 参数
3. 检查是否存在连接泄漏

**问题：JWT Token过期**
```
Token expired / 401 Unauthorized
```
解决：
1. 前端实现Token刷新机制
2. 调整Token有效期（`jwt.expiration`）

### 7.3 日志查看

```bash
# 实时查看日志
tail -f logs/daizhang-backend.log

# 查看错误日志
grep "ERROR" logs/daizhang-backend.log | tail -50

# 查看GC日志
tail -f logs/gc.log

# 搜索特定关键字
grep "Exception" logs/daizhang-backend.log
```

### 7.4 性能调优建议

1. **JVM调优**：根据服务器内存调整 `-Xms` 和 `-Xmx`，建议设为相同值避免堆震荡
2. **数据库调优**：
   - 合理设置连接池大小（`max-active`）
   - 开启慢SQL监控（`druid.stat.slowSqlMillis`）
3. **Redis调优**：合理设置连接池参数，考虑使用Redis集群
4. **日志级别**：生产环境将日志级别调整为 `info` 或 `warn`

---

## 8. 安全建议

1. **修改默认密码**：部署后立即修改数据库用户密码、Druid监控密码
2. **修改JWT密钥**：`jwt.secret` 必须使用强随机字符串
3. **关闭Druid监控**：生产环境建议关闭或限制访问IP
4. **启用HTTPS**：通过Nginx反向代理配置SSL证书
5. **防火墙配置**：仅开放必要端口（8080或443）

---

## 9. 目录结构

```
daizhang-backend/
├── src/
│   └── main/
│       ├── java/               # Java源代码
│       └── resources/
│           ├── application.yml # 主配置文件
│           └── db/
│               └── init.sql    # 数据库初始化脚本
├── target/
│   └── daizhang-backend-1.0.0.jar  # 构建产物
├── logs/                       # 日志目录（运行时自动创建）
│   ├── daizhang-backend.log    # 应用日志
│   ├── gc.log                  # GC日志
│   └── heapdump.hprof          # 堆转储（OOM时生成）
├── start.sh                    # Linux/Mac启动脚本
├── start.bat                   # Windows启动脚本
├── stop.sh                     # 停止脚本
├── pom.xml                     # Maven配置
└── settings.xml                # Maven仓库配置
```
