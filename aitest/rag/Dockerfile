# 1. 使用轻量级 JDK 17 镜像
FROM eclipse-temurin:17-jre-alpine

# 2. 设置工作目录
WORKDIR /app

# 3. 把本地打好的 jar 包直接复制进来（假设包名是 ai-service.jar）
COPY target/rag-1.0-SNAPSHOT.jar app.jar

# 4. 声明端口
EXPOSE 8009

# 5. 启动命令
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]