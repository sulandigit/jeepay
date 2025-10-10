#!/bin/bash

# Jeepay MySQL 升级数据备份脚本
# 用途：创建完整的数据和配置备份，确保可安全回滚
# 执行方式：bash backup-data.sh

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
BACKUP_BASE_DIR="./backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_BASE_DIR}/mysql_upgrade_${TIMESTAMP}"

echo "======================================"
echo "Jeepay MySQL 升级数据备份"
echo "======================================"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "备份目录: $BACKUP_DIR"
echo ""

# 创建备份目录
mkdir -p "$BACKUP_DIR"/{data,config,docker,logs}

# 备份函数
backup_item() {
    local item_name=$1
    local backup_command=$2
    local target_path=$3
    
    echo -e "${BLUE}正在备份 $item_name ...${NC}"
    
    if eval "$backup_command"; then
        echo -e "${GREEN}✓ $item_name 备份完成${NC}"
        if [ -n "$target_path" ] && [ -f "$target_path" ]; then
            local file_size=$(du -h "$target_path" | cut -f1)
            echo "  文件大小: $file_size"
        fi
        return 0
    else
        echo -e "${RED}✗ $item_name 备份失败${NC}"
        return 1
    fi
}

# 信息收集函数
collect_backup_info() {
    echo -e "${YELLOW}收集系统信息...${NC}"
    
    # 创建备份信息文件
    cat > "$BACKUP_DIR/backup_info.txt" << EOF
Jeepay MySQL 升级备份信息
================================

备份时间: $(date '+%Y-%m-%d %H:%M:%S')
备份目录: $BACKUP_DIR
操作系统: $(uname -a)
Docker 版本: $(docker --version)
Docker Compose 版本: $(docker-compose --version)

当前 MySQL 版本:
$(docker exec $MYSQL_CONTAINER mysql --version 2>/dev/null || echo "无法获取MySQL版本")

数据库信息:
$(docker exec $MYSQL_CONTAINER mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "
SELECT 
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) AS 'DB Size (MB)',
    COUNT(*) AS 'Table Count'
FROM information_schema.tables 
WHERE table_schema='$DATABASE_NAME';
" 2>/dev/null || echo "无法获取数据库信息")

容器状态:
$(docker ps -a | grep jeepay)

网络状态:
$(docker network ls | grep jeepay)

数据卷状态:
$(docker volume ls | grep jeepay)
EOF

    echo -e "${GREEN}✓ 系统信息收集完成${NC}"
}

echo "================== 数据库备份 =================="

# 检查 MySQL 容器是否运行
if ! docker ps | grep -q "$MYSQL_CONTAINER"; then
    echo -e "${RED}错误: MySQL 容器 $MYSQL_CONTAINER 未运行${NC}"
    exit 1
fi

# 1. 数据库完整备份
backup_item "MySQL 数据库完整备份" \
    "docker exec $MYSQL_CONTAINER mysqldump -u root -p${MYSQL_ROOT_PASSWORD} --single-transaction --routines --triggers --complete-insert $DATABASE_NAME > '$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql'" \
    "$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql"

# 2. 数据库结构备份
backup_item "MySQL 数据库结构备份" \
    "docker exec $MYSQL_CONTAINER mysqldump -u root -p${MYSQL_ROOT_PASSWORD} --no-data --routines --triggers $DATABASE_NAME > '$BACKUP_DIR/data/${DATABASE_NAME}_schema_backup.sql'" \
    "$BACKUP_DIR/data/${DATABASE_NAME}_schema_backup.sql"

# 3. 关键表数据快照
echo -e "${BLUE}正在创建关键表数据快照...${NC}"
CRITICAL_TABLES=("t_pay_order" "t_mch_info" "t_pay_interface_define" "t_sys_user" "t_sys_config")

for table in "${CRITICAL_TABLES[@]}"; do
    backup_item "表 $table 数据快照" \
        "docker exec $MYSQL_CONTAINER mysql -u root -p${MYSQL_ROOT_PASSWORD} -e \"SELECT COUNT(*) AS '${table}_count' FROM ${DATABASE_NAME}.${table};\" > '$BACKUP_DIR/data/${table}_count.txt'" \
        "$BACKUP_DIR/data/${table}_count.txt"
done

echo ""
echo "================== Docker 数据卷备份 =================="

# 4. MySQL 数据卷备份
if docker volume ls | grep -q "jeepay_mysql"; then
    backup_item "MySQL 数据卷备份" \
        "docker run --rm -v jeepay_mysql:/data -v $(pwd)/${BACKUP_DIR}/docker:/backup ubuntu tar czf /backup/mysql_volume_backup.tar.gz /data" \
        "$BACKUP_DIR/docker/mysql_volume_backup.tar.gz"
else
    echo -e "${YELLOW}警告: 未找到 jeepay_mysql 数据卷${NC}"
fi

echo ""
echo "================== 配置文件备份 =================="

# 5. 关键配置文件备份
CONFIG_FILES=(
    "docker-compose.yml"
    "pom.xml"
    ".env"
)

for config_file in "${CONFIG_FILES[@]}"; do
    if [ -f "$config_file" ]; then
        backup_item "配置文件 $config_file" \
            "cp '$config_file' '$BACKUP_DIR/config/'" \
            "$BACKUP_DIR/config/$(basename $config_file)"
    else
        echo -e "${YELLOW}警告: 配置文件 $config_file 不存在${NC}"
    fi
done

# 6. conf 目录完整备份
if [ -d "conf" ]; then
    backup_item "conf 目录完整备份" \
        "cp -r conf '$BACKUP_DIR/config/'" \
        "$BACKUP_DIR/config/conf"
fi

# 7. docs 目录中的 install 配置备份
if [ -d "docs/install" ]; then
    backup_item "安装配置文件备份" \
        "cp -r docs/install '$BACKUP_DIR/config/'" \
        "$BACKUP_DIR/config/install"
fi

echo ""
echo "================== 应用日志备份 =================="

# 8. 应用日志备份（最近的日志）
LOG_DIRS=("logs/payment" "logs/manager" "logs/merchant")

for log_dir in "${LOG_DIRS[@]}"; do
    if [ -d "$log_dir" ]; then
        backup_item "日志目录 $log_dir" \
            "cp -r '$log_dir' '$BACKUP_DIR/logs/'" \
            "$BACKUP_DIR/logs/$(basename $log_dir)"
    fi
done

# 9. Docker 容器日志备份
CONTAINERS=("jeepay-mysql" "jeepay-payment" "jeepay-manager" "jeepay-merchant")

for container in "${CONTAINERS[@]}"; do
    if docker ps -a | grep -q "$container"; then
        backup_item "容器 $container 日志" \
            "docker logs $container --tail 1000 > '$BACKUP_DIR/logs/${container}.log' 2>&1" \
            "$BACKUP_DIR/logs/${container}.log"
    fi
done

echo ""
echo "================== 环境状态快照 =================="

# 10. 系统状态快照
collect_backup_info

# 11. Docker 环境状态
backup_item "Docker 环境状态快照" \
    "docker ps -a > '$BACKUP_DIR/docker_containers.txt' && docker images > '$BACKUP_DIR/docker_images.txt' && docker network ls > '$BACKUP_DIR/docker_networks.txt' && docker volume ls > '$BACKUP_DIR/docker_volumes.txt'" \
    "$BACKUP_DIR/docker_containers.txt"

echo ""
echo "================== 备份验证 =================="

# 验证备份完整性
echo -e "${BLUE}正在验证备份完整性...${NC}"

BACKUP_VALIDATION_FAILED=0

# 验证数据库备份文件
if [ -f "$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql" ]; then
    if grep -q "CREATE TABLE" "$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql"; then
        echo -e "${GREEN}✓ 数据库备份文件包含表结构${NC}"
    else
        echo -e "${RED}✗ 数据库备份文件可能不完整${NC}"
        BACKUP_VALIDATION_FAILED=1
    fi
    
    # 检查文件大小
    backup_size=$(stat -c%s "$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql")
    if [ $backup_size -gt 1024 ]; then
        echo -e "${GREEN}✓ 数据库备份文件大小正常 ($(du -h "$BACKUP_DIR/data/${DATABASE_NAME}_full_backup.sql" | cut -f1))${NC}"
    else
        echo -e "${RED}✗ 数据库备份文件过小，可能存在问题${NC}"
        BACKUP_VALIDATION_FAILED=1
    fi
else
    echo -e "${RED}✗ 数据库备份文件不存在${NC}"
    BACKUP_VALIDATION_FAILED=1
fi

# 验证配置文件备份
if [ -f "$BACKUP_DIR/config/docker-compose.yml" ] && [ -f "$BACKUP_DIR/config/pom.xml" ]; then
    echo -e "${GREEN}✓ 关键配置文件备份完整${NC}"
else
    echo -e "${RED}✗ 配置文件备份不完整${NC}"
    BACKUP_VALIDATION_FAILED=1
fi

echo ""
echo "================== 备份总结 =================="

# 计算备份总大小
TOTAL_BACKUP_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)

echo "备份位置: $BACKUP_DIR"
echo "备份大小: $TOTAL_BACKUP_SIZE"
echo "备份时间: $(date '+%Y-%m-%d %H:%M:%S')"

# 创建备份清单
cat > "$BACKUP_DIR/BACKUP_MANIFEST.txt" << EOF
Jeepay MySQL 升级备份清单
========================

备份时间: $(date '+%Y-%m-%d %H:%M:%S')
备份标识: mysql_upgrade_${TIMESTAMP}
备份大小: $TOTAL_BACKUP_SIZE

数据备份文件:
$(ls -la "$BACKUP_DIR/data/" 2>/dev/null || echo "无数据备份文件")

配置备份文件:
$(ls -la "$BACKUP_DIR/config/" 2>/dev/null || echo "无配置备份文件")

Docker 备份文件:
$(ls -la "$BACKUP_DIR/docker/" 2>/dev/null || echo "无Docker备份文件")

日志备份文件:
$(ls -la "$BACKUP_DIR/logs/" 2>/dev/null || echo "无日志备份文件")

备份验证状态: $([ $BACKUP_VALIDATION_FAILED -eq 0 ] && echo "通过" || echo "失败")

恢复说明:
1. 数据库恢复: docker exec jeepay-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} $DATABASE_NAME < data/${DATABASE_NAME}_full_backup.sql
2. 配置文件恢复: cp config/* ./
3. Docker 卷恢复: docker run --rm -v jeepay_mysql:/data -v $(pwd)/docker:/backup ubuntu tar xzf /backup/mysql_volume_backup.tar.gz -C /

重要提醒:
- 请妥善保管备份文件
- 验证备份完整性后再开始升级
- 升级成功后可适当清理旧备份
EOF

if [ $BACKUP_VALIDATION_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ 备份完成且验证通过！可以安全进行升级操作。${NC}"
    echo -e "\n${YELLOW}备份标识: mysql_upgrade_${TIMESTAMP}${NC}"
    echo -e "${YELLOW}备份位置: $BACKUP_DIR${NC}"
    echo -e "${YELLOW}恢复命令已保存在: $BACKUP_DIR/BACKUP_MANIFEST.txt${NC}"
    exit 0
else
    echo -e "\n${RED}✗ 备份验证失败！请检查备份文件完整性后再进行升级。${NC}"
    exit 1
fi