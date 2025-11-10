#
#   SkyWalking Agent Dockerfile
#   用于构建包含 SkyWalking Agent 的基础镜像
#

FROM openjdk:8u342-jdk

MAINTAINER Jeepay Team

# 配置环境变量，支持中文
ENV LANG=C.UTF-8

# 设置时区 东八区
ENV TZ=Asia/Shanghai

# SkyWalking Agent 版本
ENV SKYWALKING_VERSION=8.16.0

# 下载并安装 SkyWalking Agent
RUN cd /tmp && \
    wget https://archive.apache.org/dist/skywalking/${SKYWALKING_VERSION}/apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz && \
    tar -zxvf apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz && \
    mv skywalking-agent /opt/skywalking-agent && \
    rm -rf apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz

# 设置 SkyWalking Agent 路径
ENV SW_AGENT_HOME=/opt/skywalking-agent
