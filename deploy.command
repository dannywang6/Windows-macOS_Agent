#!/bin/bash
# StudyAgent 发布/部署脚本（mac）
# 把项目打包好的 target/study-agent.jar 复制到运行目录 ~/study-agent/
# 用法: 先 mvn package 打好 jar，再双击本脚本。

set -e

# 本项目根目录（脚本所在处）
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_SRC="$PROJECT_DIR/target/study-agent.jar"
RUN_DIR="$HOME/study-agent"

if [ ! -f "$JAR_SRC" ]; then
    echo "错误: 找不到 $JAR_SRC"
    echo "请先在项目里执行打包: mvn -q clean package"
    read -r _
    exit 1
fi

mkdir -p "$RUN_DIR"
cp "$JAR_SRC" "$RUN_DIR/study-agent.jar"

echo "发布完成 ✅"
echo "已复制: $JAR_SRC"
echo "  -> $RUN_DIR/study-agent.jar"
echo ""
echo "接下来:"
echo "  1. 运行  → 双击 $RUN_DIR/start.command"
echo "  2. 自启  → 双击 $RUN_DIR/enable-autostart.command"
read -r _
