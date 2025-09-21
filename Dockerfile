# Multi-stage build for optimized image size
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy POM files for dependency resolution
COPY pom.xml .
COPY luckxpress-common/pom.xml luckxpress-common/
COPY luckxpress-core/pom.xml luckxpress-core/
COPY luckxpress-data/pom.xml luckxpress-data/
COPY luckxpress-remote/pom.xml luckxpress-remote/
COPY luckxpress-service/pom.xml luckxpress-service/
COPY luckxpress-web/pom.xml luckxpress-web/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY luckxpress-common/src luckxpress-common/src
COPY luckxpress-core/src luckxpress-core/src
COPY luckxpress-data/src luckxpress-data/src
COPY luckxpress-remote/src luckxpress-remote/src
COPY luckxpress-service/src luckxpress-service/src
COPY luckxpress-web/src luckxpress-web/src

# Build application
RUN mvn clean package -DskipTests -P prod

# Extract layers for better caching
RUN java -Djarmode=layertools -jar luckxpress-web/target/*.jar extract

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install required packages
RUN apk add --no-cache \
    curl \
    jq \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

# Set working directory
WORKDIR /app

# Copy layers from builder
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/tmp && \
    chown -R spring:spring /app/logs /app/tmp

# Switch to non-root user
USER spring:spring

# JVM configuration for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
    -XX:InitialRAMPercentage=50 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8"

# Sentry configuration
ENV SENTRY_ENVIRONMENT="production" \
    SENTRY_TRACES_SAMPLE_RATE="0.1" \
    SENTRY_ATTACH_STACKTRACE="true"

# Application configuration
ENV SPRING_PROFILES_ACTIVE="prod" \
    SERVER_PORT="8080" \
    MANAGEMENT_SERVER_PORT="8081"

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
