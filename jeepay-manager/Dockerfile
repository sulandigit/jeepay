#
#   jeepay-manager（管理平台） java程序镜像构建脚本   .Power by terrfly
#

# ------   START   ------

# 基础镜像来自： https://hub.docker.com/ 官方OFFICIAL
FROM openjdk:8u342-jdk

MAINTAINER Terrfly

# 配置环境变量，支持中文。
ENV LANG=C.UTF-8

# 设置时区 东八区， 解决日志时间不正确的问题。
ENV TZ=Asia/Shanghai

# SkyWalking Agent 版本
ENV SKYWALKING_VERSION=8.16.0

# 对外映射的端口 ( 不明确EXPOSE 也不影响映射  )
EXPOSE 9217

# 挂载目录 "/jeepayhomes/service/app/application.yml 这个文件不能写到VOLUME中， 否则将映射成为了目录导致启动异常。
VOLUME ["/jeepayhomes/service/logs", "/jeepayhomes/service/uploads"]

# 使用jeepay用户启动。 ( 需要 RUN adduser等一系列操作 )
# USER jeepay:jeepay

# 创建目录
RUN mkdir /jeepayhomes/service/app -p

# 下载并安装 SkyWalking Agent
RUN cd /tmp && \
    wget -q https://archive.apache.org/dist/skywalking/${SKYWALKING_VERSION}/apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz && \
    tar -zxf apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz && \
    mv skywalking-agent /opt/skywalking-agent && \
    rm -rf apache-skywalking-java-agent-${SKYWALKING_VERSION}.tgz

# 设置 SkyWalking Agent 路径
ENV SW_AGENT_HOME=/opt/skywalking-agent

# 安装
COPY ./target/jeepay-manager.jar /jeepayhomes/service/app/jeepay-manager.jar

# 设置工作目录
WORKDIR /jeepayhomes/service/app

# 启动应用（通过环境变量 JAVA_OPTS 注入 SkyWalking Agent）
CMD java ${JAVA_OPTS} -jar jeepay-manager.jar

# ------   END   ------




