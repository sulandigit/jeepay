#!/bin/bash

# Jeepay MySQL 升级回滚脚本
# 用途：在升级失败或出现问题时，快速回滚到升级前状态
# 执行方式：bash rollback.sh [备份目录]

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
ROLLBACK_LOG_FILE="mysql_rollback_$(date +%Y%m%d_%H%M%S).log"

echo "======================================"
echo "Jeepay MySQL 升级回滚脚本"
echo "======================================"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "日志文件: $ROLLBACK_LOG_FILE"
echo ""

# 检查备份目录参数
if [ $# -eq 0 ]; then
    echo -e "${RED}错误: 请提供备份目录路径${NC}"
    echo "用法: bash rollback.sh [备份目录路径]"
    echo ""
    echo "可用的备份目录："
    find ./backup -name "mysql_upgrade_*" -type d 2>/dev/null | sort -r | head -5
    exit 1
fi

BACKUP_DIR="$1"

# 验证备份目录
if [ ! -d "$BACKUP_DIR" ]; then
    echo -e "${RED}错误: 备份目录不存在: $BACKUP_DIR${NC}"
    exit 1
fi

if [ ! -f "$BACKUP_DIR/BACKUP_MANIFEST.txt" ]; then
    echo -e "${RED}错误: 备份清单文件不存在，可能不是有效的备份目录${NC}"
    exit 1
fi

echo "使用备份目录: $BACKUP_DIR"
echo ""

# 记录日志函数
log_message() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "$message" | tee -a "$ROLLBACK_LOG_FILE"
}

# 错误处理函数
handle_error() {
    local error_msg="$1"
    log_message "错误: $error_msg"
    echo -e "${RED}回滚失败: $error_msg${NC}"
    echo -e "${YELLOW}请检查日志文件: $ROLLBACK_LOG_FILE${NC}"
    exit 1
}

# 执行步骤函数
execute_step() {
    local step_name=$1
    local step_command=$2
    local skip_error=${3:-false}
    
    echo -e "${BLUE}正在执行: $step_name${NC}"
    log_message "开始执行: $step_name"
    
    if eval "$step_command" >> "$ROLLBACK_LOG_FILE" 2>&1; then
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
confirm_rollback() {
    echo -e "${YELLOW}警告: 即将执行回滚操作！${NC}"
    echo "这将会："
    echo "1. 停止所有应用服务"
    echo "2. 恢复 MySQL 容器和数据"
    echo "3. 恢复项目配置文件"
    echo "4. 重启所有服务"
    echo ""
    echo "备份信息："
    cat "$BACKUP_DIR/backup_info.txt" | head -10
    echo ""
    echo -n "确认要继续回滚吗？请输入 'ROLLBACK' 确认: "
    read -r response
    if [ "$response" != "ROLLBACK" ]; then
        log_message "用户取消回滚操作"
        echo "回滚操作已取消"
        exit 0
    fi
}

echo "================== 回滚前检查 =================="

# 显示备份信息
echo "备份信息概要："
cat "$BACKUP_DIR/backup_info.txt" | head -15

# 确认回滚操作
confirm_rollback

log_message "开始执行回滚操作，使用备份: $BACKUP_DIR"

echo ""
echo "================== 停止所有服务 =================="

# 1. 停止所有相关容器
execute_step "停止所有 Jeepay 容器" "docker-compose down" true

echo ""
echo "================== 恢复 MySQL 数据 =================="

# 2. 检查备份文件完整性
BACKUP_SQL_FILE="$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql"
if [ ! -f "$BACKUP_SQL_FILE" ]; then
    handle_error "数据库备份文件不存在: $BACKUP_SQL_FILE"
fi

# 3. 检查备份文件大小
BACKUP_SIZE=$(stat -c%s "$BACKUP_SQL_FILE")
if [ $BACKUP_SIZE -lt 1024 ]; then
    handle_error "数据库备份文件过小，可能损坏: $BACKUP_SQL_FILE"
fi

log_message "数据库备份文件检查通过，大小: $(du -h "$BACKUP_SQL_FILE" | cut -f1)"

# 4. 恢复配置文件
echo ""
echo "================== 恢复配置文件 =================="

# 恢复主要配置文件
CONFIG_FILES=("docker-compose.yml" "pom.xml" ".env")

for config_file in "${CONFIG_FILES[@]}"; do
    if [ -f "$BACKUP_DIR/config/$config_file" ]; then
        execute_step "恢复配置文件 $config_file" "cp '$BACKUP_DIR/config/$config_file' './$config_file'"
    else
        log_message "警告: 备份中未找到配置文件 $config_file"
    fi
done

# 恢复 conf 目录
if [ -d "$BACKUP_DIR/config/conf" ]; then
    execute_step "恢复 conf 目录" "rm -rf conf && cp -r '$BACKUP_DIR/config/conf' './conf'"
fi

# 恢复 docs/install 目录
if [ -d "$BACKUP_DIR/config/install" ]; then
    execute_step "恢复 install 配置" "rm -rf docs/install && cp -r '$BACKUP_DIR/config/install' './docs/install'"
fi

# 5. 启动 MySQL 容器（使用原配置）
echo ""
echo "================== 启动 MySQL 服务 =================="

execute_step "启动 MySQL 容器" "docker-compose up -d mysql"

# 等待 MySQL 启动
echo -e "${BLUE}等待 MySQL 启动完成...${NC}"
sleep 15

# 检查 MySQL 启动状态
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

# 6. 恢复数据库数据
echo ""
echo "================== 恢复数据库数据 =================="

# 创建数据库（如果不存在）
execute_step "确保数据库存在" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'CREATE DATABASE IF NOT EXISTS $DATABASE_NAME DEFAULT CHARACTER SET utf8mb4;'"

# 恢复数据库数据
log_message "开始恢复数据库数据，这可能需要几分钟..."
echo -e "${BLUE}正在恢复数据库数据，请等待...${NC}"

if docker exec -i $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD $DATABASE_NAME < "$BACKUP_SQL_FILE" >> "$ROLLBACK_LOG_FILE" 2>&1; then
    echo -e "${GREEN}✓ 数据库数据恢复完成${NC}"
    log_message "数据库数据恢复成功"
else
    handle_error "数据库数据恢复失败"
fi

# 7. 验证数据恢复
execute_step "验证数据库连接" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_pay_order LIMIT 1;'"

# 8. 重新编译项目（如果需要）
echo ""
echo "================== 重新编译项目 =================="

if command -v mvn > /dev/null 2>&1; then
    execute_step "Maven 清理项目" "mvn clean" true
    execute_step "Maven 重新编译" "mvn install -DskipTests" true
else
    log_message "Maven 未安装，跳过编译步骤"
fi

# 9. 重新构建 Docker 镜像
execute_step "重新构建应用镜像" "docker-compose build --no-cache payment manager merchant"

# 10. 启动所有服务
echo ""
echo "================== 启动所有服务 =================="

execute_step "启动所有服务" "docker-compose up -d"

# 等待服务启动
echo -e "${BLUE}等待服务启动完成...${NC}"
sleep 20

echo ""
echo "================== 回滚验证 =================="

# 11. 验证服务状态
APP_PORTS=("9216" "9217" "9218")
for port in "${APP_PORTS[@]}"; do
    execute_step "检查端口 $port 服务状态" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/ | grep -E '200|404|302'" true
done

# 12. 验证容器状态
execute_step "验证所有容器运行状态" "docker ps | grep jeepay"

# 13. 验证 MySQL 版本
CURRENT_VERSION=$(docker exec $MYSQL_CONTAINER mysql --version 2>/dev/null | grep -oP 'mysql\s+Ver\s+\K[0-9]+\.[0-9]+\.[0-9]+' || echo "unknown")
log_message "回滚后 MySQL 版本: $CURRENT_VERSION"

# 14. 基础功能测试
execute_step "数据库基础查询测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SHOW TABLES; SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema=\"$DATABASE_NAME\";'"

# 15. 生成回滚报告
cat > "rollback_result_$(date +%Y%m%d_%H%M%S).txt" << EOF
Jeepay MySQL 回滚结果报告
========================

回滚时间: $(date '+%Y-%m-%d %H:%M:%S')
使用备份: $BACKUP_DIR
回滚后版本: $CURRENT_VERSION
数据库: $DATABASE_NAME

回滚状态: 成功
日志文件: $ROLLBACK_LOG_FILE

容器状态:
$(docker ps | grep jeepay)

数据库状态:
版本: $(docker exec $MYSQL_CONTAINER mysql --version)
连接状态: $(docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 'OK' as status;" 2>/dev/null || echo "连接失败")

恢复的配置文件:
$(ls -la docker-compose.yml pom.xml 2>/dev/null || echo "配置文件状态未知")

建议后续操作:
1. 全面测试系统功能
2. 检查应用日志确认无异常
3. 验证关键业务流程
4. 分析升级失败原因
EOF

echo ""
echo "================== 回滚完成 =================="

echo -e "${GREEN}🔄 MySQL 回滚成功完成！${NC}"
echo ""
echo "回滚概要："
echo "- 使用备份: $BACKUP_DIR"
echo "- 当前版本: $CURRENT_VERSION"
echo "- 日志文件: $ROLLBACK_LOG_FILE"
echo ""
echo -e "${YELLOW}后续建议操作：${NC}"
echo "1. 全面测试系统功能，确保回滚成功"
echo "2. 检查应用日志，确认无异常错误"
echo "3. 验证关键业务流程（用户登录、支付等）"
echo "4. 分析升级失败的原因，为下次升级做准备"
echo ""
echo -e "${BLUE}系统访问地址：${NC}"
echo "- 运营管理: http://localhost:9227"
echo "- 商户管理: http://localhost:9228"
echo "- 支付网关: http://localhost:9226"
echo "- Druid监控: http://localhost:9217/druid/"
echo ""
echo "回滚完成时间: $(date '+%Y-%m-%d %H:%M:%S')"

log_message "MySQL 回滚成功完成"