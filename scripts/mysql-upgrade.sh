#!/bin/bash

# Jeepay MySQL 8.0.35 升级执行脚本
# 用途：自动化执行 MySQL 数据库升级流程
# 执行方式：bash mysql-upgrade.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置参数
MYSQL_CONTAINER="jeepay-mysql"
MYSQL_ROOT_PASSWORD="rootroot"
DATABASE_NAME="jeepaydb"
TARGET_MYSQL_VERSION="8.0.35"
BACKUP_DIR=""
UPGRADE_LOG_FILE="mysql_upgrade_$(date +%Y%m%d_%H%M%S).log"

echo "======================================"
echo "Jeepay MySQL 8.0.35 升级执行脚本"
echo "======================================"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "日志文件: $UPGRADE_LOG_FILE"
echo ""

# 记录日志函数
log_message() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "$message" | tee -a "$UPGRADE_LOG_FILE"
}

# 错误处理函数
handle_error() {
    local error_msg="$1"
    log_message "错误: $error_msg"
    echo -e "${RED}升级失败: $error_msg${NC}"
    echo -e "${YELLOW}请检查日志文件: $UPGRADE_LOG_FILE${NC}"
    exit 1
}

# 检查函数
check_prerequisite() {
    local check_name=$1
    local check_command=$2
    
    log_message "检查 $check_name ..."
    
    if eval "$check_command" >> "$UPGRADE_LOG_FILE" 2>&1; then
        echo -e "${GREEN}✓ $check_name 检查通过${NC}"
        return 0
    else
        handle_error "$check_name 检查失败"
    fi
}

# 执行步骤函数
execute_step() {
    local step_name=$1
    local step_command=$2
    local skip_error=${3:-false}
    
    echo -e "${BLUE}正在执行: $step_name${NC}"
    log_message "开始执行: $step_name"
    
    if eval "$step_command" >> "$UPGRADE_LOG_FILE" 2>&1; then
        echo -e "${GREEN}✓ $step_name 完成${NC}"
        log_message "成功完成: $step_name"
        return 0
    else
        if [ "$skip_error" = "true" ]; then
            echo -e "${YELLOW}⚠ $step_name 执行有警告，但继续进行${NC}"
            log_message "警告: $step_name 执行有警告，但继续进行"
            return 0
        else
            handle_error "$step_name 执行失败"
        fi
    fi
}

# 用户确认函数
confirm_action() {
    local prompt=$1
    echo -e "${YELLOW}$prompt${NC}"
    echo -n "请输入 'yes' 确认继续: "
    read -r response
    if [ "$response" != "yes" ]; then
        log_message "用户取消操作"
        echo "操作已取消"
        exit 0
    fi
}

echo "================== 升级前检查 =================="

# 1. 基础环境检查
check_prerequisite "Docker 服务" "docker info > /dev/null 2>&1"
check_prerequisite "Docker Compose" "docker-compose --version > /dev/null 2>&1"
check_prerequisite "项目目录结构" "[ -f 'docker-compose.yml' ] && [ -f 'pom.xml' ]"

# 2. 容器状态检查
if ! docker ps | grep -q "$MYSQL_CONTAINER"; then
    handle_error "MySQL 容器 $MYSQL_CONTAINER 未运行"
fi

log_message "当前 MySQL 容器状态："
docker ps | grep "$MYSQL_CONTAINER" | tee -a "$UPGRADE_LOG_FILE"

# 3. 获取当前 MySQL 版本
CURRENT_VERSION=$(docker exec $MYSQL_CONTAINER mysql --version 2>/dev/null | grep -oP 'mysql\s+Ver\s+\K[0-9]+\.[0-9]+\.[0-9]+' || echo "unknown")
log_message "当前 MySQL 版本: $CURRENT_VERSION"

if [ "$CURRENT_VERSION" = "$TARGET_MYSQL_VERSION" ]; then
    echo -e "${GREEN}MySQL 已经是目标版本 $TARGET_MYSQL_VERSION，无需升级${NC}"
    exit 0
fi

echo ""
echo "================== 确认升级信息 =================="
echo "当前版本: $CURRENT_VERSION"
echo "目标版本: $TARGET_MYSQL_VERSION"
echo "数据库: $DATABASE_NAME"
echo "容器名: $MYSQL_CONTAINER"
echo ""

confirm_action "确认要开始 MySQL 升级吗？这个操作将会："
echo "1. 停止应用服务"
echo "2. 创建数据备份"
echo "3. 升级 MySQL 版本"
echo "4. 重新编译项目"
echo "5. 重启所有服务"

echo ""
echo "================== 执行数据备份 =================="

# 4. 执行数据备份
if [ -f "./scripts/backup-data.sh" ]; then
    execute_step "执行数据备份" "bash ./scripts/backup-data.sh"
    
    # 获取备份目录
    BACKUP_DIR=$(find ./backup -name "mysql_upgrade_*" -type d | sort | tail -n 1)
    if [ -n "$BACKUP_DIR" ]; then
        log_message "备份目录: $BACKUP_DIR"
        echo -e "${GREEN}数据备份完成，备份位置: $BACKUP_DIR${NC}"
    else
        handle_error "无法找到备份目录"
    fi
else
    handle_error "备份脚本不存在: ./scripts/backup-data.sh"
fi

echo ""
echo "================== 停止应用服务 =================="

# 5. 停止应用服务（保留 MySQL、Redis、ActiveMQ）
APP_CONTAINERS=("jeepay-ui-payment" "jeepay-ui-manager" "jeepay-ui-merchant" "jeepay-payment" "jeepay-manager" "jeepay-merchant")

for container in "${APP_CONTAINERS[@]}"; do
    if docker ps | grep -q "$container"; then
        execute_step "停止容器 $container" "docker stop $container" true
    else
        log_message "容器 $container 未运行，跳过"
    fi
done

echo ""
echo "================== 升级 MySQL =================="

# 6. 停止 MySQL 容器
execute_step "停止 MySQL 容器" "docker stop $MYSQL_CONTAINER"

# 7. 备份当前 MySQL 配置
execute_step "备份当前 my.cnf" "cp docs/install/include/my.cnf docs/install/include/my.cnf.backup" true

# 8. 更新优化的 MySQL 配置
if [ -f "docs/install/include/my.cnf.8035" ]; then
    execute_step "更新 MySQL 配置文件" "cp docs/install/include/my.cnf.8035 docs/install/include/my.cnf"
else
    log_message "警告: 未找到优化配置文件，使用原配置"
fi

# 9. 拉取新的 MySQL 镜像
execute_step "拉取 MySQL $TARGET_MYSQL_VERSION 镜像" "docker pull mysql:$TARGET_MYSQL_VERSION"

# 10. 启动新版本 MySQL
execute_step "启动新版本 MySQL 容器" "docker-compose up -d mysql"

# 11. 等待 MySQL 启动完成
echo -e "${BLUE}等待 MySQL 启动完成...${NC}"
sleep 10

# 12. 检查 MySQL 启动状态
RETRY_COUNT=0
MAX_RETRIES=12

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 1" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ MySQL 启动成功${NC}"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "等待 MySQL 启动... ($RETRY_COUNT/$MAX_RETRIES)"
        sleep 10
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    handle_error "MySQL 启动超时"
fi

# 13. 验证升级后的版本
UPGRADED_VERSION=$(docker exec $MYSQL_CONTAINER mysql --version 2>/dev/null | grep -oP 'mysql\s+Ver\s+\K[0-9]+\.[0-9]+\.[0-9]+' || echo "unknown")
log_message "升级后 MySQL 版本: $UPGRADED_VERSION"

if [ "$UPGRADED_VERSION" != "$TARGET_MYSQL_VERSION" ]; then
    handle_error "MySQL 版本升级失败，当前版本: $UPGRADED_VERSION，目标版本: $TARGET_MYSQL_VERSION"
fi

echo -e "${GREEN}✓ MySQL 成功升级到版本 $TARGET_MYSQL_VERSION${NC}"

echo ""
echo "================== 重新编译项目 =================="

# 14. Maven 清理并重新编译
if command -v mvn > /dev/null 2>&1; then
    execute_step "Maven 清理项目" "mvn clean"
    execute_step "Maven 重新编译" "mvn install -DskipTests"
else
    log_message "Maven 未安装，跳过编译步骤"
fi

# 15. 重新构建 Docker 镜像
execute_step "重新构建应用 Docker 镜像" "docker-compose build --no-cache payment manager merchant"

echo ""
echo "================== 启动服务 =================="

# 16. 启动所有服务
execute_step "启动所有服务" "docker-compose up -d"

# 17. 等待服务启动
echo -e "${BLUE}等待服务启动完成...${NC}"
sleep 20

echo ""
echo "================== 升级验证 =================="

# 18. 验证数据库连接
execute_step "验证数据库连接" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_pay_order LIMIT 1;'"

# 19. 检查应用服务状态
APP_PORTS=("9216" "9217" "9218")
for port in "${APP_PORTS[@]}"; do
    execute_step "检查端口 $port 服务状态" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/ | grep -E '200|404|302'" true
done

# 20. 验证容器状态
execute_step "验证所有容器运行状态" "docker ps | grep jeepay"

echo ""
echo "================== 性能基线测试 =================="

# 21. 基础性能测试
log_message "执行性能基线测试..."

# 数据库查询性能测试
QUERY_START_TIME=$(date +%s%3N)
docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "USE $DATABASE_NAME; SELECT COUNT(*) FROM t_pay_order;" > /dev/null 2>&1
QUERY_END_TIME=$(date +%s%3N)
QUERY_TIME=$((QUERY_END_TIME - QUERY_START_TIME))

log_message "查询性能测试结果: ${QUERY_TIME}ms"

if [ $QUERY_TIME -lt 1000 ]; then
    echo -e "${GREEN}✓ 查询性能正常 (${QUERY_TIME}ms)${NC}"
else
    echo -e "${YELLOW}⚠ 查询性能需要关注 (${QUERY_TIME}ms)${NC}"
fi

# 22. 收集升级后系统信息
cat > "upgrade_result_$(date +%Y%m%d_%H%M%S).txt" << EOF
Jeepay MySQL 升级结果报告
========================

升级时间: $(date '+%Y-%m-%d %H:%M:%S')
原版本: $CURRENT_VERSION
新版本: $UPGRADED_VERSION
数据库: $DATABASE_NAME

升级状态: 成功
备份位置: $BACKUP_DIR
日志文件: $UPGRADE_LOG_FILE

容器状态:
$(docker ps | grep jeepay)

数据库状态:
版本: $(docker exec $MYSQL_CONTAINER mysql --version)
连接状态: $(docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 'OK' as status;" 2>/dev/null || echo "连接失败")

性能指标:
查询响应时间: ${QUERY_TIME}ms

建议后续操作:
1. 监控系统运行 24 小时
2. 检查应用日志是否有异常
3. 验证关键业务功能
4. 如无问题可清理旧备份文件
EOF

echo ""
echo "================== 升级完成 =================="

echo -e "${GREEN}🎉 MySQL 升级成功完成！${NC}"
echo ""
echo "升级概要："
echo "- 原版本: $CURRENT_VERSION"
echo "- 新版本: $UPGRADED_VERSION"
echo "- 备份位置: $BACKUP_DIR"
echo "- 日志文件: $UPGRADE_LOG_FILE"
echo ""
echo -e "${YELLOW}后续建议操作：${NC}"
echo "1. 监控系统运行状态，特别关注应用日志"
echo "2. 测试关键业务功能（用户登录、支付流程等）"
echo "3. 检查 Druid 监控面板: http://localhost:9217/druid/"
echo "4. 如果 24 小时内运行正常，可清理备份文件"
echo ""
echo -e "${BLUE}如遇问题，可使用以下命令回滚：${NC}"
echo "bash scripts/rollback.sh $BACKUP_DIR"
echo ""
echo "升级完成时间: $(date '+%Y-%m-%d %H:%M:%S')"

log_message "MySQL 升级成功完成"