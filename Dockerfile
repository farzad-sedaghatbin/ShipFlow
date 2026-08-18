# Build stage for frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install --no-audit
COPY frontend/ ./
RUN npm run build

# Build stage for backend
FROM maven:3.9-eclipse-temurin-21 AS backend-builder

# Build & install shipflow-plugin-api into the local Maven repo first — backend/pom.xml depends on
# it as a regular Maven dependency (v1.12.0 S60: a real, distributable Plugin SDK artifact, not a
# copy of the SPI interfaces baked into backend). Same local repo (~/.m2) persists across RUN
# layers in this stage, so the later `mvn dependency:go-offline` for backend resolves it locally.
# See plugin-sdk/README.md.
WORKDIR /app/plugin-sdk/shipflow-plugin-api
COPY plugin-sdk/shipflow-plugin-api/pom.xml ./
RUN mvn dependency:go-offline -B
COPY plugin-sdk/shipflow-plugin-api/src ./src
RUN mvn install -DskipTests -B

WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Copy frontend build to static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests -B

# Runtime stage (glibc-based for onnxruntime)
FROM eclipse-temurin:21.0.11_10-jre-jammy
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

# Create uploads directory so the named volume inherits shipflow ownership on first mount
RUN mkdir -p /app/uploads

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