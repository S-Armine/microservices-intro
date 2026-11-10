FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY api-gateway/build.gradle ./build.gradle

RUN chmod +x ./gradlew \
    && printf "rootProject.name = 'api-gateway'\n" > settings.gradle \
    && ./gradlew dependencies --no-daemon

COPY api-gateway/src ./src
RUN ./gradlew assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
