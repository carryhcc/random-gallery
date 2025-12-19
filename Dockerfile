# 强制指定为 AMD64 架构的运行环境
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制由脚本在第一步编译生成的 jar 包
COPY target/random-gallery-0.0.1-SNAPSHOT.jar app.jar

# 声明端口
EXPOSE 8086

# 默认环境变量
ENV DB_HOST=111.111.111.111
ENV DB_PORT=3306
ENV DB_NAME=test
ENV DB_USERNAME=root
ENV DB_PASSWORD=root

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]