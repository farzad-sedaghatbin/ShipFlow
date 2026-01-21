#!/bin/bash

# ShipFlow - Development Launch Script
# This script starts both backend and frontend in development mode

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Load environment variables from .env file if it exists
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo "📝 Loading environment variables from .env file..."
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

echo "🚀 Starting ShipFlow in Development Mode..."
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i:"$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

# Read AI provider from application-dev.properties or environment variable
# Spring Boot uses APP_AI_PROVIDER environment variable -> maps to app.ai.provider
AI_PROVIDER="${APP_AI_PROVIDER}"
if [ -z "$AI_PROVIDER" ]; then
    # Try to read from application-dev.properties
    if [ -f "$PROJECT_ROOT/backend/src/main/resources/application-dev.properties" ]; then
        AI_PROVIDER=$(grep -E "^app\.ai\.provider=" "$PROJECT_ROOT/backend/src/main/resources/application-dev.properties" 2>/dev/null | cut -d'=' -f2)
    fi
    
    # If still not found, try application.properties
    if [ -z "$AI_PROVIDER" ] && [ -f "$PROJECT_ROOT/backend/src/main/resources/application.properties" ]; then
        AI_PROVIDER=$(grep -E "^app\.ai\.provider=" "$PROJECT_ROOT/backend/src/main/resources/application.properties" 2>/dev/null | cut -d'=' -f2 | sed 's/\${AI_PROVIDER:\([^}]*\)}/\1/')
    fi
    
    # Default to runpod if not found
    AI_PROVIDER="${AI_PROVIDER:-runpod}"
fi

echo -e "${BLUE}AI Provider: $AI_PROVIDER${NC}"

if ! command_exists java; then
    echo -e "${RED}Error: Java is not installed. Please install Java 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17+ is required. Found Java $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION found${NC}"

if ! command_exists node; then
    echo -e "${RED}Error: Node.js is not installed. Please install Node.js 18+${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Node.js $(node -v) found${NC}"

if ! command_exists npm; then
    echo -e "${RED}Error: npm is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ npm $(npm -v) found${NC}"

# Check AI Provider
AI_PROVIDER=${AI_PROVIDER:-ollama}
echo -e "${BLUE}Configured AI Provider: ${AI_PROVIDER}${NC}"

# Check for Ollama only if provider is 'ollama'
if [ "$AI_PROVIDER" = "ollama" ]; then
    echo -e "${BLUE}Checking Ollama (local AI provider)...${NC}"
    
    if command_exists ollama; then
        echo -e "${GREEN}✓ Ollama found${NC}"
        
        # Check if Ollama is running
        if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
            echo -e "${GREEN}✓ Ollama is running${NC}"
            
            # Check if Mistral model is available
            if ollama list | grep -q "mistral"; then
                echo -e "${GREEN}✓ Mistral model is available${NC}"
            else
                echo -e "${YELLOW}Mistral model not found. Pulling...${NC}"
                ollama pull mistral
                echo -e "${GREEN}✓ Mistral model pulled${NC}"
            fi
        else
            echo -e "${YELLOW}Ollama is installed but not running. Starting Ollama...${NC}"
            ollama serve > /dev/null 2>&1 &
            OLLAMA_PID=$!
            echo -e "${BLUE}Waiting for Ollama to start...${NC}"
            
            # Wait for Ollama to be ready (max 30 seconds)
            for i in {1..30}; do
                if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
                    echo -e "${GREEN}✓ Ollama started successfully${NC}"
                    
                    # Check/pull Mistral model
                    if ollama list | grep -q "mistral"; then
                        echo -e "${GREEN}✓ Mistral model is available${NC}"
                    else
                        echo -e "${YELLOW}Mistral model not found. Pulling...${NC}"
                        ollama pull mistral
                        echo -e "${GREEN}✓ Mistral model pulled${NC}"
                    fi
                    break
                fi
                sleep 1
            done
            
            if ! curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
                echo -e "${RED}Error: Ollama failed to start within 30 seconds${NC}"
                exit 1
            fi
        fi
    else
        echo -e "${RED}Error: Ollama is not installed${NC}"
        echo -e "${RED}  Install from: https://ollama.ai${NC}"
        echo -e "${RED}  Or change AI_PROVIDER to 'openai' or 'runpod' in your .env${NC}"
        exit 1
    fi
elif [ "$AI_PROVIDER" = "openai" ]; then
    echo -e "${GREEN}✓ Using OpenAI (ChatGPT) for AI${NC}"
    if [ -z "$APP_AI_OPENAI_API_KEY" ]; then
        echo -e "${RED}Error: APP_AI_OPENAI_API_KEY is not set${NC}"
        echo -e "${RED}  Please set it in your .env file${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ OpenAI API key is configured${NC}"
        echo -e "${BLUE}  Model: ${APP_AI_OPENAI_MODEL:-gpt-4-turbo-preview}${NC}"
    fi
elif [ "$AI_PROVIDER" = "runpod" ]; then
    echo -e "${GREEN}✓ Using RunPod for AI (cloud GPU)${NC}"
    if [ -z "$APP_AI_RUNPOD_API_KEY" ]; then
        echo -e "${RED}Error: APP_AI_RUNPOD_API_KEY is not set${NC}"
        echo -e "${RED}  Please set it in your .env file${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ RunPod API key is configured${NC}"
        echo -e "${BLUE}  Model: ${RUNPOD_MODEL:-mistral:instruct}${NC}"
    fi
else
    echo -e "${YELLOW}Warning: Unknown AI provider '${AI_PROVIDER}'${NC}"
    echo -e "${YELLOW}  Supported: ollama, openai, runpod${NC}"
    echo -e "${BLUE}  Continuing with configured provider...${NC}"
fi

# Check ports
if port_in_use 8080; then
    echo -e "${YELLOW}Warning: Port 8080 is already in use${NC}"
fi

if port_in_use 3000; then
    echo -e "${YELLOW}Warning: Port 3000 is already in use${NC}"
fi

# Start Backend
echo ""
echo -e "${BLUE}Starting Backend (Spring Boot)...${NC}"
cd "$PROJECT_ROOT/backend"

# Install Maven wrapper if not present
if [ ! -f "./mvnw" ]; then
    echo "Installing Maven wrapper..."
    mvn -N io.takari:maven:wrapper -Dmaven=3.9.6
fi

chmod +x ./mvnw

# Start backend in background
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!
echo -e "${GREEN}Backend starting with PID: $BACKEND_PID${NC}"

# Wait for backend to be ready
echo "Waiting for backend to start..."
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend is ready!${NC}"
        break
    fi
    if [ $i -eq 60 ]; then
        echo -e "${YELLOW}Backend may still be starting...${NC}"
    fi
    sleep 2
done

# Start Frontend
echo ""
echo -e "${BLUE}Starting Frontend (React + Vite)...${NC}"
cd "$PROJECT_ROOT/frontend"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

# Start frontend in background
npm run dev &
FRONTEND_PID=$!
echo -e "${GREEN}Frontend starting with PID: $FRONTEND_PID${NC}"

# Print summary
echo ""
echo "=================================================="
echo -e "${GREEN}🎉 ShipFlow is starting!${NC}"
echo ""
echo -e "  ${BLUE}Backend:${NC}  http://localhost:8080"
echo -e "  ${BLUE}API Docs:${NC} http://localhost:8080/swagger-ui.html"
echo -e "  ${BLUE}H2 Console:${NC} http://localhost:8080/h2-console"
echo -e "  ${BLUE}Frontend:${NC} http://localhost:3000"
if [ "$AI_PROVIDER" = "ollama" ]; then
    echo -e "  ${BLUE}Ollama:${NC}   http://localhost:11434"
else
    echo -e "  ${BLUE}AI Provider:${NC} RunPod (cloud GPU)"
fi
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
echo "=================================================="

# Trap Ctrl+C to kill both processes
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    kill $BACKEND_PID 2>/dev/null
    kill $FRONTEND_PID 2>/dev/null
    echo -e "${GREEN}All services stopped${NC}"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Wait for processes
wait
