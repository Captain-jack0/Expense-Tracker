# Multi-stage build for the Expense Tracker backend.
# Build stage uses a full Maven image (no need for a wrapper in the
# repo), runtime stage uses a slim JRE so the final image is small
# and doesn't ship build tooling to production.

# ==========================================================
# Build stage
# ==========================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy pom.xml first so Docker can cache the dependency resolution
# layer separately from source changes.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Copy sources and build the fat jar. Skip tests here — tests run
# in CI; this build only needs to produce the artifact.
COPY src ./src
RUN mvn -B -ntp -DskipTests clean package \
 && mv target/*.jar target/app.jar

# ==========================================================
# Runtime stage
# ==========================================================
FROM eclipse-temurin:17-jre-alpine

# curl is used by the docker-compose healthcheck to hit
# /actuator/health. Keep this package list tight.
RUN apk add --no-cache curl tini \
 && addgroup -S spring \
 && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/app.jar /app/app.jar

USER spring
EXPOSE 8080

# tini handles PID 1 signal forwarding so ctrl-c / docker stop
# terminate the JVM cleanly.
ENTRYPOINT ["/sbin/tini", "--", "java", "-jar", "/app/app.jar"]
