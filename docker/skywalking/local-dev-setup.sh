#!/bin/bash

################################################################################
# Jeepay 本地开发环境 SkyWalking Agent 启动脚本
# 
# 使用方法:
#   1. 在 IDEA 启动配置中添加 VM Options
#   2. 复制以下参数到 VM Options 文本框:
#      -javaagent:/path/to/skywalking-agent/skywalking-agent.jar
#      -DSW_AGENT_NAME=jeepay-payment
#      -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
#
# 或者使用此脚本设置环境变量后启动应用
################################################################################

# SkyWalking Agent 路径（请根据实际情况修改）
SKYWALKING_AGENT_HOME="/opt/skywalking-agent"

# OAP Server 地址
OAP_SERVER="127.0.0.1:11800"

# 服务名称（从参数获取，默认为 jeepay-local）
SERVICE_NAME=${1:-jeepay-local}

# 采样率（-1 表示全量采集）
SAMPLE_RATE=${2:--1}

# 日志级别
LOG_LEVEL=${3:-INFO}

# SkyWalking Agent JVM 参数
SKYWALKING_OPTS="-javaagent:${SKYWALKING_AGENT_HOME}/skywalking-agent.jar"
SKYWALKING_OPTS="${SKYWALKING_OPTS} -DSW_AGENT_NAME=${SERVICE_NAME}"
SKYWALKING_OPTS="${SKYWALKING_OPTS} -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=${OAP_SERVER}"
SKYWALKING_OPTS="${SKYWALKING_OPTS} -DSW_LOGGING_LEVEL=${LOG_LEVEL}"
SKYWALKING_OPTS="${SKYWALKING_OPTS} -DSW_AGENT_SAMPLE=${SAMPLE_RATE}"

echo "=============================================="
echo "SkyWalking Agent 配置信息："
echo "=============================================="
echo "Agent Home: ${SKYWALKING_AGENT_HOME}"
echo "Service Name: ${SERVICE_NAME}"
echo "OAP Server: ${OAP_SERVER}"
echo "Sample Rate: ${SAMPLE_RATE}"
echo "Log Level: ${LOG_LEVEL}"
echo "=============================================="
echo ""
echo "请将以下参数添加到 IDEA VM Options："
echo ""
echo "${SKYWALKING_OPTS}"
echo ""
echo "=============================================="

# 导出环境变量（可选）
export JAVA_OPTS="${SKYWALKING_OPTS}"
