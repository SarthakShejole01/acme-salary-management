# syntax=docker/dockerfile:1

# ---- 1. Angular build -------------------------------------------------------
FROM node:24-alpine AS frontend
WORKDIR /app/frontend
# Dependencies first, so this layer is reused until package*.json change.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. Spring Boot build ---------------------------------------------------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app/backend
# Gradle wrapper + build scripts first, so the dependency download is cached.
COPY backend/gradlew backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/gradle gradle
RUN ./gradlew --no-daemon --console=plain dependencies > /dev/null
COPY backend/src src
# The Angular build is served by Spring Boot from classpath:/static.
COPY --from=frontend /app/frontend/dist/frontend/browser src/main/resources/static
# Tests run in CI (they need Docker for Testcontainers); the image build only packages.
RUN ./gradlew --no-daemon --console=plain bootJar -x test

# ---- 3. Runtime -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=backend /app/backend/build/libs/*-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
# Sized for a small (512 MB) instance; override JAVA_TOOL_OPTIONS to change.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
