# ShipFlow — Competitive Analysis

_Last updated: 2026-06-16 (v1.7.0 shipped — Workflow Automations: 14 triggers, 7 actions, 20 built-in templates)_

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
| **Scrum mode (sprints, story points, burndown, velocity)** | ✅ (v1.1.0) | ✅ | ✅ | Partial | Partial | ❌ | ✅ |
| **Triple mode (Shape Up + Kanban + Scrum per project)** | ✅ (v1.1.0) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
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
| **AI pitch writer (Shape Up draft from one sentence)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **AI retrospective summarizer (patterns, blockers, team health)** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Proactive dashboard insights (overdue, at-risk, scope creep, velocity)** | ✅ | Partial³ | ❌ | ❌ | ❌ | ❌ | ❌ |
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
| **CSV import from Jira / Linear / Asana** | ✅ (v1.2.0) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Direct Linear API import (OAuth2)** | ✅ (v1.2.0) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Direct Jira Cloud API import (OAuth2)** | ✅ (v1.2.0) | ❌ | ❌ | N/A | ❌ | ❌ | ❌ |
| **No-code workflow automations (trigger/action engine)** | ✅ (v1.7.0) | ✅ | ✅ (Automation for Jira) | ✅ | ✅ | ❌ | Partial |
| **Shape Up–aware automation triggers (hill chart, appetite, betting table)** | ✅ (v1.7.0) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Automation execution log (per-run status, payload, result)** | ✅ (v1.7.0) | Partial | Partial | ❌ | Partial | ❌ | ❌ |
| **Unified Knowledge Center wired into AI features** | ✅ (v1.8.0) | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

¹ Basecamp invented Shape Up but does not implement it as a structured workflow in its own app.
² Shortcut has cycles and stories but no pitch/betting/hill-chart workflow.
³ Linear has a "Triage" view for overdue issues but no proactive AI-computed insight panel with scope-creep detection or velocity trend analysis.

---

## 3. Deep-Dive Comparisons

### 3.1 ShipFlow vs Linear

**Linear wins**: fastest UI, excellent keyboard navigation, strong engineering culture brand, good GitHub integration, polished mobile app.

**ShipFlow wins**:
- The only tool with a complete Shape Up workflow (pitches, betting, hill charts, circuit breaker)
- **Workflow Automations with Shape Up–aware triggers** — Linear's automation triggers are generic; ShipFlow uniquely fires on hill chart movement, appetite exceeded, scope creep detected, and betting table locked
- MCP server — Linear has no way to expose project data to AI editors as structured tool calls
- Self-hosted / open source — Linear is SaaS-only
- Pluggable AI stack — Ollama for local/private deployments, OpenAI for production
- RTL support (Persian, Arabic)
- No per-seat cost when self-hosted

**Who should use Linear**: Teams that want the fastest Kanban tool on the market and don't need self-hosting or AI-editor integration. ShipFlow v1.1.0 now matches Linear's Scrum feature set — sprints, story points, burndown, and velocity — while also supporting Shape Up and self-hosting.
**Who should use ShipFlow**: Teams that practice Shape Up, want AI-native workflows, or need self-hosted data ownership.

---

### 3.2 ShipFlow vs Jira

**Jira wins**: market leader, deep integrations ecosystem (thousands of plugins), enterprise SSO/SAML (ShipFlow adds SSO admin UI + OIDC/SAML2 in v1.4.0), advanced dashboards, Confluence integration.

**ShipFlow wins**:
- Zero configuration overhead — Shape Up workflow is first-class, not a custom template
- **Workflow Automations built-in** — "Automation for Jira" is a paid plugin sold separately; ShipFlow ships 20 automation templates out of the box with no plugin purchase
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

**They win on**: richer reporting dashboards, enterprise tier support, non-technical stakeholder adoption. **Note**: ShipFlow v1.7.0 now ships its own no-code workflow automation engine (14 triggers, 7 actions, 20 templates), closing the automation gap that previously favoured Asana/Monday.

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

## 4b. The Knowledge Center Differentiator

ShipFlow's Knowledge Center gives teams a single place to upload docs and paste URLs that the AI uses everywhere — Q&A, AI test generation, Wise Architecture, and risk analysis all draw from the same indexed corpus. Sources are scoped at the Org, Team, or Project level, and a pluggable provider SPI ships file-upload + URL today with GitHub / Confluence / Notion / Drive integrations queued as follow-ups.

| Capability | ShipFlow | Linear | Jira + Confluence | Asana | Monday.com |
|------------|:--------:|:------:|:-----------------:|:-----:|:----------:|
| Upload docs that feed AI features | ✅ | ❌ | Partial¹ | ❌ | ❌ |
| Paste URLs as live AI knowledge sources | ✅ | ❌ | ❌ | ❌ | ❌ |
| Org / Team / Project scope for every source | ✅ | ❌ | ❌ | ❌ | ❌ |
| Single corpus reused across Q&A, test gen, Wise Architecture, risk | ✅ | ❌ | ❌ | ❌ | ❌ |
| Pluggable provider SPI (open source, extend in-tree) | ✅ | ❌ | ❌ | ❌ | ❌ |
| Citation chips link AI answers back to the source | ✅ | ❌ | ❌ | ❌ | ❌ |

¹ Confluence indexes pages for Atlassian Intelligence search, but the corpus is not exposed as a unified knowledge layer to other AI features (Jira AI, Compass, Rovo each maintain separate indexes).

**Why this matters**: every competitor's AI is locked to whatever data already lives inside their product. ShipFlow lets teams point the AI at their actual source-of-truth docs (PRDs in Notion, architecture in Confluence, runbooks in Drive) and reuse that knowledge across every AI surface.

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
- **You need a fully operational SSO IdP integration** — the admin UI and login flow are built (v1.4.0); backend SAML2/OIDC Spring Security integration is in progress (S32). SCIM 2.0 auto-provisioning is now live (v1.4.0).
- **You need a mobile app** — ShipFlow is web-first; mobile is responsive but not a native app.
- **You need thousands of plugins** — Jira's plugin ecosystem is unmatched.
- **You need non-technical stakeholder tools** (timesheets, resource planning) — Asana/Monday win here. ShipFlow now has interactive Gantt-style timeline bars for initiatives and epics, but not full resource planning.

---

## 7. Summary

ShipFlow is the right choice when:

1. Your team practices or wants to practice **Shape Up**
2. You want **AI features that developers actually use** — in the editor, not buried in a settings page
3. You need **self-hosted / open source** with full data ownership and no per-seat cost
4. You value **pluggability** — swap LLM providers, vector stores, VCS providers, and notification channels without code changes

> See the full feature list in [README.md](README.md) and the release history in [CHANGELOG.md](CHANGELOG.md).
