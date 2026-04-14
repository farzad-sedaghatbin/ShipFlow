# Architecture Overview

## Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.x (Java 21) |
| Frontend | React 18 + TypeScript + Vite |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| Auth | JWT + Spring Security |
| Audit | Hibernate Envers |

## Repository layout

```
shipflow/
├── backend/          # Spring Boot API (Maven)
│   └── src/main/java/.../shipflow/
│       ├── controller/   # 65 REST controllers
│       ├── service/      # 86 business-logic services
│       ├── entity/       # 63 JPA entities
│       ├── repository/   # 60+ Spring Data repositories
│       ├── dto/          # request/response DTOs per feature
│       ├── config/       # Spring configs
│       ├── security/     # JWT + RBAC
│       └── service/mcp/  # MCP client + server
├── frontend/         # React + Vite + TypeScript
│   └── src/
│       ├── pages/        # 65 page components
│       ├── components/   # 108+ reusable components
│       ├── services/     # API client layer
│       ├── hooks/        # Custom React hooks
│       └── contexts/     # React context providers
└── docs/             # VitePress documentation site
```

## Key architecture docs

| File | Topic |
|------|-------|
| `WISE_ARCHITECTURE.md` | AI-powered tech advice feature |
| `RAG_ARCHITECTURE.md` | RAG + vector stores |
| `PROJECT_TYPE_ARCHITECTURE.md` | Shape Up vs Kanban dual-mode |
| `PERMISSION_MATRIX.md` | RBAC roles and permissions |
| `MCP_SERVER_MILESTONE.md` | MCP server implementation |

## Layering rules

```
Controller → Service → Repository
```

- Controllers handle HTTP, auth checks (`@PreAuthorize`), DTOs
- Services contain business logic, fire Spring events
- Repositories are Spring Data JPA interfaces only
- Entities never cross the controller boundary — always map to DTOs

## AI architecture

LLM and vector store providers are pluggable via interface + Spring `@Profile`:

```
AI call → LlmProvider interface → OllamaProvider | OpenAiProvider | RunPodProvider
RAG    → VectorStoreProvider  → InMemory | Qdrant | ChromaDB
```

Never call AI providers directly — always route through the service layer.
