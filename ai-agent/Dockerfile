FROM maven:3.9-amazoncorretto-21
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

EXPOSE 8123

CMD ["java", "-jar", "/app/target/office-ai-assistant-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=prod"]
