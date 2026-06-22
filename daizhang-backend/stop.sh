#!/bin/bash

# ============================================
# 代账系统后端停止脚本 (Linux/Mac)
# ============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 应用配置
APP_NAME="daizhang-backend"
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="${BASE_DIR}/${APP_NAME}.pid"

# 等待超时（秒）
SHUTDOWN_TIMEOUT=30

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

# 获取PID
get_pid() {
    if [ -f "${PID_FILE}" ]; then
        PID=$(cat "${PID_FILE}")
        if kill -0 "${PID}" 2>/dev/null; then
            echo "${PID}"
            return 0
        fi
    fi

    # PID文件不存在或进程已死，尝试通过进程名查找
    PID=$(pgrep -f "${APP_NAME}" 2>/dev/null | head -n 1)
    if [ -n "${PID}" ]; then
        echo "${PID}"
        return 0
    fi

    return 1
}

# 优雅停止
graceful_stop() {
    PID=$(get_pid)
    if [ -z "${PID}" ]; then
        log_warn "${APP_NAME} 未在运行"
        rm -f "${PID_FILE}"
        return 0
    fi

    log_info "正在停止 ${APP_NAME} (PID: ${PID})..."

    # 发送SIGTERM信号（优雅关闭）
    kill "${PID}" 2>/dev/null

    # 等待进程退出
    WAIT_COUNT=0
    while [ ${WAIT_COUNT} -lt ${SHUTDOWN_TIMEOUT} ]; do
        if ! kill -0 "${PID}" 2>/dev/null; then
            log_info "${APP_NAME} 已停止"
            rm -f "${PID_FILE}"
            return 0
        fi
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
        printf "."
    done

    echo ""
    log_warn "优雅停止超时(${SHUTDOWN_TIMEOUT}秒)，尝试强制停止..."
    return 1
}

# 强制停止
force_stop() {
    PID=$(get_pid)
    if [ -z "${PID}" ]; then
        log_info "${APP_NAME} 已停止"
        rm -f "${PID_FILE}"
        return 0
    fi

    log_warn "强制停止进程 (PID: ${PID})..."
    kill -9 "${PID}" 2>/dev/null
    sleep 2

    if kill -0 "${PID}" 2>/dev/null; then
        log_error "无法停止进程 (PID: ${PID})，请手动处理"
        return 1
    fi

    log_info "${APP_NAME} 已强制停止"
    rm -f "${PID_FILE}"
    return 0
}

# ============================================
# 主入口
# ============================================

case "${1:-stop}" in
    stop)
        graceful_stop
        if [ $? -ne 0 ]; then
            force_stop
        fi
        ;;
    force)
        force_stop
        ;;
    *)
        echo "用法: $0 {stop|force}"
        echo "  stop  - 优雅停止应用 (默认，等待${SHUTDOWN_TIMEOUT}秒)"
        echo "  force - 强制停止应用 (kill -9)"
        exit 1
        ;;
esac
