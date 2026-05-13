# --- build stage ---
FROM gradle:8.10-jdk17-alpine AS build
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle --no-daemon shadowJar

# --- runtime stage ---
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S vouch && adduser -S vouch -G vouch
WORKDIR /app
COPY --from=build /src/build/libs/selfhosted-checks.jar /app/selfhosted-checks.jar
USER vouch
ENTRYPOINT ["java", "-jar", "/app/selfhosted-checks.jar"]
