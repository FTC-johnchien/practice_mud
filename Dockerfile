# 使用包含 JDK 25 的基底映像檔
#FROM eclipse-temurin:25-jdk
# 使用 JRE (Runtime) 而不是 JDK (Development Kit)
FROM eclipse-temurin:25-jre

# 設定工作目錄
WORKDIR /app

# 把你打包好的 jar 丟進去
COPY target/*.jar app.jar

# 設定啟動指令
ENTRYPOINT ["java", "-jar", "app.jar"]