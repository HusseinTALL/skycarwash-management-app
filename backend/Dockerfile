# ── Build stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Cache dependency layer separately from source layer
COPY pom.xml .
RUN apk add --no-cache maven && mvn -q dependency:go-offline -B

COPY src ./src

# Build fat jar (tests are run separately in CI)
RUN mvn -q -DskipTests package

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

EXPOSE 8080

# Docker health check – maps to Spring Boot Actuator liveness probe
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# Use shell form (not exec/JSON form) so ${SPRING_PROFILES_ACTIVE:-prod} is
# expanded by /bin/sh at container start – exec form does NOT do shell expansion.
# JVM flags tuned for containerised workloads:
#   UseContainerSupport   → respect cgroup memory limits
#   MaxRAMPercentage      → use up to 75% of container RAM for heap
#   ExitOnOutOfMemoryError → crash immediately so the orchestrator restarts cleanly
ENTRYPOINT java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
    -jar app.jar
