# ShipFlow — Competitive Analysis

_Last updated: 2026-04-05 (v0.8.0)_

This document positions ShipFlow against the tools teams most commonly evaluate before adopting it.
It is written factually to help evaluators make an informed decision.

---

## 1. Market Positioning

ShipFlow targets teams that:

- Practice or want to adopt the **Shape Up methodology** (Basecamp's 6-week cycle framework)
- Need **AI-native** project management — not bolted-on AI, but AI woven into the workflow
- Run **self-hosted** infrastructure and need full data ownership
- Want **editor-first project context** — developers shouldn't need to switch apps to know what they're building

---

## 2. Head-to-Head Feature Matrix

| Feature | ShipFlow | Linear | Jira | Asana | Monday.com | Basecamp | Shortcut |
|---------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Shape Up methodology** | ✅ Full | ❌ | ❌ | ❌ | ❌ | Partial¹ | Partial² |
| **Kanban mode** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Dual mode (Shape Up + Kanban per project)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **6-week cycles with betting table** | ✅ | ❌ | ❌ | ❌ | ❌ | Partial | ❌ |
| **Hill charts** | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| **Circuit breaker (appetite enforcement)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Pitch lifecycle (IDEA→DRAFT→SHAPED→BET)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **MCP server (AI editor integration)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Figma MCP client** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI Q&A on project docs (RAG)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI Q&A multi-turn context + entity disambiguation** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Interactive onboarding tour** | ✅ | ❌ | ❌ | Partial | ❌ | ❌ | ❌ |
| **AI technical solution generator** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI test case generation** | ✅ | ❌ | ❌ | ❌ | Partial | ❌ | ❌ |
| **Pluggable LLM (Ollama/OpenAI/RunPod)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Pluggable vector store (Qdrant/Chroma/Memory)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI risk scoring per pitch** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI cycle narrative summaries** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Global search (⌘K)** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **GitHub integration** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Pluggable VCS providers** | ✅ | ❌ | Partial | ❌ | ❌ | ❌ | ❌ |
| **Slack / Teams notifications** | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| **Pluggable notification providers** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Generic inbound webhooks** | ✅ | ❌ | Partial | ❌ | Partial | ❌ | ❌ |
| **Full audit trail (Hibernate Envers)** | ✅ | Partial | ✅ | ❌ | ❌ | ❌ | Partial |
| **Multi-layer caching (ETag + Redis + React Query)** | ✅ | Partial | ❌ | ❌ | ❌ | ❌ | ❌ |
| **RTL language support** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Self-hosted** | ✅ | ❌ | ✅ (Data Center) | ❌ | ❌ | ❌ | ❌ |
| **Open source** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Free (self-hosted)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

¹ Basecamp invented Shape Up but does not implement it as a structured workflow in its own app.
² Shortcut has cycles and stories but no pitch/betting/hill-chart workflow.

---

## 3. Deep-Dive Comparisons

### 3.1 ShipFlow vs Linear

**Linear wins**: fastest UI, excellent keyboard navigation, strong engineering culture brand, good GitHub integration, polished mobile app.

**ShipFlow wins**:
- The only tool with a complete Shape Up workflow (pitches, betting, hill charts, circuit breaker)
- MCP server — Linear has no way to expose project data to AI editors as structured tool calls
- Self-hosted / open source — Linear is SaaS-only
- Pluggable AI stack — Ollama for local/private deployments, OpenAI for production
- RTL support (Persian, Arabic)
- No per-seat cost when self-hosted

**Who should use Linear**: Teams that don't practice Shape Up and want the fastest Kanban/sprint tool on the market.
**Who should use ShipFlow**: Teams that practice Shape Up, want AI-native workflows, or need self-hosted data ownership.

---

### 3.2 ShipFlow vs Jira

**Jira wins**: market leader, deep integrations ecosystem (thousands of plugins), enterprise SSO/SAML, advanced dashboards, Confluence integration.

**ShipFlow wins**:
- Zero configuration overhead — Shape Up workflow is first-class, not a custom template
- MCP server — Jira has no concept of exposing project data as AI tool calls
- AI features built-in (no plugins needed): RAG Q&A, technical solution generator, test generation, risk scoring
- Multi-layer caching for snappy performance (Jira is notoriously slow at scale)
- Open source (auditable, no license cost)
- Significantly simpler to self-host

**Who should use Jira**: Large enterprises with existing Jira ecosystems, compliance requirements, and dedicated Jira admins.
**Who should use ShipFlow**: Product and engineering teams that value methodology over configuration, want AI-native features, or need self-hosted simplicity.

---

### 3.3 ShipFlow vs Asana / Monday.com

Both are general-purpose work management tools with strong project templates and dashboards.

**ShipFlow wins on every Shape Up dimension** — neither Asana nor Monday.com has pitches, hill charts, betting tables, or circuit breakers. Both are adding AI features but none expose structured MCP interfaces for AI editors.

**They win on**: no-code automation, richer reporting dashboards, enterprise tier support, non-technical stakeholder adoption.

**Who should use them**: Cross-functional teams (marketing, ops, HR) that need a single work management platform. Not the right fit for engineering-first Shape Up teams.

---

### 3.4 ShipFlow vs Basecamp

Basecamp invented Shape Up but their own product does not implement the methodology's structured workflow (no formal hill charts, no betting table, no pitch lifecycle).

**ShipFlow wins**: Full Shape Up implementation, AI features, MCP server, open source, free self-hosted.
**Basecamp wins**: Simplicity, client communication features, non-technical user adoption, established brand.

---

## 4. The MCP Server Differentiator (v0.7.0+)

As of v0.7.0, ShipFlow is the **only project management tool** that implements the [Model Context Protocol](https://modelcontextprotocol.io) as a server — meaning AI coding assistants (Claude Code, Cursor, GitHub Copilot) can query your project board as a first-class tool call.

Atlassian launched their "Teamwork Graph CLI" for Claude Code in 2026, which indexes Jira, Confluence, and Bitbucket into a single graph. ShipFlow's `get_work_context` tool (added alongside this) offers the same relationship-graph pattern natively for Shape Up teams — cycle + pitches + tasks + blockers + hill chart + retros in one call.

### What this enables that no competitor offers

| Workflow | With ShipFlow MCP | Without MCP (every other tool) |
|----------|------------------|--------------------------------|
| Full context for a cycle or pitch | `get_work_context(pitchId)` returns the entire graph | Open 4–5 browser tabs |
| Know your tasks while coding | Ask Claude Code in the terminal | Open browser tab, navigate to board |
| Find blockers before writing code | `get_blockers(cycleId)` returns live list | Manual check in the tool |
| Understand pitch scope before implementing | `get_pitch_detail` returns full Shape Up fields + Figma URL | Copy-paste from browser |
| Update task status after a commit | `update_task_status` from the editor | Manual click in the UI |
| Design context while coding | Pitch → Figma MCP chain, automatic | Open Figma manually |
| Run AI architecture analysis from editor | `wise_architecture_analyze` returns Markdown guides | Open ShipFlow UI |

### Why competitors haven't matched this yet

MCP is an emerging standard (2024). Most PM tools are building AI features **inside** their UI (AI-assisted summaries, auto-prioritization). Atlassian's Teamwork Graph is the first serious competitor move into editor-native context — but it requires Jira + Confluence + Bitbucket. ShipFlow's bet is that the most valuable AI surface is **the developer's editor**, and the full relationship graph should be a single tool call, not a product suite.

---

## 5. Pricing Comparison

| Tool | Self-hosted | OSS | Free tier | Typical team cost |
|------|:-----------:|:---:|:---------:|-------------------|
| ShipFlow | ✅ | ✅ | ✅ (unlimited) | $0 (self-hosted) |
| Linear | ❌ | ❌ | ✅ (limited) | ~$8/user/month |
| Jira | ✅ (Data Center) | ❌ | ✅ (10 users) | ~$8–$16/user/month |
| Asana | ❌ | ❌ | ✅ (limited) | ~$11–$25/user/month |
| Monday.com | ❌ | ❌ | ❌ | ~$9–$19/user/month |
| Basecamp | ❌ | ❌ | ❌ | $15/user or $299/month flat |
| Shortcut | ❌ | ❌ | ✅ (10 users) | ~$8.50/user/month |

ShipFlow's self-hosted model gives unlimited users at infrastructure cost only.

---

## 6. When ShipFlow Is Not the Right Choice

Be honest with evaluators:

- **You don't practice Shape Up** — ShipFlow is optimised for it. A general Kanban team may be happier with Linear or Shortcut.
- **You need enterprise SSO/SAML today** — not yet implemented (planned).
- **You need a mobile app** — ShipFlow is web-first; mobile is responsive but not a native app.
- **You need thousands of plugins** — Jira's plugin ecosystem is unmatched.
- **You need non-technical stakeholder tools** (timesheets, resource planning, Gantt) — Asana/Monday win here.

---

## 7. Summary

ShipFlow is the right choice when:

1. Your team practices or wants to practice **Shape Up**
2. You want **AI features that developers actually use** — in the editor, not buried in a settings page
3. You need **self-hosted / open source** with full data ownership and no per-seat cost
4. You value **pluggability** — swap LLM providers, vector stores, VCS providers, and notification channels without code changes

> See the full feature list in [README.md](README.md) and the release history in [CHANGELOG.md](CHANGELOG.md).
