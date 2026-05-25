# --- Build stage ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Кешируем зависимости отдельным слоем
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# --- Run stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/WarehouseProjectServer-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
