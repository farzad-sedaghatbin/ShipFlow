#!/bin/bash

# ShipFlow - Production Launch Script
# This script builds and starts the application for production

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🚀 Building ShipFlow for Production..."
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Default values
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-shipflowdb}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

# AI Configuration
AI_RISK_ENABLED="${AI_RISK_ENABLED:-true}"
OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"
OLLAMA_MODEL="${OLLAMA_MODEL:-mistral}"

# Check if PostgreSQL connection details are set
echo -e "${BLUE}Database Configuration:${NC}"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  Username: $DB_USERNAME"
echo ""

echo -e "${BLUE}AI Configuration:${NC}"
echo "  Enabled: $AI_RISK_ENABLED"
echo "  Ollama URL: $OLLAMA_BASE_URL"
echo "  Model: $OLLAMA_MODEL"
echo ""

# Build Frontend
echo -e "${BLUE}Building Frontend...${NC}"
cd "$PROJECT_ROOT/frontend"

if [ ! -d "node_modules" ]; then
    npm install
fi

npm run build
echo -e "${GREEN}✓ Frontend built successfully${NC}"

# Copy frontend build to backend static resources
echo -e "${BLUE}Copying frontend build to backend...${NC}"
rm -rf "$PROJECT_ROOT/backend/src/main/resources/static"
mkdir -p "$PROJECT_ROOT/backend/src/main/resources/static"
cp -r dist/* "$PROJECT_ROOT/backend/src/main/resources/static/"
echo -e "${GREEN}✓ Frontend copied to backend${NC}"

# Build Backend
echo -e "${BLUE}Building Backend...${NC}"
cd "$PROJECT_ROOT/backend"

if [ ! -f "./mvnw" ]; then
    mvn -N io.takari:maven:wrapper -Dmaven=3.9.6
fi

chmod +x ./mvnw
./mvnw clean package -DskipTests -q
echo -e "${GREEN}✓ Backend built successfully${NC}"

# Start application
echo ""
echo -e "${BLUE}Starting Application...${NC}"
echo "=============================================="

export DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD
export AI_RISK_ENABLED OLLAMA_BASE_URL OLLAMA_MODEL

java -jar target/shipflow-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

