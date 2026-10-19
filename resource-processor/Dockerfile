FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY resource-processor/build.gradle ./build.gradle

RUN chmod +x ./gradlew \
    && printf "rootProject.name = 'resource-processor'\n" > settings.gradle \
    && ./gradlew dependencies --no-daemon

COPY resource-processor/src ./src
RUN ./gradlew assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
