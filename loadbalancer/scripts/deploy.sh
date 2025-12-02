#!/bin/bash

# JeePay负载均衡环境部署脚本
# 用于自动化部署和管理负载均衡环境

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

# 检查依赖
check_dependencies() {
    log_info "检查部署依赖..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    log_info "依赖检查通过"
}

# 构建镜像
build_images() {
    log_info "构建应用镜像..."
    
    cd "$PROJECT_DIR"
    
    # 构建基础依赖镜像
    log_info "构建基础依赖镜像..."
    docker build -t jeepay-deps:latest -f docs/Dockerfile .
    
    # 构建各服务镜像
    log_info "构建支付网关镜像..."
    docker build -t jeepay-payment:lb --build-arg PORT=9216 --build-arg PLATFORM=payment .
    
    log_info "构建运营平台镜像..."
    docker build -t jeepay-manager:lb --build-arg PORT=9217 --build-arg PLATFORM=manager .
    
    log_info "构建商户平台镜像..."
    docker build -t jeepay-merchant:lb --build-arg PORT=9218 --build-arg PLATFORM=merchant .
    
    log_info "镜像构建完成"
}

# 创建网络
create_network() {
    log_info "创建Docker网络..."
    
    if ! docker network ls | grep -q "jeepay-lb"; then
        docker network create jeepay-lb --subnet=172.21.0.0/16
        log_info "网络jeepay-lb创建成功"
    else
        log_info "网络jeepay-lb已存在"
    fi
}

# 启动基础设施
start_infrastructure() {
    log_info "启动基础设施服务..."
    
    cd "$PROJECT_DIR"
    
    # 启动MySQL
    log_info "启动MySQL..."
    docker-compose -f "$COMPOSE_FILE" up -d mysql
    
    # 等待MySQL启动
    log_info "等待MySQL启动..."
    sleep 30
    
    # 启动Redis
    log_info "启动Redis..."
    docker-compose -f "$COMPOSE_FILE" up -d redis
    
    # 启动ActiveMQ
    log_info "启动ActiveMQ..."
    docker-compose -f "$COMPOSE_FILE" up -d activemq
    
    # 启动Nacos
    log_info "启动Nacos..."
    docker-compose -f "$COMPOSE_FILE" up -d nacos
    
    # 等待服务启动
    log_info "等待基础设施服务启动..."
    sleep 60
    
    log_info "基础设施服务启动完成"
}

# 启动应用服务
start_applications() {
    log_info "启动应用服务..."
    
    cd "$PROJECT_DIR"
    
    # 启动支付网关集群
    log_info "启动支付网关集群..."
    docker-compose -f "$COMPOSE_FILE" up -d payment-1 payment-2 payment-3
    
    # 启动运营平台集群
    log_info "启动运营平台集群..."
    docker-compose -f "$COMPOSE_FILE" up -d manager-1 manager-2
    
    # 启动商户平台集群
    log_info "启动商户平台集群..."
    docker-compose -f "$COMPOSE_FILE" up -d merchant-1 merchant-2
    
    # 等待应用启动
    log_info "等待应用服务启动..."
    sleep 90
    
    log_info "应用服务启动完成"
}

# 启动负载均衡器
start_loadbalancer() {
    log_info "启动负载均衡器..."
    
    cd "$PROJECT_DIR"
    
    # 启动Nginx
    docker-compose -f "$COMPOSE_FILE" up -d nginx
    
    # 等待负载均衡器启动
    sleep 30
    
    log_info "负载均衡器启动完成"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    local failed_services=()
    
    # 检查基础设施
    if ! docker-compose -f "$COMPOSE_FILE" ps mysql | grep -q "Up"; then
        failed_services+=("mysql")
    fi
    
    if ! docker-compose -f "$COMPOSE_FILE" ps redis | grep -q "Up"; then
        failed_services+=("redis")
    fi
    
    if ! docker-compose -f "$COMPOSE_FILE" ps nacos | grep -q "Up"; then
        failed_services+=("nacos")
    fi
    
    # 检查应用服务
    for service in payment-1 payment-2 payment-3 manager-1 manager-2 merchant-1 merchant-2; do
        if ! docker-compose -f "$COMPOSE_FILE" ps $service | grep -q "Up"; then
            failed_services+=("$service")
        fi
    done
    
    # 检查负载均衡器
    if ! docker-compose -f "$COMPOSE_FILE" ps nginx | grep -q "Up"; then
        failed_services+=("nginx")
    fi
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        log_info "所有服务健康检查通过"
        return 0
    else
        log_error "以下服务健康检查失败: ${failed_services[*]}"
        return 1
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    docker-compose -f "$COMPOSE_FILE" ps
    
    log_info ""
    log_info "访问地址:"
    log_info "Nginx负载均衡器: http://localhost"
    log_info "Nacos控制台: http://localhost:8848/nacos"
    log_info "ActiveMQ控制台: http://localhost:8161"
}

# 停止服务
stop_services() {
    log_info "停止所有服务..."
    cd "$PROJECT_DIR"
    docker-compose -f "$COMPOSE_FILE" down
    log_info "服务停止完成"
}

# 清理环境
clean_environment() {
    log_info "清理环境..."
    cd "$PROJECT_DIR"
    docker-compose -f "$COMPOSE_FILE" down -v --rmi all
    docker network rm jeepay-lb 2>/dev/null || true
    log_info "环境清理完成"
}

# 主函数
main() {
    case "${1:-deploy}" in
        "deploy")
            log_info "开始部署JeePay负载均衡环境..."
            check_dependencies
            build_images
            create_network
            start_infrastructure
            start_applications
            start_loadbalancer
            
            if health_check; then
                log_info "部署成功！"
                show_status
            else
                log_error "部署失败，请检查服务状态"
                exit 1
            fi
            ;;
        "start")
            log_info "启动服务..."
            cd "$PROJECT_DIR"
            docker-compose -f "$COMPOSE_FILE" up -d
            health_check && show_status
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            sleep 10
            main start
            ;;
        "status")
            show_status
            health_check
            ;;
        "clean")
            clean_environment
            ;;
        "health")
            health_check
            ;;
        *)
            echo "Usage: $0 {deploy|start|stop|restart|status|clean|health}"
            echo ""
            echo "Commands:"
            echo "  deploy  - 完整部署环境（构建镜像+启动服务）"
            echo "  start   - 启动所有服务"
            echo "  stop    - 停止所有服务"
            echo "  restart - 重启所有服务"
            echo "  status  - 查看服务状态"
            echo "  clean   - 清理环境（删除容器、镜像、网络）"
            echo "  health  - 健康检查"
            exit 1
            ;;
    esac
}

main "$@"