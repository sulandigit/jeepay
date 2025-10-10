#!/bin/bash

# Jeepay MySQL 升级前环境检查脚本
# 用途：检查当前环境状态，确保升级条件满足
# 执行方式：bash pre-upgrade-check.sh

set -e

echo "======================================"
echo "Jeepay MySQL 升级前环境检查"
echo "======================================"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查结果统计
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# 检查函数
check_item() {
    local item_name=$1
    local command=$2
    local expected_result=$3
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    echo -n "检查 $item_name ... "
    
    if eval "$command"; then
        echo -e "${GREEN}✓ PASS${NC}"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        if [ -n "$expected_result" ]; then
            echo "   期望: $expected_result"
        fi
        return 1
    fi
}

# 信息收集函数
collect_info() {
    local item_name=$1
    local command=$2
    
    echo -e "${YELLOW}$item_name:${NC}"
    eval "$command" | sed 's/^/  /'
    echo ""
}

echo "================== 基础环境检查 =================="

# 检查 Docker 是否运行
check_item "Docker 服务状态" "docker info > /dev/null 2>&1"

# 检查 Docker Compose 是否可用
check_item "Docker Compose 可用性" "docker-compose --version > /dev/null 2>&1"

# 检查当前目录是否为 Jeepay 项目根目录
check_item "Jeepay 项目根目录" "[ -f 'docker-compose.yml' ] && [ -f 'pom.xml' ] && [ -d 'jeepay-payment' ]"

echo ""
echo "================== 容器状态检查 =================="

# 检查 MySQL 容器是否运行
if docker ps | grep -q "jeepay-mysql"; then
    check_item "MySQL 容器运行状态" "docker ps | grep -q 'jeepay-mysql'"
    
    # 收集 MySQL 版本信息
    collect_info "当前 MySQL 版本" "docker exec jeepay-mysql mysql --version 2>/dev/null || echo '无法获取版本信息'"
    
    # 检查 MySQL 连接性
    check_item "MySQL 连接测试" "docker exec jeepay-mysql mysql -u root -prootroot -e 'SELECT 1' > /dev/null 2>&1"
    
else
    echo -e "${YELLOW}MySQL 容器未运行，跳过相关检查${NC}"
fi

# 检查其他关键容器
check_item "Redis 容器运行状态" "docker ps | grep -q 'jeepay-redis'"
check_item "ActiveMQ 容器运行状态" "docker ps | grep -q 'jeepay-activemq'"

echo ""
echo "================== 应用服务检查 =================="

# 检查应用容器状态
APP_CONTAINERS=("jeepay-payment" "jeepay-manager" "jeepay-merchant")
for container in "${APP_CONTAINERS[@]}"; do
    if docker ps | grep -q "$container"; then
        check_item "$container 运行状态" "docker ps | grep -q '$container'"
    else
        echo -e "${YELLOW}$container 容器未运行${NC}"
    fi
done

echo ""
echo "================== 数据库状态检查 =================="

if docker ps | grep -q "jeepay-mysql"; then
    # 收集数据库大小信息
    collect_info "数据库大小" "docker exec jeepay-mysql mysql -u root -prootroot -e \"SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) AS 'DB Size in MB' FROM information_schema.tables WHERE table_schema='jeepaydb';\" 2>/dev/null || echo '无法获取数据库大小'"
    
    # 收集表数量信息
    collect_info "数据表数量" "docker exec jeepay-mysql mysql -u root -prootroot -e \"SELECT COUNT(*) AS 'Table Count' FROM information_schema.tables WHERE table_schema='jeepaydb';\" 2>/dev/null || echo '无法获取表数量'"
    
    # 检查关键表是否存在
    CRITICAL_TABLES=("t_pay_order" "t_mch_info" "t_pay_interface_define" "t_sys_user")
    for table in "${CRITICAL_TABLES[@]}"; do
        check_item "关键表 $table 存在性" "docker exec jeepay-mysql mysql -u root -prootroot -e \"USE jeepaydb; DESCRIBE $table;\" > /dev/null 2>&1"
    done
    
    # 收集当前连接数
    collect_info "当前数据库连接数" "docker exec jeepay-mysql mysql -u root -prootroot -e \"SHOW STATUS LIKE 'Threads_connected';\" 2>/dev/null || echo '无法获取连接数'"
    
else
    echo -e "${YELLOW}MySQL 容器未运行，跳过数据库检查${NC}"
fi

echo ""
echo "================== 配置文件检查 =================="

# 检查关键配置文件是否存在
CONFIG_FILES=(
    "docker-compose.yml"
    "pom.xml"
    "conf/devCommons/config/application.yml"
    "conf/manager/application.yml"
    "conf/merchant/application.yml"
    "conf/payment/application.yml"
    "docs/install/include/my.cnf"
)

for config_file in "${CONFIG_FILES[@]}"; do
    check_item "配置文件 $config_file" "[ -f '$config_file' ]"
done

echo ""
echo "================== 磁盘空间检查 =================="

# 检查磁盘空间
collect_info "当前磁盘使用情况" "df -h ."

# 检查 Docker 空间使用
collect_info "Docker 空间使用" "docker system df"

echo ""
echo "================== Maven 环境检查 =================="

# 检查 Maven 是否可用
if command -v mvn > /dev/null 2>&1; then
    check_item "Maven 可用性" "mvn --version > /dev/null 2>&1"
    collect_info "Maven 版本" "mvn --version | head -n 1"
else
    echo -e "${YELLOW}Maven 未安装或不在 PATH 中${NC}"
fi

echo ""
echo "================== 网络连接检查 =================="

# 检查网络连接
check_item "网络连通性 (ping mysql hub)" "ping -c 1 hub.docker.com > /dev/null 2>&1"

echo ""
echo "================== 检查结果汇总 =================="

echo "总检查项: $TOTAL_CHECKS"
echo -e "通过检查: ${GREEN}$PASSED_CHECKS${NC}"
echo -e "失败检查: ${RED}$FAILED_CHECKS${NC}"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "\n${GREEN}✓ 所有检查项通过，环境状态良好，可以开始升级！${NC}"
    exit 0
else
    echo -e "\n${RED}✗ 有 $FAILED_CHECKS 项检查失败，请先解决相关问题再开始升级。${NC}"
    echo ""
    echo -e "${YELLOW}建议处理步骤：${NC}"
    echo "1. 检查并启动所有必要的 Docker 容器"
    echo "2. 确保 MySQL 数据库连接正常"
    echo "3. 验证所有配置文件完整"
    echo "4. 确保有足够的磁盘空间"
    exit 1
fi