# Multi-stage build for LuckXpress Backend

# Stage 1: Build
FROM maven:3.9-openjdk-17-slim AS build
WORKDIR /app

# Copy POM files
COPY pom.xml .
COPY luckxpress-core/pom.xml luckxpress-core/
COPY luckxpress-data/pom.xml luckxpress-data/
COPY luckxpress-service/pom.xml luckxpress-service/
COPY luckxpress-remote/pom.xml luckxpress-remote/
COPY luckxpress-web/pom.xml luckxpress-web/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY luckxpress-core/src luckxpress-core/src
COPY luckxpress-data/src luckxpress-data/src
COPY luckxpress-service/src luckxpress-service/src
COPY luckxpress-remote/src luckxpress-remote/src
COPY luckxpress-web/src luckxpress-web/src

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from build stage
COPY --from=build /app/luckxpress-web/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
