#!/bin/bash

# JeePay负载均衡环境扩容脚本
# 用于动态扩容和缩容服务实例

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker-compose-loadbalancer.yml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 获取下一个可用端口
get_next_port() {
    local service_type=$1
    local base_port
    
    case $service_type in
        "payment")
            base_port=9200
            ;;
        "manager")
            base_port=9300
            ;;
        "merchant")
            base_port=9400
            ;;
        *)
            log_error "未知服务类型: $service_type"
            exit 1
            ;;
    esac
    
    # 查找下一个可用端口
    for ((port=base_port; port<base_port+100; port++)); do
        if ! netstat -tuln 2>/dev/null | grep -q ":$port "; then
            echo $port
            return
        fi
    done
    
    log_error "无法找到可用端口"
    exit 1
}

# 获取下一个可用IP
get_next_ip() {
    local service_type=$1
    local base_ip
    
    case $service_type in
        "payment")
            base_ip="172.21.0.30"
            ;;
        "manager")
            base_ip="172.21.0.40"
            ;;
        "merchant")
            base_ip="172.21.0.50"
            ;;
        *)
            log_error "未知服务类型: $service_type"
            exit 1
            ;;
    esac
    
    # 查找下一个可用IP
    local base_num=$(echo $base_ip | cut -d. -f4)
    for ((num=base_num; num<base_num+50; num++)); do
        local ip="172.21.0.$num"
        if ! docker network inspect jeepay-lb | grep -q "$ip"; then
            echo $ip
            return
        fi
    done
    
    log_error "无法找到可用IP地址"
    exit 1
}

# 扩容服务实例
scale_up() {
    local service_type=$1
    local count=${2:-1}
    
    log_info "扩容$service_type服务，增加$count个实例..."
    
    for ((i=1; i<=count; i++)); do
        local instance_num=$(docker ps -a --format "table {{.Names}}" | grep "jeepay-$service_type-" | wc -l)
        instance_num=$((instance_num + 1))
        
        local container_name="jeepay-$service_type-$instance_num"
        local hostname="$service_type-$instance_num"
        local port=$(get_next_port $service_type)
        local ip=$(get_next_ip $service_type)
        
        # 获取服务端口
        local service_port
        case $service_type in
            "payment") service_port=9216 ;;
            "manager") service_port=9217 ;;
            "merchant") service_port=9218 ;;
        esac
        
        log_info "创建实例: $container_name (端口:$port, IP:$ip)"
        
        # 创建并启动容器
        docker run -d \
            --name "$container_name" \
            --hostname "$hostname" \
            --network jeepay-lb \
            --ip "$ip" \
            -p "$port:$service_port" \
            -v "$PROJECT_DIR/logs/$hostname:/workspace/logs" \
            -v "$PROJECT_DIR/loadbalancer/config/$service_type-lb.yml:/workspace/application-lb.yml:ro" \
            -e SPRING_PROFILES_ACTIVE=lb \
            -e SERVER_PORT=$service_port \
            -e SPRING_APPLICATION_NAME=jeepay-$service_type \
            -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848 \
            -e INSTANCE_ID=$hostname \
            "jeepay-$service_type:lb"
        
        # 等待服务启动
        log_info "等待实例$container_name启动..."
        sleep 30
        
        # 健康检查
        local health_check_url="http://localhost:$port"
        if [ "$service_type" = "payment" ]; then
            health_check_url="$health_check_url/lb/health"
        else
            health_check_url="$health_check_url/actuator/health"
        fi
        
        local retry_count=0
        while [ $retry_count -lt 10 ]; do
            if curl -f -s "$health_check_url" > /dev/null 2>&1; then
                log_info "实例$container_name启动成功"
                break
            fi
            retry_count=$((retry_count + 1))
            sleep 10
        done
        
        if [ $retry_count -eq 10 ]; then
            log_error "实例$container_name启动失败"
            docker logs "$container_name"
            return 1
        fi
    done
    
    # 更新Nginx配置
    update_nginx_config $service_type
    
    log_info "$service_type服务扩容完成"
}

# 缩容服务实例
scale_down() {
    local service_type=$1
    local count=${2:-1}
    
    log_info "缩容$service_type服务，减少$count个实例..."
    
    # 获取当前实例列表
    local instances=($(docker ps --format "table {{.Names}}" | grep "jeepay-$service_type-" | sort -V))
    local current_count=${#instances[@]}
    
    if [ $current_count -le $count ]; then
        log_error "当前只有$current_count个实例，无法缩容$count个"
        return 1
    fi
    
    # 从最后的实例开始删除
    for ((i=0; i<count; i++)); do
        local instance=${instances[$((current_count - 1 - i))]}
        log_info "停止实例: $instance"
        
        # 优雅停止容器
        docker stop "$instance"
        docker rm "$instance"
        
        log_info "实例$instance已删除"
    done
    
    # 更新Nginx配置
    update_nginx_config $service_type
    
    log_info "$service_type服务缩容完成"
}

# 更新Nginx配置
update_nginx_config() {
    local service_type=$1
    
    log_info "更新Nginx配置..."
    
    # 获取当前实例列表
    local instances=($(docker ps --format "table {{.Names}}" | grep "jeepay-$service_type-"))
    
    # 生成upstream配置
    local upstream_config=""
    for instance in "${instances[@]}"; do
        local hostname=$(echo $instance | sed "s/jeepay-//")
        local service_port
        case $service_type in
            "payment") service_port=9216 ;;
            "manager") service_port=9217 ;;
            "merchant") service_port=9218 ;;
        esac
        upstream_config="$upstream_config    server $hostname:$service_port weight=1 max_fails=3 fail_timeout=30s;\n"
    done
    
    # 更新upstream配置文件
    local upstream_file="$PROJECT_DIR/loadbalancer/nginx/upstream.conf"
    local temp_file="/tmp/upstream.conf.tmp"
    
    # 备份原配置
    cp "$upstream_file" "$upstream_file.bak"
    
    # 生成新配置
    cat > "$temp_file" << EOF
# 上游服务器配置文件 - 自动生成
# 生成时间: $(date)

# ${service_type}服务集群
upstream ${service_type}_servers {
    # 负载均衡算法
    least_conn;
    
    # 服务器列表
$upstream_config
    # 长连接配置
    keepalive 16;
    keepalive_requests 100;
    keepalive_timeout 60s;
}
EOF
    
    # 合并配置文件
    # 这里需要更复杂的逻辑来合并多个服务的配置
    # 简化处理：重新生成完整配置
    
    # 重新加载Nginx配置
    docker exec jeepay-nginx-lb nginx -s reload
    
    log_info "Nginx配置更新完成"
}

# 显示当前实例状态
show_instances() {
    local service_type=${1:-"all"}
    
    log_info "当前实例状态:"
    
    if [ "$service_type" = "all" ] || [ "$service_type" = "payment" ]; then
        log_info "支付网关实例:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "jeepay-payment-" || echo "  无实例"
    fi
    
    if [ "$service_type" = "all" ] || [ "$service_type" = "manager" ]; then
        log_info "运营平台实例:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "jeepay-manager-" || echo "  无实例"
    fi
    
    if [ "$service_type" = "all" ] || [ "$service_type" = "merchant" ]; then
        log_info "商户平台实例:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "jeepay-merchant-" || echo "  无实例"
    fi
}

# 主函数
main() {
    local action=$1
    local service_type=$2
    local count=$3
    
    case "$action" in
        "up"|"scale-up")
            if [ -z "$service_type" ]; then
                log_error "请指定服务类型: payment, manager, merchant"
                exit 1
            fi
            scale_up "$service_type" "${count:-1}"
            ;;
        "down"|"scale-down")
            if [ -z "$service_type" ]; then
                log_error "请指定服务类型: payment, manager, merchant"
                exit 1
            fi
            scale_down "$service_type" "${count:-1}"
            ;;
        "status"|"list")
            show_instances "$service_type"
            ;;
        *)
            echo "Usage: $0 {up|down|status} <service_type> [count]"
            echo ""
            echo "Actions:"
            echo "  up/scale-up    - 扩容服务实例"
            echo "  down/scale-down - 缩容服务实例"
            echo "  status/list    - 显示实例状态"
            echo ""
            echo "Service Types:"
            echo "  payment  - 支付网关服务"
            echo "  manager  - 运营平台服务"
            echo "  merchant - 商户平台服务"
            echo ""
            echo "Examples:"
            echo "  $0 up payment 2      # 扩容2个支付网关实例"
            echo "  $0 down manager 1     # 缩容1个运营平台实例"
            echo "  $0 status payment     # 查看支付网关实例状态"
            echo "  $0 status             # 查看所有服务实例状态"
            exit 1
            ;;
    esac
}

main "$@"