---
globs: backend/**/*.java
---

# ShipFlow Java/Spring Boot Rules

**Layer discipline**: Controller → Service → Repository. Never call Repository from Controller. Never add `@Transactional` to Controller methods.

**DTOs at boundaries**: Controllers receive and return DTOs only. Entity objects must not cross the controller boundary. DTOs live in `dto/<feature>/`.

**Security**: Every new controller method needs `@PreAuthorize`. Check `PERMISSION_MATRIX.md` for role capabilities. Never bypass `@PreAuthorize`.

**Soft delete only**: Use `deletedAt` timestamp. Never hard-delete user data. Hibernate Envers audits all entity changes automatically — no manual audit logging needed.

**LLM and vector store calls**: Route through `service/llm/` and `service/vectorstore/` plugin systems. Never call HTTP directly to OpenAI/Ollama/Qdrant.

**Caching**: Use `@Cacheable` / `@CacheEvict`. Redis is the production store.

**Cross-cutting side effects**: Use Spring `ApplicationEventPublisher`. Never call notification/side-effect services directly from the primary service.

**MCP server tools**: Register in `McpToolDispatcher`. Check `properties.isWriteEnabled()` for any write tool. Delegate to the service layer — never call repositories directly from McpTools.

**After every change**: Run `cd backend && ./mvnw spotless:apply` then `./mvnw verify`. JaCoCo gate is 80% line coverage. CI fails on Spotless violations.
