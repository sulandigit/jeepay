#
#   Jeepay 多模块 Docker 构建文件 - JDK 17
#   支持 manager、merchant、payment 模块
#

# ------   BUILD STAGE   ------

# 使用 JDK 17 进行编译
FROM eclipse-temurin:17-jdk as builder

MAINTAINER Terrfly

# 设置工作目录
WORKDIR /workspace

# 复制 Maven 配置文件
COPY pom.xml ./
COPY */pom.xml ./*/

# 复制源代码
COPY . .

# 编译项目
RUN ./mvnw clean package -DskipTests

# ------   RUNTIME STAGE   ------

# 使用 JDK 17 运行时镜像
FROM eclipse-temurin:17-jre

MAINTAINER Terrfly

# 配置环境变量，支持中文
ENV LANG=C.UTF-8

# 设置时区 东八区，解决日志时间不正确的问题
ENV TZ=Asia/Shanghai

# 构建参数
ARG PORT=9216
ARG PLATFORM=payment

# 对外映射的端口
EXPOSE ${PORT}

# 挂载目录
VOLUME ["/workspace/logs", "/workspace/uploads"]

# 创建目录
RUN mkdir -p /workspace

# 根据平台参数复制对应的 JAR 文件
COPY --from=builder /workspace/jeepay-${PLATFORM}/target/jeepay-${PLATFORM}.jar /workspace/app.jar

# 设置工作目录
WORKDIR /workspace

# JDK 17 优化的 JVM 参数
ENV JVM_OPTS="-Xms512m -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication \
    --add-opens java.base/java.lang=ALL-UNNAMED \
    --add-opens java.base/java.util=ALL-UNNAMED \
    --add-opens java.base/java.time=ALL-UNNAMED \
    --add-opens java.base/java.io=ALL-UNNAMED \
    --add-opens java.base/java.net=ALL-UNNAMED"

# 启动命令
CMD java ${JVM_OPTS} -jar app.jar

# ------   END   ------