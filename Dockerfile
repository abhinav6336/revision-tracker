# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
