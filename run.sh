#!/usr/bin/env bash
# Minellk —— 一键编译并运行（需要 JDK 11 及以上）
# 注意：lib/javafx 内为 Windows x64 的 JavaFX 21.0.4，
# Linux/macOS 用户请从 https://gluonhq.com/products/openjfx/ 下载对应平台的 SDK 替换。
cd "$(dirname "$0")"

mkdir -p out
find src -name "*.java" > sources.txt

if ! javac -encoding UTF-8 --module-path lib/javafx --add-modules javafx.controls -d out @sources.txt; then
    echo "编译失败：请确认已安装 JDK 11 或更高版本。"
    rm -f sources.txt
    exit 1
fi
rm -f sources.txt

java --module-path lib/javafx --add-modules javafx.controls -cp out app.Main
