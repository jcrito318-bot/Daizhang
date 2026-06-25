@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

:: ============================================
:: 代账系统后端启动脚本 (Windows)
:: ============================================

:: 应用配置
set APP_NAME=daizhang-backend
set APP_VERSION=1.0.0
set APP_JAR=%APP_NAME%-%APP_VERSION%.jar
set APP_PORT=8080

:: 目录配置
set BASE_DIR=%~dp0
set LOG_DIR=%BASE_DIR%logs
set PID_FILE=%BASE_DIR%%APP_NAME%.pid

:: 日志文件
set LOG_FILE=%LOG_DIR%\%APP_NAME%.log
set LOG_GC_FILE=%LOG_DIR%\gc.log

:: JVM参数
set JAVA_OPTS=-server
set JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx1024m
set JAVA_OPTS=%JAVA_OPTS% -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseG1GC
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxGCPauseMillis=200
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=%LOG_DIR%\heapdump.hprof
set JAVA_OPTS=%JAVA_OPTS% -Xlog:gc*:file=%LOG_GC_FILE%:time,tags:filecount=5,filesize=10m

:: Spring Boot配置（可通过环境变量覆盖）
set SPRING_OPTS=
if "%SPRING_PROFILES_ACTIVE%"=="" (set SPRING_PROFILES_ACTIVE=default)
set SPRING_OPTS=%SPRING_OPTS% --spring.profiles.active=%SPRING_PROFILES_ACTIVE%
set SPRING_OPTS=%SPRING_OPTS% --server.port=%APP_PORT%

:: 数据存储目录（H2嵌入式数据库，本地文件存储，无需外部数据库服务）
if "%DATA_DIR%"=="" set DATA_DIR=.\data
set SPRING_OPTS=%SPRING_OPTS% --spring.datasource.url=jdbc:h2:file:%DATA_DIR%/daizhang;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE

:: 创建数据目录
if not exist "%DATA_DIR%" mkdir "%DATA_DIR%"

:: ============================================
:: 检查Java环境
:: ============================================
:check_java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到Java运行环境，请先安装JDK 17+
    echo [ERROR] 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_RAW=%%~v
    goto :version_found
)
:version_found

:: 提取主版本号
for /f "tokens=1 delims=." %%a in ("%JAVA_VERSION_RAW%") do set JAVA_MAJOR=%%a
if %JAVA_MAJOR%==1 (
    for /f "tokens=2 delims=." %%a in ("%JAVA_VERSION_RAW%") do set JAVA_VERSION=%%a
) else (
    set JAVA_VERSION=%JAVA_MAJOR%
)

if %JAVA_VERSION% LSS 17 (
    echo [ERROR] Java版本过低 (当前: %JAVA_VERSION%)，代账系统要求JDK 17+
    pause
    exit /b 1
)

echo [INFO] Java环境检查通过: java version "%JAVA_VERSION_RAW%"

:: ============================================
:: 检查JAR文件
:: ============================================
:check_jar
if exist "%BASE_DIR%target\%APP_JAR%" (
    set JAR_PATH=%BASE_DIR%target\%APP_JAR%
    goto :jar_found
)
if exist "%BASE_DIR%%APP_JAR%" (
    set JAR_PATH=%BASE_DIR%%APP_JAR%
    goto :jar_found
)

echo [ERROR] 未找到JAR文件: %APP_JAR%
echo [ERROR] 请先执行构建: mvn clean package -DskipTests
pause
exit /b 1

:jar_found
echo [INFO] 找到JAR文件: %JAR_PATH%

:: ============================================
:: 检查是否已在运行
:: ============================================
:check_running
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq %OLD_PID%" 2>nul | findstr /i "%OLD_PID%" >nul
    if not errorlevel 1 (
        echo [WARN] %APP_NAME% 已在运行中 (PID: %OLD_PID%)
        echo [WARN] 如需重启，请先执行: stop.bat
        pause
        exit /b 1
    ) else (
        echo [WARN] 发现残留PID文件，但进程已不存在，清理中...
        del /f "%PID_FILE%" >nul 2>&1
    )
)

:: ============================================
:: 创建日志目录并启动
:: ============================================
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [INFO] ==========================================
echo [INFO] 启动 %APP_NAME% v%APP_VERSION%
echo [INFO] ==========================================
echo [INFO] JVM参数: %JAVA_OPTS%
echo [INFO] Spring配置: %SPRING_OPTS%

:: 后台启动（使用start命令）
start /b java %JAVA_OPTS% -jar "%JAR_PATH%" %SPRING_OPTS% > "%LOG_FILE%" 2>&1

:: 获取Java进程PID（取最新的java进程）
timeout /t 2 /nobreak >nul
for /f "tokens=2" %%p in ('tasklist /fi "imagename eq java.exe" /fo list ^| findstr "PID:"') do (
    set APP_PID=%%p
)

if defined APP_PID (
    echo %APP_PID%> "%PID_FILE%"
    echo [INFO] %APP_NAME% 已启动 (PID: %APP_PID%)
    echo [INFO] 日志文件: %LOG_FILE%
    echo [INFO] PID文件: %PID_FILE%
    echo.
    echo [INFO] 访问地址: http://localhost:%APP_PORT%/api/
    echo [INFO] API文档:  http://localhost:%APP_PORT%/api/doc.html
    echo.
    echo [INFO] 查看日志: type %LOG_FILE%
) else (
    echo [ERROR] 启动失败，请检查日志: %LOG_FILE%
)

endlocal
pause
