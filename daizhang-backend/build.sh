#!/bin/bash

# 代账系统后端打包脚本
# 使用方法: ./build.sh

set -e

echo "开始打包代账系统后端..."

# 清理并打包
mvn clean package -DskipTests

echo "打包完成！"
echo "生成的JAR文件位置: target/daizhang-backend-1.0.0.jar"
echo "运行命令: java -jar target/daizhang-backend-1.0.0.jar"
