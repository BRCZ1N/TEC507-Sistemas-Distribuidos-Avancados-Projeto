FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

RUN echo "PWD:"
RUN pwd

RUN echo "FILES:"
RUN ls -la

RUN echo "POMS:"
RUN find / -name pom.xml 2>/dev/null || true

RUN mvn clean package