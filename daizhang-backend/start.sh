#!/bin/bash

# ============================================
# 代账系统后端启动脚本 (Linux/Mac)
# ============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 应用配置
APP_NAME="daizhang-backend"
APP_VERSION="1.0.0"
APP_JAR="${APP_NAME}-${APP_VERSION}.jar"
APP_PORT=8080

# 目录配置
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="${BASE_DIR}/logs"
PID_FILE="${BASE_DIR}/${APP_NAME}.pid"

# 日志文件
LOG_FILE="${LOG_DIR}/${APP_NAME}.log"
LOG_GC_FILE="${LOG_DIR}/gc.log"

# JVM参数
JAVA_OPTS="-server"
JAVA_OPTS="${JAVA_OPTS} -Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS} -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
JAVA_OPTS="${JAVA_OPTS} -XX:MaxGCPauseMillis=200"
JAVA_OPTS="${JAVA_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="${JAVA_OPTS} -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof"
JAVA_OPTS="${JAVA_OPTS} -Xlog:gc*:file=${LOG_GC_FILE}:time,tags:filecount=5,filesize=10m"

# Spring Boot配置（可通过环境变量覆盖）
SPRING_OPTS=""
SPRING_OPTS="${SPRING_OPTS} --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}"
SPRING_OPTS="${SPRING_OPTS} --server.port=${SERVER_PORT:-${APP_PORT}}"

# 数据存储目录（H2嵌入式数据库，本地文件存储，无需外部数据库服务）
SPRING_OPTS="${SPRING_OPTS} --spring.datasource.url=jdbc:h2:file:${DATA_DIR:-./data}/daizhang;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE"

# 创建数据目录
mkdir -p "${DATA_DIR:-./data}"

# ============================================
# 函数定义
# ============================================

log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        log_error "未找到Java运行环境，请先安装JDK 17+"
        log_error "下载地址: https://adoptium.net/"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{if($1==1) print $2; else print $1}')
    if [ "${JAVA_VERSION}" -lt 17 ] 2>/dev/null; then
        log_error "Java版本过低 (当前: ${JAVA_VERSION})，代账系统要求JDK 17+"
        exit 1
    fi

    log_info "Java环境检查通过: $(java -version 2>&1 | head -n 1)"
}

# 检查JAR文件
check_jar() {
    # 优先查找target目录下的JAR
    if [ -f "${BASE_DIR}/target/${APP_JAR}" ]; then
        JAR_PATH="${BASE_DIR}/target/${APP_JAR}"
    elif [ -f "${BASE_DIR}/${APP_JAR}" ]; then
        JAR_PATH="${BASE_DIR}/${APP_JAR}"
    else
        log_error "未找到JAR文件: ${APP_JAR}"
        log_error "请先执行构建: mvn clean package -DskipTests"
        exit 1
    fi
    log_info "找到JAR文件: ${JAR_PATH}"
}

# 检查端口是否被占用
check_port() {
    if command -v lsof &> /dev/null; then
        PID=$(lsof -t -i:${APP_PORT} 2>/dev/null)
        if [ -n "${PID}" ]; then
            log_warn "端口 ${APP_PORT} 已被进程 ${PID} 占用"
            log_warn "如需停止已有进程，请执行: ./stop.sh"
            exit 1
        fi
    elif command -v ss &> /dev/null; then
        PID=$(ss -tlnp | grep ":${APP_PORT} " | awk '{print $NF}' | grep -oP 'pid=\K\d+')
        if [ -n "${PID}" ]; then
            log_warn "端口 ${APP_PORT} 已被进程 ${PID} 占用"
            log_warn "如需停止已有进程，请执行: ./stop.sh"
            exit 1
        fi
    fi
}

# 检查是否已在运行
check_running() {
    if [ -f "${PID_FILE}" ]; then
        OLD_PID=$(cat "${PID_FILE}")
        if kill -0 "${OLD_PID}" 2>/dev/null; then
            log_warn "${APP_NAME} 已在运行中 (PID: ${OLD_PID})"
            log_warn "如需重启，请先执行: ./stop.sh"
            exit 1
        else
            log_warn "发现残留PID文件，但进程已不存在，清理中..."
            rm -f "${PID_FILE}"
        fi
    fi
}

# 启动应用
start() {
    log_info "=========================================="
    log_info "启动 ${APP_NAME} v${APP_VERSION}"
    log_info "=========================================="

    # 创建日志目录
    mkdir -p "${LOG_DIR}"

    check_java
    check_jar
    check_running
    check_port

    log_info "JVM参数: ${JAVA_OPTS}"
    log_info "Spring配置: ${SPRING_OPTS}"

    # 后台启动
    nohup java ${JAVA_OPTS} -jar "${JAR_PATH}" ${SPRING_OPTS} \
        >> "${LOG_FILE}" 2>&1 &

    APP_PID=$!
    echo ${APP_PID} > "${PID_FILE}"

    log_info "${APP_NAME} 已启动 (PID: ${APP_PID})"
    log_info "日志文件: ${LOG_FILE}"
    log_info "PID文件: ${PID_FILE}"

    # 等待启动完成
    log_info "等待应用启动..."
    WAIT_COUNT=0
    MAX_WAIT=60
    while [ ${WAIT_COUNT} -lt ${MAX_WAIT} ]; do
        if ! kill -0 ${APP_PID} 2>/dev/null; then
            log_error "应用启动失败，请检查日志: ${LOG_FILE}"
            rm -f "${PID_FILE}"
            exit 1
        fi
        if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${APP_PORT}/api/" 2>/dev/null | grep -q "200\|401\|403"; then
            log_info "${APP_NAME} 启动成功！"
            log_info "访问地址: http://localhost:${APP_PORT}/api/"
            log_info "API文档:  http://localhost:${APP_PORT}/api/doc.html"
            exit 0
        fi
        sleep 2
        WAIT_COUNT=$((WAIT_COUNT + 2))
        printf "."
    done

    echo ""
    log_warn "等待超时(${MAX_WAIT}秒)，应用可能仍在启动中"
    log_warn "请检查日志确认启动状态: tail -f ${LOG_FILE}"
}

# 前台运行（调试用）
run() {
    log_info "=========================================="
    log_info "前台运行 ${APP_NAME} v${APP_VERSION}"
    log_info "=========================================="

    check_java
    check_jar

    log_info "JVM参数: ${JAVA_OPTS}"
    log_info "按 Ctrl+C 停止应用"

    java ${JAVA_OPTS} -jar "${JAR_PATH}" ${SPRING_OPTS}
}

# ============================================
# 主入口
# ============================================

case "${1:-start}" in
    start)
        start
        ;;
    run)
        run
        ;;
    restart)
        log_info "重启应用..."
        if [ -f "${PID_FILE}" ]; then
            bash "${BASE_DIR}/stop.sh"
            sleep 3
        fi
        start
        ;;
    status)
        if [ -f "${PID_FILE}" ]; then
            PID=$(cat "${PID_FILE}")
            if kill -0 "${PID}" 2>/dev/null; then
                log_info "${APP_NAME} 正在运行 (PID: ${PID})"
            else
                log_warn "${APP_NAME} 未运行 (PID文件存在但进程不存在)"
            fi
        else
            log_info "${APP_NAME} 未运行"
        fi
        ;;
    *)
        echo "用法: $0 {start|stop|restart|run|status}"
        echo "  start   - 后台启动应用 (默认)"
        echo "  run     - 前台运行应用 (调试用)"
        echo "  restart - 重启应用"
        echo "  stop    - 停止应用"
        echo "  status  - 查看运行状态"
        exit 1
        ;;
esac
