#!/bin/bash

# JeePay 负载均衡健康检查脚本
# 用于检查各服务实例的健康状态

set -e

# 配置参数
HEALTH_CHECK_TIMEOUT=5
LOG_FILE="/var/log/nginx/health-check.log"
SERVICES=(
    "payment-1:9216"
    "payment-2:9216" 
    "payment-3:9216"
    "manager-1:9217"
    "manager-2:9217"
    "merchant-1:9218"
    "merchant-2:9218"
)

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 健康检查函数
check_service_health() {
    local service=$1
    local host=$(echo $service | cut -d: -f1)
    local port=$(echo $service | cut -d: -f2)
    
    log "检查服务: $service"
    
    # TCP连接检查
    if ! timeout $HEALTH_CHECK_TIMEOUT nc -z $host $port; then
        log "ERROR: $service TCP连接失败"
        return 1
    fi
    
    # HTTP健康检查
    local health_url="http://$service/actuator/health"
    local response=$(curl -s -w "%{http_code}" -o /dev/null --connect-timeout $HEALTH_CHECK_TIMEOUT "$health_url" || echo "000")
    
    if [ "$response" = "200" ]; then
        log "OK: $service 健康检查通过"
        return 0
    else
        log "ERROR: $service 健康检查失败，HTTP状态码: $response"
        return 1
    fi
}

# 检查所有服务
check_all_services() {
    local failed_services=()
    
    log "开始健康检查..."
    
    for service in "${SERVICES[@]}"; do
        if ! check_service_health "$service"; then
            failed_services+=("$service")
        fi
    done
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        log "所有服务健康检查通过"
        exit 0
    else
        log "以下服务健康检查失败: ${failed_services[*]}"
        exit 1
    fi
}

# 单个服务检查
check_single_service() {
    local service=$1
    if check_service_health "$service"; then
        echo "healthy"
        exit 0
    else
        echo "unhealthy"
        exit 1
    fi
}

# 主函数
main() {
    case "${1:-all}" in
        "all")
            check_all_services
            ;;
        "payment-"*)
            check_single_service "$1:9216"
            ;;
        "manager-"*)
            check_single_service "$1:9217"
            ;;
        "merchant-"*)
            check_single_service "$1:9218"
            ;;
        *)
            echo "Usage: $0 [all|service-name]"
            echo "Examples:"
            echo "  $0 all                # 检查所有服务"
            echo "  $0 payment-1          # 检查单个支付服务"
            echo "  $0 manager-1          # 检查单个运营服务"
            echo "  $0 merchant-1         # 检查单个商户服务"
            exit 1
            ;;
    esac
}

main "$@"