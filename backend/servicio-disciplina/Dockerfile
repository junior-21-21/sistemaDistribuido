FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S padgroup && adduser -S paduser -G padgroup
COPY --from=build /app/target/*.jar app.jar
RUN chown paduser:padgroup app.jar
USER paduser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
