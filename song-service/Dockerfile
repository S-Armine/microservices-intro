FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY song-service/build.gradle ./build.gradle

RUN chmod +x ./gradlew \
    && printf "rootProject.name = 'song-service'\n" > settings.gradle \
    && ./gradlew dependencies --no-daemon

COPY song-service/src ./src
RUN ./gradlew assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]