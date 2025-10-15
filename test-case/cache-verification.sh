#!/bin/bash

# Jeepay缓存优化验证脚本
# 用于验证缓存功能是否正常工作

echo "========================================="
echo "Jeepay缓存优化功能验证"
echo "========================================="

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Redis连接配置
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_DB=${REDIS_DB:-1}

# 检查Redis连接
check_redis() {
    echo -e "\n${YELLOW}1. 检查Redis连接...${NC}"
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB PING > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Redis连接正常${NC}"
        return 0
    else
        echo -e "${RED}✗ Redis连接失败${NC}"
        return 1
    fi
}

# 检查布隆过滤器
check_bloom_filters() {
    echo -e "\n${YELLOW}2. 检查布隆过滤器...${NC}"
    
    # 检查商户编号布隆过滤器
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB EXISTS "bloom:mch:no" | grep -q "1"; then
        count=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB BF.INFO "bloom:mch:no" | grep "Number of items inserted" -A1 | tail -n1)
        echo -e "${GREEN}✓ 商户编号布隆过滤器已初始化，包含 $count 个元素${NC}"
    else
        echo -e "${YELLOW}⚠ 商户编号布隆过滤器未初始化（应用启动后会自动初始化）${NC}"
    fi
    
    # 检查商户应用布隆过滤器
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB EXISTS "bloom:mch:app" | grep -q "1"; then
        count=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB BF.INFO "bloom:mch:app" | grep "Number of items inserted" -A1 | tail -n1)
        echo -e "${GREEN}✓ 商户应用布隆过滤器已初始化，包含 $count 个元素${NC}"
    else
        echo -e "${YELLOW}⚠ 商户应用布隆过滤器未初始化${NC}"
    fi
    
    # 检查服务商布隆过滤器
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB EXISTS "bloom:isv:no" | grep -q "1"; then
        count=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB BF.INFO "bloom:isv:no" | grep "Number of items inserted" -A1 | tail -n1)
        echo -e "${GREEN}✓ 服务商编号布隆过滤器已初始化，包含 $count 个元素${NC}"
    else
        echo -e "${YELLOW}⚠ 服务商编号布隆过滤器未初始化${NC}"
    fi
}

# 检查缓存Key命名规范
check_cache_keys() {
    echo -e "\n${YELLOW}3. 检查缓存Key命名规范...${NC}"
    
    # 查找所有缓存Key
    echo "正在扫描Redis中的缓存Key..."
    
    # 检查商户信息缓存
    mch_keys=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB KEYS "mch:info:*" 2>/dev/null | wc -l)
    if [ $mch_keys -gt 0 ]; then
        echo -e "${GREEN}✓ 发现 $mch_keys 个商户信息缓存 (mch:info:*)${NC}"
    fi
    
    # 检查商户应用缓存
    app_keys=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB KEYS "mch:app:*" 2>/dev/null | wc -l)
    if [ $app_keys -gt 0 ]; then
        echo -e "${GREEN}✓ 发现 $app_keys 个商户应用缓存 (mch:app:*)${NC}"
    fi
    
    # 检查认证Token缓存
    token_keys=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB KEYS "auth:token:*" 2>/dev/null | wc -l)
    if [ $token_keys -gt 0 ]; then
        echo -e "${GREEN}✓ 发现 $token_keys 个Token缓存 (auth:token:*)${NC}"
    fi
    
    if [ $mch_keys -eq 0 ] && [ $app_keys -eq 0 ] && [ $token_keys -eq 0 ]; then
        echo -e "${YELLOW}⚠ 未发现新命名规范的缓存Key（需要应用运行一段时间后产生）${NC}"
    fi
}

# 检查Spring Cache缓存空间
check_spring_cache() {
    echo -e "\n${YELLOW}4. 检查Spring Cache缓存空间...${NC}"
    
    cache_spaces=("mchInfo" "mchApp" "isvInfo" "payIfConfig" "sysConfig" "userPermission")
    
    for space in "${cache_spaces[@]}"; do
        keys=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB KEYS "${space}::*" 2>/dev/null | wc -l)
        if [ $keys -gt 0 ]; then
            echo -e "${GREEN}✓ 缓存空间 '${space}' 包含 $keys 个缓存条目${NC}"
        else
            echo -e "${YELLOW}⚠ 缓存空间 '${space}' 暂无数据${NC}"
        fi
    done
}

# 测试布隆过滤器功能
test_bloom_filter() {
    echo -e "\n${YELLOW}5. 测试布隆过滤器拦截功能...${NC}"
    
    # 测试一个肯定不存在的商户号
    test_mch_no="NONEXISTENT_MCH_12345678"
    
    if redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB BF.EXISTS "bloom:mch:no" "$test_mch_no" | grep -q "0"; then
        echo -e "${GREEN}✓ 布隆过滤器正确拦截了不存在的商户号: $test_mch_no${NC}"
    else
        echo -e "${YELLOW}⚠ 布隆过滤器未能拦截（可能是误判或布隆过滤器未初始化）${NC}"
    fi
}

# 显示缓存统计
show_cache_stats() {
    echo -e "\n${YELLOW}6. 缓存统计信息...${NC}"
    
    # Redis信息
    info=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB INFO stats 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        hits=$(echo "$info" | grep "keyspace_hits" | cut -d':' -f2 | tr -d '\r')
        misses=$(echo "$info" | grep "keyspace_misses" | cut -d':' -f2 | tr -d '\r')
        
        if [ -n "$hits" ] && [ -n "$misses" ]; then
            total=$((hits + misses))
            if [ $total -gt 0 ]; then
                hit_rate=$(awk "BEGIN {printf \"%.2f\", ($hits / $total) * 100}")
                echo -e "缓存命中次数: $hits"
                echo -e "缓存未命中次数: $misses"
                echo -e "${GREEN}缓存命中率: ${hit_rate}%${NC}"
                
                if (( $(echo "$hit_rate > 80" | bc -l) )); then
                    echo -e "${GREEN}✓ 缓存命中率良好${NC}"
                elif (( $(echo "$hit_rate > 60" | bc -l) )); then
                    echo -e "${YELLOW}⚠ 缓存命中率一般，建议优化${NC}"
                else
                    echo -e "${RED}✗ 缓存命中率较低，需要优化${NC}"
                fi
            fi
        fi
    fi
    
    # 显示Key总数
    total_keys=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT -n $REDIS_DB DBSIZE 2>/dev/null | cut -d':' -f2)
    if [ -n "$total_keys" ]; then
        echo -e "\nRedis DB${REDIS_DB} 总Key数: $total_keys"
    fi
}

# 提供优化建议
show_recommendations() {
    echo -e "\n${YELLOW}7. 优化建议...${NC}"
    
    echo "✓ 定期监控缓存命中率，目标90%以上"
    echo "✓ 监控布隆过滤器拦截率，确保有效防穿透"
    echo "✓ 关注热点数据的缓存策略"
    echo "✓ 定期检查缓存过期时间配置是否合理"
    echo "✓ 监控Redis内存使用情况"
}

# 主函数
main() {
    check_redis || exit 1
    check_bloom_filters
    check_cache_keys
    check_spring_cache
    test_bloom_filter
    show_cache_stats
    show_recommendations
    
    echo -e "\n========================================="
    echo -e "${GREEN}验证完成！${NC}"
    echo -e "========================================="
}

# 运行主函数
main
