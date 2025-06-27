# 使用基于 Alpine Linux 的 Eclipse Temurin 17 JRE 作为基础镜像，适用于 AMD64 架构
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制本地打包好的 JAR 文件到镜像中（假设 JAR 在 target 目录下）
# 请确保你的 JAR 文件名与此一致，或者进行相应修改
COPY target/random-gallery-0.0.1-SNAPSHOT.jar app.jar

# 声明容器运行时监听的端口（根据实际应用端口调整）
EXPOSE 8086

# 设置默认的数据库环境变量
# 这些值可以在 'docker run' 命令中使用 '-e' 标志进行覆盖
ENV DB_HOST=111.111.111.111
ENV DB_PORT=3306
ENV DB_NAME=test
ENV DB_USERNAME=root
ENV DB_PASSWORD=root

ENTRYPOINT ["java", "-jar", "app.jar"]
# 导出镜像tar

# docker build -t helloworld:latest .
# docker save -o helloworld.tar helloworld:latest