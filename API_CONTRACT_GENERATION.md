# API Contract Generation

This document explains how to generate and maintain TypeScript types from the backend OpenAPI specification.

## Overview

The project uses **OpenAPI 3.0** (via Springdoc) on the backend to automatically document all REST APIs. TypeScript types and API clients are then generated from this specification to ensure type safety and contract alignment between frontend and backend.

## Benefits

- **Type Safety**: Frontend code gets compile-time type checking against backend DTOs
- **Contract Alignment**: Eliminates API contract mismatches (like the v0.5.2 cooldown status enum issue)
- **Auto-completion**: IDEs provide intelligent suggestions for API requests/responses
- **Single Source of Truth**: Backend DTOs drive frontend types, no manual duplication
- **Refactoring Safety**: Backend API changes immediately surface as TypeScript errors

## Prerequisites

Backend must be running on `http://localhost:8080`. Start it with:

```bash
./scripts/start-dev.sh
```

## Generating Types

### Quick Start

```bash
cd frontend
npm run generate:api
```

This will:
1. Download the OpenAPI spec from the running backend (`/v3/api-docs`)
2. Generate TypeScript type definitions in `src/types/api-schema.d.ts`
3. Generate an API client in `src/api/generated/` (optional, for advanced usage)

### When to Regenerate

Regenerate types when:
- Backend DTOs are added, modified, or removed
- API endpoints change
- Request/response schemas are updated
- After pulling changes that affect backend models

### Workflow Integration

**Recommended workflow:**

1. Make backend changes (DTOs, controllers, etc.)
2. Start backend: `./scripts/start-dev.sh`
3. Generate types: `npm run generate:api`
4. Update frontend code to use new types
5. TypeScript compiler will catch any breaking changes

## Generated Files

### `src/types/api-schema.d.ts`

Type definitions for all API schemas. Example usage:

```typescript
import { components } from './types/api-schema';

type CooldownActivityDTO = components['schemas']['CooldownActivityDTO'];
type CreateCooldownActivityRequest = components['schemas']['CreateCooldownActivityRequest'];

const activity: CooldownActivityDTO = {
  id: 1,
  title: 'Fix bug',
  activityType: 'BUG_FIX',  // TypeScript knows valid values!
  status: 'COMPLETED',
  // ... TypeScript will validate all fields
};
```

### `src/api/generated/` (Optional)

Auto-generated API client with type-safe methods. Can replace manual service files:

```typescript
import { CooldownActivityService } from './api/generated';

// All methods are type-safe
const activities = await CooldownActivityService.getActivitiesByCycle(cycleId);
```

## CI/CD Integration

**Future Enhancement**: Add pre-commit hook or CI check to verify types are up-to-date:

```json
{
  "scripts": {
    "validate:api-types": "./scripts/check-api-types-sync.sh"
  }
}
```

This would fail the build if frontend types are out of sync with backend.

## OpenAPI Documentation

View interactive API documentation at: http://localhost:8080/swagger-ui.html

This provides:
- All available endpoints
- Request/response schemas
- Example payloads
- Try-it-out functionality

## Troubleshooting

### Backend not running

```
❌ Backend server not running at http://localhost:8080
```

**Solution**: Start the backend with `./scripts/start-dev.sh`

### Generation fails

**Solution**: 
1. Check backend logs for errors
2. Verify `/v3/api-docs` endpoint is accessible: `curl http://localhost:8080/v3/api-docs`
3. Ensure all controllers have proper OpenAPI annotations

### Types seem outdated

**Solution**: 
1. Stop backend
2. Clean build: `cd backend && mvn clean install`
3. Restart backend
4. Regenerate types: `npm run generate:api`

## Best Practices

1. **Always regenerate after backend changes**: Don't manually edit generated files
2. **Review generated types**: Check git diff to understand what changed
3. **Use generated types in services**: Import from `api-schema.d.ts` instead of duplicating DTOs
4. **Document custom types**: If you need wrapper types, document why they exist
5. **Version control**: Commit generated files so CI/CD and other developers stay in sync

## Related Files

- `frontend/generate-api-types.sh` - Generation script
- `backend/src/main/java/com/github/farzadsedaghatbin/shipflow/config/OpenApiConfig.java` - OpenAPI configuration
- `backend/pom.xml` - Springdoc dependency

## Resources

- [OpenAPI Specification](https://swagger.io/specification/)
- [Springdoc Documentation](https://springdoc.org/)
- [openapi-typescript](https://github.com/drwpow/openapi-typescript)
- [openapi-typescript-codegen](https://github.com/ferdikoomen/openapi-typescript-codegen)
