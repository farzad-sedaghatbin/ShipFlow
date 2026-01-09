#!/bin/bash

# ShipFlow - Test Runner Script
# Runs all backend and frontend tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🧪 Running ShipFlow Tests..."
echo "===================================="

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

BACKEND_RESULT=0
FRONTEND_RESULT=0

# Run Backend Tests
echo ""
echo -e "${BLUE}Running Backend Tests...${NC}"
cd "$PROJECT_ROOT/backend"

if [ ! -f "./mvnw" ]; then
    mvn -N io.takari:maven:wrapper -Dmaven=3.9.6
fi

chmod +x ./mvnw

if ./mvnw test; then
    echo -e "${GREEN}✓ Backend tests passed${NC}"
else
    echo -e "${RED}✗ Backend tests failed${NC}"
    BACKEND_RESULT=1
fi

# Run Frontend Tests
echo ""
echo -e "${BLUE}Running Frontend Tests...${NC}"
cd "$PROJECT_ROOT/frontend"

if [ ! -d "node_modules" ]; then
    npm install
fi

if npm test -- --run --passWithNoTests; then
    echo -e "${GREEN}✓ Frontend tests passed${NC}"
else
    echo -e "${RED}✗ Frontend tests failed${NC}"
    FRONTEND_RESULT=1
fi

# Summary
echo ""
echo "===================================="
if [ $BACKEND_RESULT -eq 0 ] && [ $FRONTEND_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
