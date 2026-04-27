#!/bin/bash
# Generate TypeScript types from OpenAPI spec
# Requires backend to be running on http://localhost:8080

set -e

echo "🚀 Generating TypeScript types from OpenAPI spec..."

# Check if backend is running
if ! curl -s http://localhost:8080/v3/api-docs > /dev/null; then
  echo "❌ Backend server not running at http://localhost:8080"
  echo "Please start the backend server first with: ./scripts/start-dev.sh"
  exit 1
fi

# Download OpenAPI spec
echo "📥 Downloading OpenAPI spec..."
curl -s http://localhost:8080/v3/api-docs > openapi-spec.json

# Generate TypeScript types
echo "⚙️  Generating TypeScript types..."
npx openapi-typescript openapi-spec.json --output src/types/api-schema.d.ts

# Generate API client (optional)
echo "⚙️  Generating API client..."
npx openapi-typescript-codegen --input openapi-spec.json --output src/api/generated --client axios

# Clean up
rm openapi-spec.json

echo "✅ TypeScript types generated successfully!"
echo "📁 Location: src/types/api-schema.d.ts"
echo "📁 API Client: src/api/generated"
