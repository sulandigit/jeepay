# JeePay 负载均衡器配置

## 目录结构

```
loadbalancer/
├── README.md                    # 说明文档
├── nginx/                       # Nginx配置文件
│   ├── nginx.conf              # 主配置文件
│   ├── upstream.conf           # 上游服务器配置
│   ├── ssl/                    # SSL证书目录
│   └── logs/                   # 日志目录
├── nacos/                      # Nacos配置中心
│   ├── application.properties  # Nacos配置
│   └── config/                 # 配置文件目录
├── docker-compose-lb.yml      # 负载均衡Docker Compose配置
├── monitoring/                 # 监控配置
│   ├── prometheus.yml         # Prometheus配置
│   └── grafana/               # Grafana配置
└── scripts/                   # 运维脚本
    ├── health-check.sh        # 健康检查脚本
    ├── deploy.sh              # 部署脚本
    └── scale.sh               # 扩容脚本
```

## 负载均衡架构

### 第一层：Nginx反向代理
- 处理静态资源
- SSL终端
- 基础路由和限流
- 健康检查

### 第二层：应用层负载均衡
- 服务发现（Nacos）
- 智能路由
- 熔断降级
- 会话管理

## 服务配置

| 服务 | 端口 | 负载策略 | 健康检查 |
|-----|------|---------|---------|
| 支付网关 | 9216 | 加权轮询 | /actuator/health |
| 运营平台 | 9217 | 轮询 | /actuator/health |
| 商户平台 | 9218 | IP哈希 | /actuator/health |

## 部署说明

1. 启动Nacos注册中心
2. 启动应用服务实例
3. 启动Nginx负载均衡器
4. 配置监控系统

详细配置请参考各目录下的具体文件。