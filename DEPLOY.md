# 代账系统 Docker 部署指南

## 环境要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| Docker | 20.0+ | 容器运行时 |
| Docker Compose | 2.0+ | 多容器编排 (使用 `docker compose` 子命令) |

无需安装 Java、Node.js、MySQL 等依赖,全部打包在 Docker 镜像中。

## 一键启动

```bash
# 设置 JWT 密钥 (至少 32 字节的随机字符串)
export JWT_SECRET="your_32_byte_random_secret_here_1234567890"

# 构建并启动
docker compose up -d --build
```

或者单行命令:

```bash
JWT_SECRET=your_32_byte_random_secret docker compose up -d --build
```

> **安全提示**: 请将 `JWT_SECRET` 替换为你自己的随机密钥,不要使用示例值。
> 可以用 `openssl rand -base64 32` 生成一个安全的随机密钥。

## 访问地址

启动完成后 (约 30 秒),通过浏览器访问:

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost | Vue 3 SPA 界面 |
| 后端 API | http://localhost:8080/api/ | Spring Boot REST API |

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 系统管理员 |

> 首次登录后请立即修改默认密码。

## 数据持久化

后端使用 H2 嵌入式文件数据库,数据存储在 Docker volume `backend-data` 中:

- 数据库文件: `/data/daizhang.mv.db`
- 上传文件: `/data/uploads/`
- 应用日志: `/data/logs/`

即使容器删除重建,数据不会丢失 (volume 独立于容器生命周期)。

### 查看数据卷

```bash
docker volume inspect daizhang_backend-data
```

### 备份数据

```bash
# 备份数据卷到当前目录
docker run --rm -v daizhang_backend-data:/data -v $(pwd):/backup alpine \
  tar czf /backup/daizhang-backup-$(date +%Y%m%d).tar.gz -C /data .

# 恢复数据卷
docker run --rm -v daizhang_backend-data:/data -v $(pwd):/backup alpine \
  tar xzf /backup/daizhang-backup-YYYYMMDD.tar.gz -C /data
```

## 升级

```bash
# 拉取最新代码后,重新构建镜像并重启
docker compose build
docker compose up -d
```

Flyway 会自动检测并执行新增的数据库迁移文件 (V8, V9, ...),无需手动操作。

## 常用运维命令

```bash
# 查看容器状态
docker compose ps

# 查看后端日志 (实时)
docker compose logs -f backend

# 查看前端日志
docker compose logs -f frontend

# 进入后端容器
docker compose exec backend sh

# 停止所有服务
docker compose down

# 停止并删除数据卷 (危险!会清除所有数据)
docker compose down -v
```

## 环境变量

可通过环境变量或 `.env` 文件配置:

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_SECRET` | (无,必填) | JWT 签名密钥,至少 32 字节 |
| `COOKIE_SECURE` | `false` | Cookie Secure 标志,HTTPS 环境设为 `true` |
| `TOKEN_BLACKLIST_TYPE` | `memory` | Token 黑名单实现:`memory` 或 `redis` |

### 使用 .env 文件

在 `/workspace` 目录下创建 `.env` 文件:

```env
JWT_SECRET=your_32_byte_random_secret_here
COOKIE_SECURE=false
```

然后直接 `docker compose up -d` 即可,无需在命令行传递环境变量。

## 架构说明

```
浏览器 ──http──> [nginx:80] ──/api/──> [Spring Boot:8080]
                   │                      │
                   │ (静态文件)           │ (H2 文件数据库)
                   v                      v
              /usr/share/nginx/html   /data/daizhang.mv.db
                                         (docker volume)
```

- **前端容器**: Nginx 托管 Vue 3 构建产物,同时反向代理 `/api/` 请求到后端
- **后端容器**: Spring Boot + H2 嵌入式数据库,Flyway 自动管理数据库迁移
- **数据卷**: `backend-data` 持久化数据库文件、上传文件和日志

## 本地开发

Docker 部署不影响本地开发流程,原有脚本仍然可用:

- 后端: `cd daizhang-backend && mvn clean package -DskipTests && ./start.sh`
- 前端: `cd daizhang-frontend && npm install && npm run dev`

## 故障排查

### 后端启动失败: JWT_SECRET 未设置

```
ERROR: JWT 密钥未配置
```

**解决**: 设置 `JWT_SECRET` 环境变量后重启:
```bash
export JWT_SECRET="your_32_byte_random_secret"
docker compose up -d
```

### Flyway 迁移失败

查看后端日志中的 Flyway 错误信息:
```bash
docker compose logs backend | grep -i flyway
```

如果是已有数据库的迁移冲突,可以:
1. 备份数据卷
2. 删除数据卷重新初始化: `docker compose down -v && docker compose up -d --build`

### 端口被占用

修改 `docker-compose.yml` 中的端口映射,例如将 `80:80` 改为 `8081:80`。
