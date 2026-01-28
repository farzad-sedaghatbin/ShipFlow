#!/bin/bash

# Build and run ShipFlow with Docker (without docker-compose)
# This script builds the Docker image and runs it with environment variables

set -e

echo "🔨 Building Docker image..."
docker build -t shipflow-app:latest .

echo "✅ Docker image built successfully!"

echo "🚀 Starting ShipFlow container..."

# Run the container with environment variables from .env file
# Note: Adjust these values or source them from .env
docker run -d \
  --name shipflow-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e AI_PROVIDER=${AI_PROVIDER:-ollama} \
  -e AI_RISK_ENABLED=true \
  -e RUNPOD_BASE_URL="${RUNPOD_BASE_URL:-}" \
  -e RUNPOD_API_KEY="${RUNPOD_API_KEY:-}" \
  -e RUNPOD_MODEL="${RUNPOD_MODEL:-mistral:instruct}" \
  -e RUNPOD_TIMEOUT="${RUNPOD_TIMEOUT:-180}" \
  -e RUNPOD_POLL_INTERVAL="${RUNPOD_POLL_INTERVAL:-2}" \
  -e OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://host.docker.internal:11434}" \
  -e OLLAMA_MODEL="${OLLAMA_MODEL:-mistral:instruct}" \
  -e OLLAMA_TIMEOUT="${OLLAMA_TIMEOUT:-180}" \
  -e QA_ENABLED=false \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=shipflowdb \
  -e DB_USERNAME=shipflow \
  -e DB_PASSWORD=shipflow_secret \
  shipflow-app:latest

echo "✅ ShipFlow container started!"
echo ""
echo "📊 View logs:"
echo "   docker logs -f shipflow-app"
echo ""
echo "🛑 Stop container:"
echo "   docker stop shipflow-app && docker rm shipflow-app"
echo ""
echo "🌐 Application will be available at: http://localhost:8080"
