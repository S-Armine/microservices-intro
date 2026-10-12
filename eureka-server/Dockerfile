FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew ./
COPY gradle gradle
COPY eureka-server/build.gradle ./build.gradle

RUN chmod +x ./gradlew \
    && printf "rootProject.name = 'eureka-server'\n" > settings.gradle \
    && ./gradlew dependencies --no-daemon

COPY eureka-server/src ./src
RUN ./gradlew assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8761
CMD ["java", "-jar", "app.jar"]