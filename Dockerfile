# 使用 JDK8 基础镜像（根据实际需求选择，如 openjdk:8-jre）
# FROM openjdk:8-jre-alpine
# FROM eclipse-temurin:8-jre-alpine
FROM --platform=linux/amd64 eclipse-temurin:8-jre-alpine
# 设置工作目录
WORKDIR /app

# 复制本地打包好的 JAR 文件到镜像中（假设 JAR 在 target 目录下）
COPY target/helloworld_jdk8.jar .

# 声明容器运行时监听的端口（根据实际应用端口调整）
EXPOSE 8086

# 启动命令：运行 JAR 文件
ENTRYPOINT ["java", "-jar", "helloworld_jdk8.jar"]

# 导出镜像tar

# docker build -t helloworld:latest .
# docker save -o helloworld.tar helloworld:latest
