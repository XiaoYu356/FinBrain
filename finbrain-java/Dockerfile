FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S finbrain && adduser -S finbrain -G finbrain

COPY target/*.jar app.jar

RUN chown -R finbrain:finbrain /app

USER finbrain

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
