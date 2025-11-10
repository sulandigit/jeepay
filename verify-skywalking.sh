#!/bin/bash

################################################################################
# SkyWalking 集成验证脚本
# 用于快速检查 SkyWalking 是否正确集成到 Jeepay 项目中
################################################################################

echo "======================================================================"
echo "  Jeepay SkyWalking 集成验证脚本"
echo "======================================================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查计数
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 辅助函数
print_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED_CHECKS++))
    ((TOTAL_CHECKS++))
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED_CHECKS++))
    ((TOTAL_CHECKS++))
}

print_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

# 1. 检查配置文件是否存在
echo "1️⃣  检查配置文件..."
echo "-----------------------------------"

if [ -f "docker-compose.yml" ]; then
    if grep -q "skywalking-oap" docker-compose.yml; then
        print_pass "docker-compose.yml 包含 SkyWalking 配置"
    else
        print_fail "docker-compose.yml 缺少 SkyWalking 配置"
    fi
else
    print_fail "docker-compose.yml 文件不存在"
fi

if [ -d "docker/skywalking" ]; then
    print_pass "docker/skywalking 目录存在"
else
    print_fail "docker/skywalking 目录不存在"
fi

if [ -f "docker/skywalking/docker-compose-skywalking.yml" ]; then
    print_pass "独立部署配置文件存在"
else
    print_fail "独立部署配置文件不存在"
fi

echo ""

# 2. 检查 Dockerfile 是否已修改
echo "2️⃣  检查 Dockerfile 集成..."
echo "-----------------------------------"

for service in payment manager merchant; do
    dockerfile="jeepay-${service}/Dockerfile"
    if [ -f "$dockerfile" ]; then
        if grep -q "SKYWALKING_VERSION" "$dockerfile"; then
            print_pass "jeepay-${service} Dockerfile 已集成 Agent"
        else
            print_fail "jeepay-${service} Dockerfile 未集成 Agent"
        fi
    else
        print_fail "jeepay-${service}/Dockerfile 不存在"
    fi
done

echo ""

# 3. 检查 logback 配置
echo "3️⃣  检查日志 TraceId 配置..."
echo "-----------------------------------"

for service in payment manager merchant; do
    logback="jeepay-${service}/src/main/resources/logback-spring.xml"
    if [ -f "$logback" ]; then
        if grep -q "%X{tid}" "$logback"; then
            print_pass "jeepay-${service} logback 已配置 TraceId"
        else
            print_fail "jeepay-${service} logback 未配置 TraceId"
        fi
    else
        print_fail "jeepay-${service}/logback-spring.xml 不存在"
    fi
done

echo ""

# 4. 检查文档是否创建
echo "4️⃣  检查文档完整性..."
echo "-----------------------------------"

if [ -f "SKYWALKING.md" ]; then
    print_pass "项目集成说明文档已创建"
else
    print_fail "项目集成说明文档不存在"
fi

if [ -f "docs/SKYWALKING_DEPLOYMENT.md" ]; then
    print_pass "部署文档已创建"
else
    print_fail "部署文档不存在"
fi

if [ -f "docs/SKYWALKING_USAGE.md" ]; then
    print_pass "使用指南已创建"
else
    print_fail "使用指南不存在"
fi

echo ""

# 5. 检查 Docker 服务状态（如果正在运行）
echo "5️⃣  检查 Docker 服务状态..."
echo "-----------------------------------"

if command -v docker &> /dev/null; then
    if docker ps | grep -q "jeepay-skywalking-oap"; then
        print_pass "SkyWalking OAP 服务正在运行"
    else
        print_info "SkyWalking OAP 服务未运行（可能尚未启动）"
    fi
    
    if docker ps | grep -q "jeepay-skywalking-ui"; then
        print_pass "SkyWalking UI 服务正在运行"
    else
        print_info "SkyWalking UI 服务未运行（可能尚未启动）"
    fi
    
    for service in payment manager merchant; do
        if docker ps | grep -q "jeepay-${service}"; then
            # 检查环境变量
            if docker exec "jeepay-${service}" env 2>/dev/null | grep -q "SW_AGENT_NAME"; then
                print_pass "jeepay-${service} 已配置 SkyWalking 环境变量"
            else
                print_info "jeepay-${service} 环境变量未配置（可能使用旧镜像）"
            fi
        else
            print_info "jeepay-${service} 服务未运行"
        fi
    done
else
    print_info "Docker 未安装，跳过运行时检查"
fi

echo ""

# 6. 检查端口占用（如果服务在运行）
echo "6️⃣  检查端口可用性..."
echo "-----------------------------------"

check_port() {
    local port=$1
    local service=$2
    
    if command -v nc &> /dev/null; then
        if nc -z localhost $port 2>/dev/null; then
            print_pass "$service (端口 $port) 可访问"
        else
            print_info "$service (端口 $port) 未响应（可能尚未启动）"
        fi
    elif command -v telnet &> /dev/null; then
        timeout 1 telnet localhost $port &>/dev/null
        if [ $? -eq 0 ]; then
            print_pass "$service (端口 $port) 可访问"
        else
            print_info "$service (端口 $port) 未响应（可能尚未启动）"
        fi
    else
        print_info "无法检查端口（nc 或 telnet 未安装）"
        return
    fi
}

check_port 8080 "SkyWalking UI"
check_port 11800 "SkyWalking OAP gRPC"
check_port 12800 "SkyWalking OAP HTTP"

echo ""

# 7. 总结
echo "======================================================================"
echo "  验证结果汇总"
echo "======================================================================"
echo ""
echo "总检查项: $TOTAL_CHECKS"
echo -e "${GREEN}通过: $PASSED_CHECKS${NC}"
echo -e "${RED}失败: $FAILED_CHECKS${NC}"
echo ""

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有检查通过！SkyWalking 已成功集成到 Jeepay 项目。${NC}"
    echo ""
    echo "下一步操作:"
    echo "  1. 启动服务: docker-compose up -d"
    echo "  2. 访问 SkyWalking UI: http://localhost:8080"
    echo "  3. 查看文档: cat SKYWALKING.md"
    exit 0
else
    echo -e "${RED}✗ 发现 $FAILED_CHECKS 个问题，请检查失败项。${NC}"
    echo ""
    echo "建议:"
    echo "  - 检查文件是否正确创建"
    echo "  - 重新执行集成脚本"
    echo "  - 查看详细文档: docs/SKYWALKING_DEPLOYMENT.md"
    exit 1
fi
