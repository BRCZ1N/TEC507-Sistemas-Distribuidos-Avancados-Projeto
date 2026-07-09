FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

WORKDIR /app/spring

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/spring/target/*.jar app.jar

EXPOSE 60000

ENTRYPOINT ["java", "-jar", "app.jar"]