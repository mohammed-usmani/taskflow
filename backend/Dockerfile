# Build stage: full JDK + Gradle wrapper. Dependencies are resolved in their own
# layer first, so changing source code doesn't re-download them.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -q -x test

# Run stage: JRE only, non-root user. Much smaller image, no build tools in production.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
