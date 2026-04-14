# REST API

ShipFlow exposes a full REST API documented via OpenAPI / Swagger.

## Swagger UI

When running locally: **http://localhost:8080/swagger-ui.html**

## Authentication

All API endpoints require a JWT token or API key.

### JWT (user sessions)

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "admin123" }
```

Response includes a `token` field. Pass it as:

```http
Authorization: Bearer <token>
```

### API keys (machine-to-machine)

Create API keys in Organization Settings → API Keys. Pass as:

```http
Authorization: Bearer <api-key>
```

## Public API (v1)

The public API is available at `/api/v1/public/` and designed for external integrations and CI/CD pipelines.

Key endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/public/tasks/{id}` | Get task by ID |
| `PATCH` | `/api/v1/public/tasks/{id}/status` | Update task status |
| `GET` | `/api/v1/public/projects` | List projects |
| `GET` | `/api/v1/public/cycles` | List cycles |

## Rate limiting

Auth and AI endpoints are rate-limited via Bucket4j:

| Endpoint group | Limit |
|---------------|-------|
| `/api/auth/**` | 10 requests / minute |
| `/api/search/**` | 30 requests / minute |
| AI endpoints | 20 requests / minute |

Rate limit headers (`X-RateLimit-Remaining`, `Retry-After`) are included in responses.
