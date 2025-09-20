# Multi-stage build for LuckXpress Backend
FROM maven:3.9.5-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml .
COPY luckxpress-common/pom.xml luckxpress-common/
COPY luckxpress-core/pom.xml luckxpress-core/
COPY luckxpress-data/pom.xml luckxpress-data/
COPY luckxpress-service/pom.xml luckxpress-service/
COPY luckxpress-web/pom.xml luckxpress-web/
COPY luckxpress-app/pom.xml luckxpress-app/
COPY luckxpress-remote/pom.xml luckxpress-remote/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/luckxpress-app/target/luckxpress-app-*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appgroup /app

# Switch to app user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
