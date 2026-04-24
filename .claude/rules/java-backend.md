---
globs: backend/**/*.java
---

# ShipFlow Java/Spring Boot Rules

**Layer discipline**: Controller → Service → Repository. Never call Repository from Controller. Never add `@Transactional` to Controller methods.

**DTOs at boundaries**: Controllers receive and return DTOs only. Entity objects must not cross the controller boundary. DTOs live in `dto/<feature>/`.

**Security**: Every new controller method needs `@PreAuthorize`. Check `PERMISSION_MATRIX.md` for role capabilities. Never bypass `@PreAuthorize`.

**Soft delete only**: Use `deletedAt` timestamp. Never hard-delete user data.

**AI/LLM and vector store calls**: Route through backend service-layer abstractions. Controllers must never call vendor clients or HTTP endpoints directly (OpenAI, Ollama, Qdrant, etc.).

**Caching**: Use `@Cacheable` / `@CacheEvict`. Redis is the production cache store.

**Cross-cutting side effects**: Use Spring `ApplicationEventPublisher`. Never call notification/side-effect services directly from the primary service.

**After every change**: Run `cd backend && ./mvnw verify`. JaCoCo gate is 80% line coverage. If the build includes formatting checks, run those too — check the CI workflow in `.github/workflows/` for the exact commands.
