#!/bin/bash

# Jeepay JDK 17 Docker构建验证脚本

echo "======================================"
echo "Jeepay JDK 17 Docker构建验证脚本"
echo "======================================"
echo ""

# 检查Docker环境
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ Docker服务未启动，请启动Docker服务"
    exit 1
fi

echo "✅ Docker环境检查通过"
echo ""

# 进入项目目录
cd /data/workspace/jeepay || exit 1

echo "1. 构建基础JDK 17镜像..."

# 构建根目录的多模块镜像
echo "  构建 payment 模块镜像..."
docker build -t jeepay-payment:jdk17-test --build-arg PLATFORM=payment --build-arg PORT=9216 . 2>/dev/null
PAYMENT_BUILD=$?

echo "  构建 manager 模块镜像..."
docker build -t jeepay-manager:jdk17-test --build-arg PLATFORM=manager --build-arg PORT=9217 . 2>/dev/null
MANAGER_BUILD=$?

echo "  构建 merchant 模块镜像..."
docker build -t jeepay-merchant:jdk17-test --build-arg PLATFORM=merchant --build-arg PORT=9218 . 2>/dev/null
MERCHANT_BUILD=$?

echo ""

# 检查构建结果
echo "2. 构建结果检查..."

if [ $PAYMENT_BUILD -eq 0 ]; then
    echo "  ✅ Payment模块构建成功"
else
    echo "  ❌ Payment模块构建失败"
fi

if [ $MANAGER_BUILD -eq 0 ]; then
    echo "  ✅ Manager模块构建成功"
else
    echo "  ❌ Manager模块构建失败"
fi

if [ $MERCHANT_BUILD -eq 0 ]; then
    echo "  ✅ Merchant模块构建成功"
else
    echo "  ❌ Merchant模块构建失败"
fi

echo ""

# 统计构建结果
SUCCESS_COUNT=0
TOTAL_COUNT=3

[ $PAYMENT_BUILD -eq 0 ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
[ $MANAGER_BUILD -eq 0 ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
[ $MERCHANT_BUILD -eq 0 ] && SUCCESS_COUNT=$((SUCCESS_COUNT + 1))

echo "3. 镜像信息检查..."

if [ $SUCCESS_COUNT -gt 0 ]; then
    echo "  成功构建的镜像:"
    [ $PAYMENT_BUILD -eq 0 ] && docker images jeepay-payment:jdk17-test --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
    [ $MANAGER_BUILD -eq 0 ] && docker images jeepay-manager:jdk17-test --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
    [ $MERCHANT_BUILD -eq 0 ] && docker images jeepay-merchant:jdk17-test --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
    
    echo ""
    echo "  镜像JDK版本验证:"
    
    if [ $PAYMENT_BUILD -eq 0 ]; then
        echo "    Payment模块 JDK版本:"
        docker run --rm jeepay-payment:jdk17-test java -version
        echo ""
    fi
else
    echo "  ❌ 没有成功构建的镜像"
fi

echo ""

# 清理测试镜像
echo "4. 清理测试镜像..."
[ $PAYMENT_BUILD -eq 0 ] && docker rmi jeepay-payment:jdk17-test >/dev/null 2>&1
[ $MANAGER_BUILD -eq 0 ] && docker rmi jeepay-manager:jdk17-test >/dev/null 2>&1
[ $MERCHANT_BUILD -eq 0 ] && docker rmi jeepay-merchant:jdk17-test >/dev/null 2>&1
echo "  ✅ 测试镜像已清理"

echo ""
echo "======================================"
echo "Docker构建验证完成"
echo "======================================"
echo ""
echo "构建摘要:"
echo "- 总模块数: $TOTAL_COUNT"
echo "- 成功构建: $SUCCESS_COUNT"
echo "- 成功率: $(( SUCCESS_COUNT * 100 / TOTAL_COUNT ))%"
echo ""

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    echo "🎉 所有模块Docker镜像构建成功！"
    echo ""
    echo "下一步操作:"
    echo "1. 运行: docker-compose build"
    echo "2. 测试: docker-compose up -d"
    echo "3. 验证: curl http://localhost:9216/actuator/health"
else
    echo "⚠️  部分模块构建失败，请检查构建日志"
    echo ""
    echo "排查建议:"
    echo "1. 检查Dockerfile配置"
    echo "2. 检查Maven依赖"
    echo "3. 查看详细构建日志: docker build --no-cache ..."
fi