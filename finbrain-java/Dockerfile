FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/finbrain-java-1.0.0.jar app.jar

EXPOSE 8080

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
