# ============================================
# 代账系统前端 Dockerfile (多阶段构建)
# ============================================

# ---- 构建阶段 ----
FROM node:20-alpine AS builder
WORKDIR /build

# 先复制 package.json + lockfile,利用 Docker 层缓存预装依赖
COPY package*.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ---- 运行阶段 (nginx 托管静态文件 + 代理后端) ----
FROM nginx:alpine

# 复制构建产物到 nginx 默认目录
COPY --from=builder /build/dist /usr/share/nginx/html

# 覆盖默认 nginx 配置 (SPA 路由回退 + API 代理)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
