FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/tmp/knowledge \
    && chown -R app:app /app

COPY --from=build /app/target/office-ai-assistant-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 8123

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
