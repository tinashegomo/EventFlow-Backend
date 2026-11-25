# syntax=docker/dockerfile:1
# ============================================================================
# EventFlow backend image - Spring Boot 3.5 on Java 21
# ============================================================================
# Build (from backend/EventManagementSystem):
#   docker build -t eventflow-backend .
#
# Run - every secret comes from the environment, nothing is baked in:
#   docker run -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e DB_URL="jdbc:mysql://host:3306/eventflow" \
#     -e DB_USER=eventflow -e DB_PASSWORD=... \
#     -e JWT_SECRET=... \
#     -e CORS_ALLOWED_ORIGINS="https://app.example.com" \
#     eventflow-backend
#
# The full variable list lives in backend/EventManagementSystem/.env.example.
# ============================================================================

# ---------------------------------------------------------------------------
# Stage 1 - build the executable jar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Resolve dependencies from the POM alone so this layer is reused on every
# rebuild that does not touch pom.xml. The Maven cache is a BuildKit cache
# mount, so it stays out of the resulting layers.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Sources are copied only after the dependency layer, so editing Java code does
# not re-download the world.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 - runtime (JRE only, no build tools, no sources)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Run as an unprivileged user instead of root.
RUN addgroup -S eventflow && adduser -S -G eventflow eventflow

# spring-boot-maven-plugin (inherited from the parent POM) rewrites the jar into
# an executable fat jar, which is why it can be started with `java -jar`.
COPY --from=build --chown=eventflow:eventflow \
    /build/target/EventManagementSystem-0.0.1-SNAPSHOT.jar /app/app.jar

USER eventflow

# Override at runtime, e.g. -e JAVA_OPTS="-Xms256m -Xmx512m".
# PORT is read by application.properties (server.port=${PORT:8080}), but the
# container port must still be published with `-p` or declared in the platform.
ENV JAVA_OPTS="" \
    TZ=UTC

EXPOSE 8080

# exec keeps the JVM as PID 1 so Spring Boot receives SIGTERM and shuts down
# gracefully instead of being killed after the timeout.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
