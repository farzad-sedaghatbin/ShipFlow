# Redis Configuration

ShipFlow uses Redis for caching, rate limiting, and SSE connection management.

::: tip Full guide
See [`REDIS_CONFIGURATION_GUIDE.md`](https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/REDIS_CONFIGURATION_GUIDE.md) in the repository for the complete guide.
:::

## What Redis is used for

| Feature | Cache key pattern |
|---------|------------------|
| API response caching | `shipflow:api:*` |
| AI response caching | `shipflow:ai:*` |
| Rate limiting (Bucket4j) | `shipflow:ratelimit:*` |
| SSE emitter registry | In-memory only (not persisted) |

## Minimum configuration

```bash
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=changeme
```

## Production recommendations

- Use Redis 7+
- Enable `requirepass` and set a strong password
- Set `maxmemory-policy allkeys-lru` so Redis evicts cache entries under pressure rather than returning errors
- Enable persistence (`appendonly yes`) if you want rate limit state to survive restarts

## Running without Redis

Redis is required. The application will fail to start without a reachable Redis instance.
