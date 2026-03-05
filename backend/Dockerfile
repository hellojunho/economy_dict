FROM gradle:8.6-jdk17 AS build
WORKDIR /app
COPY backend/build.gradle backend/settings.gradle ./
COPY backend/src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
