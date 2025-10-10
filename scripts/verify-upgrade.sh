#!/bin/bash

# Jeepay MySQL 升级后验证测试脚本
# 用途：全面验证升级后系统的功能性、性能和稳定性
# 执行方式：bash verify-upgrade.sh

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
VERIFY_LOG_FILE="mysql_verify_$(date +%Y%m%d_%H%M%S).log"

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
WARNING_TESTS=0

echo "======================================"
echo "Jeepay MySQL 升级后验证测试"
echo "======================================"
echo "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "日志文件: $VERIFY_LOG_FILE"
echo ""

# 记录日志函数
log_message() {
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] $1"
    echo "$message" | tee -a "$VERIFY_LOG_FILE"
}

# 测试函数
run_test() {
    local test_name=$1
    local test_command=$2
    local expected_result=$3
    local test_type=${4:-"CRITICAL"}  # CRITICAL, WARNING, INFO
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $test_name ... "
    log_message "开始测试: $test_name"
    
    if eval "$test_command" >> "$VERIFY_LOG_FILE" 2>&1; then
        echo -e "${GREEN}✓ PASS${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log_message "测试通过: $test_name"
        return 0
    else
        if [ "$test_type" = "WARNING" ]; then
            echo -e "${YELLOW}⚠ WARNING${NC}"
            WARNING_TESTS=$((WARNING_TESTS + 1))
            log_message "测试警告: $test_name"
            return 1
        else
            echo -e "${RED}✗ FAIL${NC}"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            log_message "测试失败: $test_name"
            if [ -n "$expected_result" ]; then
                echo "   期望结果: $expected_result"
            fi
            return 1
        fi
    fi
}

# 性能测试函数
performance_test() {
    local test_name=$1
    local test_command=$2
    local max_time_ms=$3
    
    echo -n "性能测试 $test_name ... "
    log_message "开始性能测试: $test_name (最大允许时间: ${max_time_ms}ms)"
    
    local start_time=$(date +%s%3N)
    if eval "$test_command" >> "$VERIFY_LOG_FILE" 2>&1; then
        local end_time=$(date +%s%3N)
        local elapsed_time=$((end_time - start_time))
        
        if [ $elapsed_time -le $max_time_ms ]; then
            echo -e "${GREEN}✓ PASS (${elapsed_time}ms)${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log_message "性能测试通过: $test_name (耗时: ${elapsed_time}ms)"
            return 0
        else
            echo -e "${YELLOW}⚠ SLOW (${elapsed_time}ms > ${max_time_ms}ms)${NC}"
            WARNING_TESTS=$((WARNING_TESTS + 1))
            log_message "性能测试警告: $test_name (耗时: ${elapsed_time}ms，超过预期: ${max_time_ms}ms)"
            return 1
        fi
    else
        echo -e "${RED}✗ FAIL${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log_message "性能测试失败: $test_name"
        return 1
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

echo "================== 基础环境验证 =================="

# 1. 容器状态检查
run_test "Docker 服务状态" "docker info > /dev/null 2>&1"
run_test "MySQL 容器运行状态" "docker ps | grep -q '$MYSQL_CONTAINER'"
run_test "Redis 容器运行状态" "docker ps | grep -q 'jeepay-redis'"
run_test "ActiveMQ 容器运行状态" "docker ps | grep -q 'jeepay-activemq'"

# 2. 应用服务状态检查
APP_CONTAINERS=("jeepay-payment" "jeepay-manager" "jeepay-merchant")
for container in "${APP_CONTAINERS[@]}"; do
    run_test "$container 容器状态" "docker ps | grep -q '$container'"
done

echo ""
echo "================== MySQL 版本和连接验证 =================="

# 3. MySQL 版本验证
MYSQL_VERSION=$(docker exec $MYSQL_CONTAINER mysql --version 2>/dev/null | grep -oP 'mysql\s+Ver\s+\K[0-9]+\.[0-9]+\.[0-9]+' || echo "unknown")
log_message "当前 MySQL 版本: $MYSQL_VERSION"

run_test "MySQL 版本检查" "echo '$MYSQL_VERSION' | grep -q '8.0.35'"
run_test "MySQL 连接测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SELECT 1' > /dev/null 2>&1"
run_test "数据库访问测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT 1' > /dev/null 2>&1"

echo ""
echo "================== 数据完整性验证 =================="

# 4. 关键表存在性检查
CRITICAL_TABLES=("t_pay_order" "t_mch_info" "t_pay_interface_define" "t_sys_user" "t_sys_config" "t_mch_app")

for table in "${CRITICAL_TABLES[@]}"; do
    run_test "表 $table 存在性" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; DESCRIBE $table;' > /dev/null 2>&1"
done

# 5. 数据记录数验证
run_test "支付订单表数据检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_pay_order;' > /dev/null 2>&1"
run_test "商户信息表数据检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_mch_info;' > /dev/null 2>&1"
run_test "系统配置表数据检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_sys_config;' > /dev/null 2>&1"

echo ""
echo "================== 应用连接验证 =================="

# 6. HTTP 服务可访问性测试
APP_PORTS=("9216" "9217" "9218" "9226" "9227" "9228")
APP_NAMES=("支付网关" "运营管理后端" "商户管理后端" "支付前端" "运营前端" "商户前端")

for i in "${!APP_PORTS[@]}"; do
    port=${APP_PORTS[$i]}
    name=${APP_NAMES[$i]}
    run_test "$name 服务可访问性 (端口 $port)" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/ | grep -E '200|404|302|500' > /dev/null" "" "WARNING"
done

echo ""
echo "================== 数据库性能验证 =================="

# 7. 数据库性能测试
performance_test "简单查询性能" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT 1;'" 100

performance_test "计数查询性能" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT COUNT(*) FROM t_pay_order;'" 1000

performance_test "表结构查询性能" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SHOW TABLES;'" 500

performance_test "索引查询性能" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SHOW INDEX FROM t_pay_order;'" 500

echo ""
echo "================== 连接池状态验证 =================="

# 8. 连接池和数据库连接验证
run_test "数据库连接数检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW STATUS LIKE \"Threads_connected\";' | grep -v 'Variable_name' | awk '{print \$2}' | head -1 | grep -E '^[0-9]+$'" "" "WARNING"

run_test "数据库最大连接数检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW VARIABLES LIKE \"max_connections\";' | grep -v 'Variable_name' | awk '{print \$2}' | head -1 | grep -E '^[0-9]+$'"

echo ""
echo "================== 业务功能验证 =================="

# 9. 基础 CRUD 操作测试
run_test "数据库写入测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; INSERT INTO t_sys_config (config_key, config_val, config_desc, group_key, group_name, group_sort, config_sort, type) VALUES (\"test_upgrade_$(date +%s)\", \"test_value\", \"升级测试配置\", \"TEST\", \"测试组\", 99, 99, \"text\") ON DUPLICATE KEY UPDATE config_val=\"test_value\";'"

run_test "数据库更新测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; UPDATE t_sys_config SET config_val=\"updated_value\" WHERE config_key LIKE \"test_upgrade_%\" AND group_key=\"TEST\";'"

run_test "数据库查询测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; SELECT * FROM t_sys_config WHERE config_key LIKE \"test_upgrade_%\" AND group_key=\"TEST\";'"

run_test "数据库删除测试" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'USE $DATABASE_NAME; DELETE FROM t_sys_config WHERE config_key LIKE \"test_upgrade_%\" AND group_key=\"TEST\";'"

echo ""
echo "================== 应用日志检查 =================="

# 10. 应用启动日志检查
for container in "${APP_CONTAINERS[@]}"; do
    if docker ps | grep -q "$container"; then
        run_test "$container 启动日志检查" "docker logs $container --tail 50 | grep -v -i 'error\\|exception\\|failed' > /dev/null || docker logs $container --tail 50 | grep -c -i 'error\\|exception\\|failed' | awk '{if(\$1<=3) exit 0; else exit 1}'" "" "WARNING"
    else
        log_message "容器 $container 未运行，跳过日志检查"
    fi
done

echo ""
echo "================== 配置验证 =================="

# 11. 配置文件验证
run_test "Maven 配置文件检查" "grep -q '8.0.35' pom.xml"
run_test "Docker Compose 配置检查" "grep -q 'mysql:8.0.35' docker-compose.yml"
run_test "代码生成器配置检查" "grep -q '8.0.35' jeepay-z-codegen/pom.xml"

echo ""
echo "================== 系统资源验证 =================="

# 12. 系统资源使用检查
run_test "磁盘空间检查" "df -h . | awk 'NR==2 {gsub(/%/, \"\", \$5); if(\$5 < 85) exit 0; else exit 1}'" "" "WARNING"

run_test "Docker 镜像检查" "docker images | grep -q 'mysql.*8.0.35'"

# 13. 内存使用检查
run_test "MySQL 容器内存使用" "docker stats $MYSQL_CONTAINER --no-stream --format 'table {{.MemUsage}}' | tail -1 | grep -v 'N/A'" "" "WARNING"

echo ""
echo "================== 新版本特性验证 =================="

# 14. MySQL 8.0.35 特性验证
run_test "字符集支持检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW VARIABLES LIKE \"character_set_server\";' | grep -q 'utf8mb4'"

run_test "认证插件检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW VARIABLES LIKE \"default_authentication_plugin\";' | grep -q 'caching_sha2_password\\|mysql_native_password'"

run_test "InnoDB 存储引擎检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW ENGINES;' | grep -q 'InnoDB.*YES'"

echo ""
echo "================== 安全性验证 =================="

# 15. 安全配置检查
run_test "SSL 变量检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW VARIABLES LIKE \"have_ssl\";'" "" "WARNING"

run_test "密码验证插件检查" "docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e 'SHOW PLUGINS;' | grep -q 'validate_password'" "" "WARNING"

echo ""
echo "================== 验证结果汇总 =================="

# 生成详细的验证报告
cat > "verification_report_$(date +%Y%m%d_%H%M%S).txt" << EOF
Jeepay MySQL 升级后验证报告
==========================

验证时间: $(date '+%Y-%m-%d %H:%M:%S')
MySQL 版本: $MYSQL_VERSION
数据库: $DATABASE_NAME

测试统计:
- 总测试数: $TOTAL_TESTS
- 通过测试: $PASSED_TESTS
- 失败测试: $FAILED_TESTS
- 警告测试: $WARNING_TESTS

通过率: $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)%

系统状态:
$(docker ps | grep jeepay)

MySQL 状态:
连接状态: $(docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SELECT 'OK' as status;" 2>/dev/null || echo "连接失败")
版本信息: $(docker exec $MYSQL_CONTAINER mysql --version)
连接数: $(docker exec $MYSQL_CONTAINER mysql -u root -p$MYSQL_ROOT_PASSWORD -e "SHOW STATUS LIKE 'Threads_connected';" 2>/dev/null | grep -v Variable_name | awk '{print $2}' || echo "获取失败")

详细日志: $VERIFY_LOG_FILE

建议:
EOF

# 添加建议
if [ $FAILED_TESTS -eq 0 ] && [ $WARNING_TESTS -eq 0 ]; then
    echo "- ✅ 升级验证完全通过，系统运行正常" >> "verification_report_$(date +%Y%m%d_%H%M%S).txt"
elif [ $FAILED_TESTS -eq 0 ] && [ $WARNING_TESTS -gt 0 ]; then
    echo "- ⚠️ 升级基本成功，有 $WARNING_TESTS 项警告需要关注" >> "verification_report_$(date +%Y%m%d_%H%M%S).txt"
else
    echo "- ❌ 升级存在 $FAILED_TESTS 项严重问题，建议检查或考虑回滚" >> "verification_report_$(date +%Y%m%d_%H%M%S).txt"
fi

echo "验证完成！"
echo ""
echo "📊 测试结果统计："
echo "   总测试数: $TOTAL_TESTS"
echo -e "   通过测试: ${GREEN}$PASSED_TESTS${NC}"
echo -e "   失败测试: ${RED}$FAILED_TESTS${NC}"
echo -e "   警告测试: ${YELLOW}$WARNING_TESTS${NC}"

if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_RATE=$(echo "scale=1; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc -l)
    echo "   通过率: $PASS_RATE%"
fi

echo ""
if [ $FAILED_TESTS -eq 0 ] && [ $WARNING_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 升级验证完全通过！系统运行正常。${NC}"
elif [ $FAILED_TESTS -eq 0 ] && [ $WARNING_TESTS -gt 0 ]; then
    echo -e "${YELLOW}⚠️ 升级基本成功，但有 $WARNING_TESTS 项警告需要关注。${NC}"
    echo -e "${YELLOW}建议查看日志文件了解详情：$VERIFY_LOG_FILE${NC}"
else
    echo -e "${RED}❌ 升级存在 $FAILED_TESTS 项严重问题！${NC}"
    echo -e "${RED}强烈建议检查问题或考虑回滚操作。${NC}"
    echo "回滚命令: bash scripts/rollback.sh [备份目录]"
fi

echo ""
echo "📋 验证报告已生成，详细信息请查看："
echo "   - 验证报告: verification_report_$(date +%Y%m%d_%H%M%S).txt"
echo "   - 详细日志: $VERIFY_LOG_FILE"
echo ""
echo "验证完成时间: $(date '+%Y-%m-%d %H:%M:%S')"

log_message "MySQL 升级验证测试完成"

# 返回适当的退出码
if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi