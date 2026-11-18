FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY storage-service/build.gradle ./build.gradle

RUN chmod +x ./gradlew \
    && printf "rootProject.name = 'storage-service'\n" > settings.gradle \
    && ./gradlew dependencies --no-daemon

COPY storage-service/src ./src
RUN ./gradlew assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8084
CMD ["java", "-jar", "app.jar"]