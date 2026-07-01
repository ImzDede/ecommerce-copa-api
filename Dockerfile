FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package && \
    JAR_FILE=$(find target -maxdepth 1 -name "*.jar" ! -name "*.original" | head -n 1) && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    mkdir -p /data/uploads && \
    chown -R spring:spring /app /data

ENV APP_UPLOAD_DIR=/data/uploads

COPY --from=build /app/app.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
