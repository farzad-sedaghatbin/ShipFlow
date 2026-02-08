# Build stage for frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Build stage for backend
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Copy frontend build to static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests -B

# Runtime stage (glibc-based for onnxruntime)
FROM eclipse-temurin:17.0.18_8-jre-jammy
WORKDIR /app

# Install runtime deps needed by onnxruntime-java (and healthcheck tool)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    libstdc++6 \
  && rm -rf /var/lib/apt/lists/*

# Add non-root user for security
RUN groupadd -r shipflow && useradd -r -g shipflow shipflow

# Copy the built jar
COPY --from=backend-builder /app/backend/target/shipflow-*.jar app.jar

# Change ownership
RUN chown -R shipflow:shipflow /app

USER shipflow

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]