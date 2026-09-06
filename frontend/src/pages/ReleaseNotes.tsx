import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Sparkles,
  TrendingUp,
  Users,
  BarChart3,
  Target,
  Brain,
  Calendar,
  Shield,
  Layout,
  CheckCircle,
  Clock,
  RefreshCw,
  GitBranch,
  Rocket,
  Bug,
  Settings,
  FileText,
  Bell,
  MessageSquare,
  Github,
  Activity,
  Layers,
  Key,
  Webhook,
  Search,
  Command,
  ArrowDownToLine,
  Plug,
  Cpu,
  Lock,
  Paperclip,
  ListChecks,
  Download,
  Compass,
  Wrench,
  Bookmark,
  Mail,
  FlaskConical,
  Rss,
  Container,
  Network,
  Pencil,
  GripHorizontal,
  ClipboardList,
  Keyboard,
  Workflow,
  TrendingDown,
  Gauge,
  Upload,
  FileSpreadsheet,
  FolderInput,
  BookOpen,
  Link2,
  Building2,
  Wand2,
  Zap,
  Sliders,
  Smartphone,

  WifiOff,
  Fingerprint,
  Kanban,
  Filter,
  Puzzle,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { useSeo, breadcrumbSchema } from '@/hooks/useSeo';

interface Release {
  version: string;
  date: string;
  title: string;
  upcoming?: boolean;
  highlights: {
    icon: React.ReactNode;
    title: string;
    description: string;
  }[];
}

const releases: Release[] = [
  {
    version: '1.13.0',
    date: 'Coming soon',
    title: 'Live Presence & Truth',
    upcoming: true,
    highlights: [
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Live Presence Indicators',
        description:
          "See who else is viewing or editing a pitch, retrospective, or wiki page in real time, built on a reworked notification stream that fans out across every server instance instead of just one.",
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Conflict-Safe Editing',
        description:
          "Optimistic locking with a friendly conflict dialog on pitch, retrospective, and wiki saves — no more silently overwriting a teammate's changes.",
      },
      {
        icon: <Zap className="h-5 w-5" />,
        title: 'Real-Time Infrastructure Rework',
        description:
          "The notification stream now supports multiple tabs per user and broadcasts across replicas via Redis — fixing a scaling gap in the multi-instance Helm deployment.",
      },
    ],
  },
  {
    version: '1.12.2',
    date: 'September 2, 2026',
    title: 'Accurate Hill Charts & Broader Attachment Support',
    highlights: [
      {
        icon: <TrendingUp className="h-5 w-5" />,
        title: 'Hill Chart Progress You Can Trust',
        description:
          "A scope's position on the hill chart could get stuck reflecting only the very first task ever created for it, and a single completed subtask could wrongly push the whole scope to \"Done.\" Progress is now computed from every task linked to a scope — not just one task's original subtree — and a task's own status always counts alongside its subtasks instead of being dropped.",
      },
      {
        icon: <Paperclip className="h-5 w-5" />,
        title: 'JSON and ZIP Attachments',
        description:
          'Task attachments now accept JSON and ZIP files, alongside the existing images and documents. Same 10 MB limit applies.',
      },
    ],
  },
  {
    version: '1.12.1',
    date: 'August 28, 2026',
    title: 'Self-Hosting Fixes: Clean Installs & Current Claude Models',
    highlights: [
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Clean Production Installs Stay Clean',
        description:
          'A fresh production install no longer arrives with a "Mobile App — Scrum Demo" project you never asked for. The demo seeder is now opt-in, and it can no longer take the application down: archiving or deleting that project used to make the next restart fail to start.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Support for the Latest Claude Models',
        description:
          'Selecting a current Claude model such as claude-sonnet-5 made every AI feature return a 500, because ShipFlow always sent a temperature parameter that the newest models reject. Anthropic now omits it by default, so the latest models work out of the box.',
      },
    ],
  },
  {
    version: '1.12.0',
    date: 'August 20, 2026',
    title: 'Plugin Marketplace: SDK Tooling & Source Control Integrations',
    highlights: [
      {
        icon: <Puzzle className="h-5 w-5" />,
        title: 'Real Plugin SDK & Scaffolding CLI',
        description:
          "The Plugin SDK's shipflow-plugin-api is now a real, distributable Maven module instead of interfaces living inside the main backend, and a new shipflow-plugin-archetype lets you scaffold a plugin project with one mvn archetype:generate command instead of copying files by hand. Plugins still compile into the ShipFlow build as Spring beans — this closes the SDK tooling gap, not a move to dynamic runtime loading.",
      },
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'GitLab Code Context for AI Features',
        description:
          "AI features like Wise Architecture can now pull repository code context from GitLab — gitlab.com or a self-hosted instance — alongside the existing GitHub, Figma, Notion, and Confluence integrations. Authenticate with a Personal Access Token in Organization Settings and point it at your instance.",
      },
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'Azure DevOps Code Context for AI Features',
        description:
          "The same code-context integration is now available for Azure Repos, on both Azure DevOps Services (dev.azure.com) and a self-hosted Azure DevOps Server — rounding out ShipFlow's source-control coverage across GitHub, GitLab, and Azure DevOps.",
      },
    ],
  },
  {
    version: '1.11.2',
    date: 'August 5, 2026',
    title: 'Flexible Task Cycles & Pitch Kanban',
    highlights: [
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'No More Forced Cycle at Task Creation',
        description:
          "Logging a task no longer requires picking a cycle up front. A task linked to a pitch now automatically follows whatever cycle that pitch is currently bet on — including retroactively, if you bet or re-bet the pitch later — while a new audit trail tracks which cycle a task was in at every point, for future reporting.",
      },
      {
        icon: <Kanban className="h-5 w-5" />,
        title: 'Pitch Detail Now Shows a Kanban Board',
        description:
          "A pitch's tasks render as a drag-and-drop Kanban board right on its detail page instead of a flat list — drop a card into a new column to update its status immediately, subtasks group under their parent, and you can create tasks even for a pitch that hasn't been bet to a cycle yet.",
      },
      {
        icon: <Filter className="h-5 w-5" />,
        title: 'Backlog Category Filter Replaces Tabs',
        description:
          "The Pitch Tasks / Debt & Improvements tab switcher is gone — the Backlog page now shows one merged list, with the same distinction available as a filter alongside Status and Priority. Also fixes a pagination bug where switching tabs, cycle, or the dependency filter could strand you on an empty page.",
      },
    ],
  },
  {
    version: '1.11.1',
    date: 'July 30, 2026',
    title: 'Passkey, Install & Backlog Nudges',
    highlights: [
      {
        icon: <Fingerprint className="h-5 w-5" />,
        title: 'Passkey Setup Prompt After Login',
        description:
          "Sign in with a password and have no passkey registered yet? You'll now see a one-time, dismissible prompt offering to set one up right there, instead of needing to know it's tucked away in Profile settings.",
      },
      {
        icon: <Download className="h-5 w-5" />,
        title: 'Install Prompt on Mobile Login',
        description:
          "On mobile, right after login, ShipFlow now offers to install itself to your home screen instead of relying on the browser's own install affordance, which is easy to miss. Chromium-based mobile browsers only — iOS Safari has no equivalent event to hook into.",
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Sub-Task Grouping for Kanban Backlogs',
        description:
          "Kanban projects have no Pitch concept, so Debt/Improvement backlog tasks previously sat flat with no structural grouping at all. Sub-tasks now render directly under their parent in the Backlog list view (regardless of sort order) and group within a shared column on the Kanban board, with a badge/caption keeping the relationship visible even across columns.",
      },
    ],
  },
  {
    version: '1.11.0',
    date: 'July 28, 2026',
    title: 'Mobile PWA',
    highlights: [
      {
        icon: <WifiOff className="h-5 w-5" />,
        title: 'Installable App + Offline Support',
        description:
          "Install ShipFlow like a native app (desktop \"Install\" icon or mobile \"Add to Home Screen\"). Already-visited pages stay browsable when you lose connectivity, and writes made offline — comments, status changes, new tasks — are queued and sent automatically once you're back online, instead of failing silently.",
      },
      {
        icon: <Smartphone className="h-5 w-5" />,
        title: 'Responsive-Layout Audit',
        description:
          "Swept the app for real mobile-usability gaps at 375px, not just cosmetic tweaks. The floating bulk-action toolbar on Backlog, the Wiki page-tree sidebar (now a Sheet drawer below the `lg` breakpoint), five wiki dialogs that had bypassed the shared modal component, a stat grid, and a task-detail action row were all fixed. Along the way, found and fixed a gap in the shared AlertDialog component itself — every confirmation dialog across the app now gets a proper margin on mobile instead of sitting flush against the screen edges.",
      },
      {
        icon: <Bell className="h-5 w-5" />,
        title: 'Web Push Notifications',
        description:
          'Get native push notifications for mentions, assignments, and cycle events — even when ShipFlow isn\'t open in a tab. Turn it on from your Profile page; works on desktop and mobile without any app-store install.',
      },
      {
        icon: <Fingerprint className="h-5 w-5" />,
        title: 'Sign In with a Passkey',
        description:
          'Register a passkey from your Profile page and sign in with Face ID, Touch ID, Windows Hello, or a security key instead of typing a password — built on the WebAuthn standard, no third-party authenticator app required.',
      },
    ],
  },
  {
    version: '1.10.0',
    date: 'July 14, 2026',
    title: 'Wiki References & Task Ordering',
    highlights: [
      {
        icon: <Link2 className="h-5 w-5" />,
        title: 'Linked Wiki Pages on Pitches & Tasks',
        description:
          'Reference existing research or documentation without uploading another copy. Search and link a wiki page directly from a Pitch or Task — see where it\'s linked from, jump to it, or unlink it. Scoped to Pitches and Tasks; Bug Reports keep file attachments as their evidence type.',
      },
      {
        icon: <GripHorizontal className="h-5 w-5" />,
        title: 'Drag-and-Drop Task Reordering in a Pitch',
        description:
          'Manually reorder a pitch\'s task list by dragging the grip handle. Defaults to priority order until you reorder by hand; subtasks reorder within their own parent group only.',
      },
      {
        icon: <Target className="h-5 w-5" />,
        title: 'Target Release for Standalone Tasks',
        description:
          'Debt/improvement tasks that aren\'t tied to a pitch can now be assigned a target release directly, so they show up on the release and roadmap the same way pitches do.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'AI-Recommended Deliverable Tasks',
        description:
          'A "Suggest Tasks with AI" button on Pitch Detail generates deliverable task suggestions grounded in the pitch\'s problem, solution, and appetite — plus Figma design context when your org has a Figma token configured. Each suggestion is tagged with the disciplines needed to deliver it (Design, Backend, Mobile, QA), since most deliverables span more than one. Select the ones you want and create them under the pitch in one go.',
      },
      {
        icon: <ClipboardList className="h-5 w-5" />,
        title: 'Related Test Cases on Pitch Detail',
        description:
          'A "Test Cases" card on Pitch Detail now lists the test cases already linked to that pitch — key, title, type/priority/status, and pass rate — with a link to the full QA test-management page to view, add, or AI-generate more.',
      },
      {
        icon: <Key className="h-5 w-5" />,
        title: 'MCP Connector URL for claude.ai & Free-Tier Clients',
        description:
          'claude.ai\'s connector settings can\'t send a custom Authorization header, so a secondary MCP connection method was added: paste one URL (the API key travels in the path, e.g. /mcp/<api-key>/sse) — no headers needed. This connection is always read-only regardless of the key\'s scope, since a URL-embedded token can be logged by proxies or browser history. The API Keys tab now shows a ready-to-copy connector URL alongside the raw key. The existing header-based Bearer token method is unchanged.',
      },
    ],
  },
  {
    version: '1.9.0',
    date: 'July 1, 2026',
    title: 'Production-Grade Self-Hosting',
    highlights: [
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Helm Chart & Kubernetes',
        description:
          'A first-party Helm chart deploys ShipFlow to any Kubernetes cluster — configurable replicas, HPA autoscaling, ingress, secrets, persistent uploads, and probes wired to Spring Boot health groups.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'Observability — Prometheus, Grafana & Tracing',
        description:
          'A Prometheus scrape endpoint (/actuator/prometheus), a ready-to-import Grafana dashboard, and optional OpenTelemetry distributed tracing over OTLP. Ships a docker-compose monitoring profile and structured JSON logging with traceId/spanId.',
      },
      {
        icon: <ClipboardList className="h-5 w-5" />,
        title: 'Audit Trail Export',
        description:
          'Admins can export the full Envers change history (tasks, pitches, bug reports, test cases) as CSV or JSON, filtered by entity type and date range, from Organization Settings — for compliance and offline review.',
      },
      {
        icon: <Lock className="h-5 w-5" />,
        title: 'Air-Gapped AI Mode',
        description:
          'A hard-guaranteed zero-egress mode for regulated, isolated deployments — a startup validator refuses to boot if any AI provider, MCP client, or model URL would call out beyond the cluster, with an in-app status badge.',
      },
    ],
  },
  {
    version: '1.8.0',
    date: 'June 27, 2026',
    title: 'Custom Fields, Wiki & Object Storage',
    highlights: [
      {
        icon: <BookOpen className="h-5 w-5" />,
        title: 'Upload Docs & Add URLs',
        description:
          'Upload PDFs, DOCX, and Markdown files or paste URLs that ShipFlow indexes and feeds into Q&A, AI test generation, Wise Architecture, and risk analysis — one knowledge layer reused across every AI surface.',
      },
      {
        icon: <Building2 className="h-5 w-5" />,
        title: 'Org / Team / Project Scope',
        description:
          'Every source belongs to an Org, Team, or Project scope so AI answers stay grounded in the right context. Citation chips on AI answers link back to the original source.',
      },
      {
        icon: <Link2 className="h-5 w-5" />,
        title: 'Pluggable Provider SPI',
        description:
          'File upload and URL providers ship today. The provider SPI lets you drop in a new source type (GitHub, Confluence, Notion, Drive) by implementing a single Spring bean — Spring auto-registers it via KnowledgeSourceRegistry.',
      },
      {
        icon: <BookOpen className="h-5 w-5" />,
        title: 'Built-in Wiki',
        description:
          'Hierarchical spaces → pages tree with a Notion-style block editor (headings, lists, checkboxes, tables, code blocks, callouts, slash menu). Drag pages to reorganize.',
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: 'Page comments & discussion',
        description:
          'Every wiki page has a comments thread — the same one used on tasks and bugs — with Markdown, emoji reactions, and @mention autocomplete that notifies and deep-links teammates back to the page.',
      },
      {
        icon: <Clock className="h-5 w-5" />,
        title: 'Versioned docs with history',
        description:
          'Every edit is versioned; compare any past revision against the current page in a side-by-side diff, then restore it. Breadcrumbs, auto table-of-contents, full-text search, @mentions, internal page links, and file attachments.',
      },
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'AI-connected docs',
        description:
          'Wiki pages auto-feed the Knowledge Center, so AI Q&A and Wise Architecture answer from your team\'s docs automatically.',
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Pluggable Object Storage',
        description:
          'Store attachments on AWS S3, MinIO, or local disk, chosen via Org Settings; encrypted-by-config credentials, connection test, and one-click migration between backends.',
      },
      {
        icon: <Sliders className="h-5 w-5" />,
        title: 'Custom Fields on Tasks, Pitches & Bugs',
        description: 'Add your own fields — text, number, date, URL, checkbox, single-select, multi-select — to tasks, pitches, and bug reports. Define fields org-wide or per-project. Values appear on every detail page.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Project-Level Permissions',
        description: 'Grant access at the project level, not just the organization. Give a contractor read access to one project without touching global roles.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Custom Fields Settings',
        description: 'Manage field definitions in Organization Settings → Custom Fields. Separate tabs for Tasks, Pitches, and Bug Reports. Create and edit fields via a guided dialog.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'MCP Usage Report',
        description: 'Admin dashboard showing per-user and per-tool MCP call analytics — 30-day timeline, success rate, failure breakdown, and a recent-log feed. Accessible from Integrations → MCP → View Usage Report.',
      },
    ],
  },
  {
    version: '1.7.0',
    date: 'June 16, 2026',
    title: 'Workflow Automations',
    highlights: [
      {
        icon: <Zap className="h-5 w-5" />,
        title: 'Trigger/Action Engine',
        description: 'Define automation rules that fire on events like task completion, pitch status changes, cycle starts, and Shape Up-specific triggers like appetite exceeded or scope creep detected.',
      },
      {
        icon: <Workflow className="h-5 w-5" />,
        title: '20 Built-in Templates',
        description: 'Start in seconds with 20 curated templates across Tasks, Shape Up, Automation, and Notifications categories. One click adds a fully configured rule you can customize.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'Shape Up-Specific Triggers',
        description: 'Four new triggers designed for Shape Up teams: Betting Table Locked, Hill Chart Moved, Appetite Exceeded, and Scope Creep Detected — automate the moments that matter most.',
      },
      {
        icon: <ClipboardList className="h-5 w-5" />,
        title: 'Execution Log',
        description: 'Every automation run is recorded with status, trigger event data, and result message. Browse per-rule or project-wide history to audit what fired and when.',
      },
      {
        icon: <RefreshCw className="h-5 w-5" />,
        title: 'Live Retro Board',
        description: 'Retro boards now auto-refresh every 5 seconds while open — no more manual reloads to see what teammates added or voted on.',
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: 'Reactions & Discuss Timer',
        description: 'New 👎 disagree reaction alongside existing votes. Per-item countdown timer (3 min default) auto-marks items as discussed. Vote fill bars show relative signal at a glance.',
      },
      {
        icon: <Clock className="h-5 w-5" />,
        title: 'Persistent Discussed State & Ownership',
        description: 'Discussed items are persisted server-side (green badge + strikethrough synced across all participants). Edit/delete now restricted to item authors; admins/managers can manage any item.',
      },
    ],
  },
  {
    version: '1.6.0',
    date: 'June 15, 2026',
    title: 'MCP Ecosystem',
    highlights: [
      {
        icon: <Wrench className="h-5 w-5" />,
        title: 'update_task & update_pitch MCP write tools',
        description: 'AI editors (Claude Code, Cursor) can now update task and pitch fields in-place with PATCH semantics — only the fields you supply change. Supply a solution and wireframeLinks to a pitch, or update a task\'s title, priority, and due date, without touching any other field.',
      },
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'Plugin SDK',
        description: 'Maven archetype, plugin registry, and first-party plugin scaffold — build and distribute ShipFlow extensions without forking the core.',
      },
      {
        icon: <BookOpen className="h-5 w-5" />,
        title: 'Notion & Confluence MCP Clients',
        description: 'Connect ShipFlow to your Notion workspace or Confluence Cloud space. AI features can now pull design docs and meeting notes directly into context — configure tokens in MCP Integration settings.',
      },
      {
        icon: <Network className="h-5 w-5" />,
        title: 'Rich Link Previews',
        description: 'Shared task, pitch, and cycle URLs now render meaningful og:title / og:description in Slack, iMessage, and any chat that unfurls links — without exposing protected data.',
      },
    ],
  },
  {
    version: '1.5.0',
    date: 'June 7, 2026',
    title: 'AI Copilot v2',
    highlights: [
      {
        icon: <Wand2 className="h-5 w-5" />,
        title: 'AI Pitch Writer',
        description: 'Type a problem in plain language and get a full Shape Up pitch draft — title, problem statement, solution, appetite, rabbit holes, and no-gos — in one click. Pre-fills the pitch form for human review.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'Retrospective Summarizer',
        description: 'AI-generated retro summary after each cycle close. Highlights patterns, recurring blockers, and team health signals across all board entries.',
      },
      {
        icon: <TrendingUp className="h-5 w-5" />,
        title: 'Proactive Dashboard Insights',
        description: 'The dashboard surfaces AI-driven alerts: overdue pitches, at-risk cycles, scope creep warnings, and velocity trend sparklines — computed in the background and cached.',
      },
    ],
  },
  {
    version: '1.4.0',
    date: 'June 7, 2026',
    title: 'Enterprise Auth & UX Depth',
    highlights: [
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'SSO / SAML 2.0 & OIDC',
        description: 'Sign in via Okta, Keycloak, Auth0, or Azure AD. SAML 2.0 and OIDC both supported. Just-in-time user provisioning and SSO enforcement mode for enterprise instances.',
      },
      {
        icon: <Users className="h-5 w-5" />,
        title: 'SCIM 2.0 User Provisioning',
        description: 'Automatically create and deactivate users when they join or leave your identity provider group — no manual user management.',
      },
      {
        icon: <GripHorizontal className="h-5 w-5" />,
        title: 'Interactive Roadmap Timeline',
        description: 'Drag to move or resize epic and initiative bars on the roadmap. Progress indicators shown on bars. One-click "Set dates" for undated items.',
      },
      {
        icon: <Pencil className="h-5 w-5" />,
        title: 'Inline Pitch Title Editing',
        description: 'Edit pitch names directly from the detail header or the epic\'s pitch list. Click to edit, Enter or blur to save, Escape to cancel.',
      },
      {
        icon: <ClipboardList className="h-5 w-5" />,
        title: 'Retrospective Templates',
        description: 'Structured retrospective format with Went Well, Improve, and Action Items columns. Rich-text entries, required before cycle close.',
      },
      {
        icon: <Keyboard className="h-5 w-5" />,
        title: 'Keyboard Shortcut Cheat Sheet',
        description: 'Press ? anywhere to see all keyboard shortcuts. Navigation, quick actions, and search — all documented in a single overlay.',
      },
    ],
  },
  {
    version: '1.3.0',
    date: 'June 5, 2026',
    title: 'MCP Server Admin & API Keys',
    highlights: [
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'MCP Server Runtime Toggle',
        description:
          'Enable or disable the built-in MCP server from Integrations → MCP — a DB-backed toggle that overrides the environment default and takes effect immediately, no restart required.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'MCP Write-Tools Toggle',
        description:
          'Separately allow connected AI tools to create and update data, with write tools disabled whenever the server is off.',
      },
      {
        icon: <Key className="h-5 w-5" />,
        title: 'API Key Management UI',
        description:
          'Create, list, and revoke API keys with READ / WRITE / ADMIN scopes and optional expiry. The raw key is shown once with a copy button and a copy-it-now warning.',
      },
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Admin API Key Oversight',
        description:
          'Admins see all API keys across the organization — who created each one and when — and can revoke any key directly from the MCP Integration → API Keys tab.',
      },
    ],
  },
  {
    version: '1.2.1',
    date: 'June 3, 2026',
    title: 'MCP Expansion, Bug Fixes & QA Improvements',
    highlights: [
      {
        icon: <Network className="h-5 w-5" />,
        title: 'MCP Context Aggregator & 12 New Tools',
        description:
          'New get_task_context tool returns everything a coding agent needs in one call — task, pitch (Shape Up fields + Figma URL), cycle, siblings, and hints. Plus: whoami, get_test_cases/runs, record_test_run, get/update_bug_reports, update_task_assignee, and get_tasks filters (assigneeId, pitchId, mine).',
      },
      {
        icon: <Wrench className="h-5 w-5" />,
        title: 'Date Range Filter on Work Logs',
        description:
          'From / To date pickers now appear on both the My Logs and Team Logs tabs. The filter runs server-side so it works correctly across paginated pages.',
      },
      {
        icon: <Bug className="h-5 w-5" />,
        title: 'Pitch Notes & Wireframe Links Fixed',
        description:
          'Creating a note on a pitch in IDEA/DRAFT/SHAPED status no longer crashes with a 500 (null cycle NPE). Saving wireframe links or Shape Up fields on a PENDING/ACTIVE pitch no longer gets blocked by a spurious appetite validation 400.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'MEMBER Access Fixed for Bug & Test-Case Writes',
        description:
          'QATestManagementController PreAuthorize annotations referenced roles that no longer exist — MEMBER users received 403 on all bug and test-case create/update. Updated to the current MEMBER/MANAGER/ADMIN role model.',
      },
    ],
  },
  {
    version: '1.2.0',
    date: 'May 24, 2026',
    title: 'Competitor Migration Tooling — Import from Jira, Linear & Asana',
    highlights: [
      {
        icon: <Upload className="h-5 w-5" />,
        title: 'CSV Import — Jira, Linear, Asana & Generic',
        description:
          'Upload a CSV export from Jira, Linear, or Asana and ShipFlow auto-detects the format from column headers. Tasks, epics, and sprints are mapped into a new Kanban project. A 3-step stepper guides you through upload → processing → results.',
      },
      {
        icon: <FileSpreadsheet className="h-5 w-5" />,
        title: 'Per-Row Error Log',
        description:
          'Failed rows are captured individually with their error reason — the rest of the import still completes. A detailed error log is shown on the results screen so you can fix and re-import only the broken rows.',
      },
      {
        icon: <FolderInput className="h-5 w-5" />,
        title: 'Import History',
        description:
          'View all past imports with file name, source format, row counts (imported / failed), and status. REST API: POST /api/import/csv, GET /api/import, GET /api/import/{id}.',
      },
      {
        icon: <Workflow className="h-5 w-5" />,
        title: 'Always Imports into Kanban',
        description:
          'Imported projects start as Kanban so teams can onboard immediately. Switch to Shape Up or Scrum at your own pace — no forced methodology change on day one.',
      },
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'Linear API Import — OAuth2 + GraphQL',
        description:
          'Connect ShipFlow to Linear via OAuth2 and import issues, cycles, and projects directly — no CSV export required. Linear Cycles → ShipFlow Cycles, Projects → Epics, Issues → Tasks with priority and state preserved. Target project type is Kanban or Scrum.',
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Jira API Import — Atlassian OAuth 2.0 + REST',
        description:
          'Connect ShipFlow to Jira Cloud via Atlassian OAuth 2.0 (3-legged) and import issues, sprints, and epics directly. Jira Epics → ShipFlow Epics, Sprints → Cycles, Issues → Tasks. Atlassian Document Format descriptions extracted to plain text. Cloud workspace auto-detected.',
      },
    ],
  },
  {
    version: '1.1.0',
    date: 'May 20, 2026',
    title: 'Scrum Mode — Sprints, Story Points & Velocity',
    highlights: [
      {
        icon: <Workflow className="h-5 w-5" />,
        title: 'Scrum as Third Project Type',
        description:
          'Alongside Shape Up and Kanban, projects can now adopt Scrum. Cycles become Sprints, complete with sprint goals, story-point estimates, and time-boxed planning.',
      },
      {
        icon: <Target className="h-5 w-5" />,
        title: 'Story Points on Tasks',
        description:
          'Estimate tasks in story points and see them on backlog rows, Kanban cards, and sprint planning lists. Totals automatically roll up per backlog and per sprint.',
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Sprint Planning Page',
        description:
          'A dedicated two-column workspace that pulls the product backlog on the left and the active sprint on the right. Move work between columns with one click; story-point totals update live.',
      },
      {
        icon: <TrendingDown className="h-5 w-5" />,
        title: 'Burndown Chart',
        description:
          'Real-time sprint burndown comparing remaining story points against the ideal trajectory. Spot scope creep or progress lulls the moment they happen.',
      },
      {
        icon: <Gauge className="h-5 w-5" />,
        title: 'Velocity Chart',
        description:
          'Track planned vs completed story points across the last several sprints. Use the historical trend to commit better in your next sprint planning meeting.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'Sprint Goal on Cycles',
        description:
          'Capture the goal statement for each sprint when creating or editing a cycle. The goal is surfaced on the sprint planning header and on the cycle list cards.',
      },
    ],
  },
  {
    version: '1.0.0',
    date: 'April 21, 2026',
    title: 'First Open Source Release',
    highlights: [
      {
        icon: <Network className="h-5 w-5" />,
        title: 'MCP Relationship Graph: get_work_context',
        description:
          'New read tool that returns the full context for a pitch or cycle in one call — cycle metadata, pitches, tasks with status breakdown, blockers, hill-chart scope positions (0–100), and retrospective summaries. Replaces chaining 4–5 separate MCP tool calls.',
      },
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'Anthropic Claude Provider',
        description:
          'Set AI_PROVIDER=anthropic and ANTHROPIC_API_KEY to use Claude models alongside OpenAI and Ollama. Default claude-3-5-haiku for cost efficiency; claude-3-5-sonnet recommended for Wise Architecture.',
      },
      {
        icon: <Rss className="h-5 w-5" />,
        title: 'Blog System at /blog',
        description:
          'Static blog powered by Markdown files. Responsive 2-column card grid, full typography rendering via @tailwindcss/typography, and blog nav link on the landing page. Launch posts: What is Shape Up, Hill Charts, ShipFlow vs Linear, Shape Up vs Scrum.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Production Fixes: JWT, CORS & SSE',
        description:
          'JWT secret now configurable via JWT_SECRET env var. CORS_ALLOWED_ORIGINS properly threaded through docker-compose. Spring Security 6 SSE async-dispatch AccessDeniedException fixed.',
      },
      {
        icon: <Container className="h-5 w-5" />,
        title: 'Docker Image on GHCR',
        description:
          'ghcr.io/farzad-sedaghatbin/shipflow:1.0.0 and :latest published. One-command self-hosted setup via Docker Compose verified on Linux, macOS, and WSL2.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Live Demo at shipflow.dev',
        description:
          'Public demo with pre-loaded seed data. No sign-up required to explore Shape Up and Kanban modes.',
      },
    ],
  },
  {
    version: '1.0.0-rc1',
    date: 'April 14, 2026',
    title: 'Stabilization — Docs, Community & Production Fixes',
    highlights: [
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'VitePress Documentation Site',
        description:
          'Dedicated docs site at farzad-sedaghatbin.github.io/ShipFlow/ with Getting Started, User Guide, Admin Guide, and Developer Guide. 20 content pages covering all integrations and guides.',
      },
      {
        icon: <Github className="h-5 w-5" />,
        title: 'GOVERNANCE.md + Community Templates',
        description:
          'Governance document covering maintainer expectations, contribution process, and security disclosure. GitHub Discussion templates for community support and feature requests.',
      },
      {
        icon: <FlaskConical className="h-5 w-5" />,
        title: 'Full E2E Suite on Production Image',
        description:
          'All 32 Playwright tests passing against the production Docker image on CI. Auth, project management, pitch lifecycle, hill chart drag-and-persist, and task flows covered.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Bug Bash & Production Fixes',
        description:
          'SSE async-dispatch security fix, rate-limiter bucket for async polling, Anthropic model IDs corrected for 2025+ API keys, and Playwright selector stabilization for CI.',
      },
    ],
  },
  {
    version: '0.9.0',
    date: 'April 14, 2026',
    title: 'Power User Features + Polish',
    highlights: [
      {
        icon: <Wrench className="h-5 w-5" />,
        title: 'MCP Phase 2: Bidirectional AI Editor Integration',
        description:
          'AI editors (Claude Code, Cursor) can now create tasks, create and advance pitches, and add comments via the MCP server. Five new write tools: create_task, create_pitch, update_pitch_status, add_comment, plus the existing update_task_status.',
      },
      {
        icon: <Bell className="h-5 w-5" />,
        title: 'Real-Time SSE Notifications',
        description:
          'Replaced 30-second polling with instant Server-Sent Events push. Notifications appear the moment they are created — no more waiting up to 30 seconds.',
      },
      {
        icon: <Bookmark className="h-5 w-5" />,
        title: 'Saved Filter Views',
        description:
          'Save, load, and manage named filter presets in the task backlog. One click to return to your most-used filter combination.',
      },
      {
        icon: <Mail className="h-5 w-5" />,
        title: 'Email Notifications',
        description:
          'Optional SMTP integration for email delivery of key events: task assigned, @mention in comment, pitch status change. Configure via Organization Settings → Email.',
      },
      {
        icon: <FlaskConical className="h-5 w-5" />,
        title: 'Playwright E2E Test Suite',
        description:
          'Full end-to-end coverage across five core flows: authentication, project management, pitch lifecycle, hill chart drag-and-persist, and task management with Cmd+K search. Wired into CI with Postgres + Redis services.',
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Component Decomposition',
        description:
          'BacklogPage (2320 → 170 lines), OrganizationSettings (1740 → 271 lines), and PitchDetail (1615 → 609 lines) each decomposed into focused sub-components, making the codebase significantly easier to navigate and extend.',
      },
    ],
  },
  {
    version: '0.8.0',
    date: 'April 5, 2026',
    title: 'Core Product + Hardening',
    highlights: [
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'AI Q&A Multi-Turn Memory & Entity Disambiguation',
        description:
          '"Cycle 5" now resolves to the cycle named Cycle 5, not the one with id=5. Conversation context persists across page navigation, evolves as you switch topics, and cache is bypassed for multi-turn sessions so every answer reflects the full conversation history.',
      },
      {
        icon: <Compass className="h-5 w-5" />,
        title: 'Interactive Onboarding Tour',
        description:
          '21-step guided tour powered by driver.js walks first-time users through every key feature — projects, cycles, pitches, betting table, hill charts and more. Appears automatically on first login; restartable any time from the sidebar.',
      },
      {
        icon: <Rocket className="h-5 w-5" />,
        title: 'Public Roadmap Page',
        description:
          'A new /roadmap page shows what\'s shipping next, what\'s been shipped, and the long-term vision. Built in public, updated every commit.',
      },
      {
        icon: <Paperclip className="h-5 w-5" />,
        title: 'File Attachments on Tasks',
        description:
          'Drag-and-drop file uploads directly on tasks — screenshots, specs, designs. The feature every PM tool has, now in ShipFlow.',
      },
      {
        icon: <ListChecks className="h-5 w-5" />,
        title: 'Bulk Task Operations',
        description:
          'Multi-select tasks and bulk assign, change status, change priority, or add tags in one action.',
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: '@Mention Notifications',
        description:
          'Type @Name in any comment to instantly notify that person. Mentions appear in the notification bell with a distinct icon so they stand out from other alerts.',
      },
      {
        icon: <Download className="h-5 w-5" />,
        title: 'CSV Export for Task Backlog',
        description:
          'One-click CSV download of your current task list with all active filters applied. Includes title, status, priority, assignee, pitch, cycle, estimates, tags, and timestamps.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Security Hardening',
        description:
          'Bucket4j rate limiting on auth/search/AI endpoints, CSP headers, startup secret validation, and Spring Boot upgrade to 3.4.x.',
      },
    ],
  },
  {
    version: '0.7.0',
    date: 'March 24, 2026',
    title: 'MCP Server — Your AI Editor Meets Your Project Board',
    highlights: [
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'ShipFlow as an MCP Server (opt-in)',
        description:
          'Claude Code, Cursor, Claude Desktop, and any MCP-compatible AI assistant can now query ShipFlow directly from the editor — no tab switching. Disabled by default; enable with MCP_SERVER_ENABLED=true so self-hosters stay in control.',
      },
      {
        icon: <Cpu className="h-5 w-5" />,
        title: '10 Read Tools + 1 Write Tool',
        description:
          'list_projects, get_project, get_cycles, get_cycle, get_tasks, get_task, get_blockers, get_pitches, get_pitch_detail, get_betting_candidates — and update_task_status (write, opt-in). Ask your AI "what\'s blocking me?" and get a live answer.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Pitch → Figma → Code Loop',
        description:
          'get_pitch_detail returns wireframe (Figma) URLs stored in the pitch. When Figma MCP is also configured, the AI chains the calls automatically — reading design context without you copying a URL.',
      },
      {
        icon: <Lock className="h-5 w-5" />,
        title: 'API Key Auth on All MCP Endpoints',
        description:
          'Bearer token authentication on /mcp/** reuses existing API key scopes (READ / WRITE / ADMIN). Write tools require both MCP_SERVER_WRITE_ENABLED=true and a WRITE-scoped key.',
      },
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'HTTP + SSE Transport (JSON-RPC 2.0)',
        description:
          'Standard MCP transport: GET /mcp/sse establishes the session stream, POST /mcp/messages sends tool calls. GET /mcp/health is public for readiness probes. Zero additional dependencies — pure Spring Boot.',
      },
    ],
  },
  {
    version: '0.6.2',
    date: 'February 26, 2026',
    title: 'Multi-Layer Caching & Performance',
    highlights: [
      {
        icon: <RefreshCw className="h-5 w-5" />,
        title: 'HTTP ETag / 304 Caching',
        description: 'ShallowEtagHeaderFilter computes response ETags so browsers and API clients receive 304 Not Modified when resources are unchanged — eliminating redundant payload transfers over slow connections.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Spring Service-Layer Caching with Redis',
        description: '@Cacheable / @CacheEvict on eight domain services (PermissionService, ProjectService, CycleService, TeamService, TagService, PersonService, UserService, RoadmapService) with per-domain TTLs. Redis in production with automatic in-memory fallback for dev/test.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'React Query & Axios ETag Interceptor',
        description: 'Per-domain staleTime constants (Tasks 30 s, Entities 5 min, Reference data 10 min). Axios interceptor stores and replays ETags per endpoint — 304 responses return cached bodies transparently. Dashboard widgets migrated to useQuery / useQueries.',
      },
      {
        icon: <Command className="h-5 w-5" />,
        title: 'Global Search (⌘K)',
        description: 'Instantly search tasks, subtasks, bug reports, pitches, and epics from the top bar using ⌘K / Ctrl+K. Powered by pg_trgm GIN indexes with trigram similarity scoring and exact bug-key matching.',
      },
      {
        icon: <ArrowDownToLine className="h-5 w-5" />,
        title: 'Inbound Webhook Admin UI',
        description: 'Configure inbound webhook providers through Integrations → Inbound Webhooks — no environment variables needed. Per-provider HMAC setup (SHA256/SHA1/SHA512), secret masking, enable/disable toggle, and copyable auto-generated webhook URLs.',
      },
    ],
  },
  {
    version: '0.6.1',
    date: 'February 25, 2026',
    title: 'Markdown Editor, Project Selection & Pitch Prioritization',
    highlights: [
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'Markdown Editor for Descriptions',
        description: 'All description fields now support Markdown with a Write/Preview tab toggle. Forms updated: EpicForm, InitiativeForm, BugReportModal, PitchBoard, TaskDetail. Detail pages render rich Markdown across pitches, epics, initiatives, tasks, and bug reports.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Project Selection Dialog',
        description: 'A modal popup guides users to select a project when navigating to pages that require one (Epics, Initiatives, Releases, Roadmap, Retros). Replaces the blank empty-state card that users often mistook for a broken page.',
      },
      {
        icon: <TrendingUp className="h-5 w-5" />,
        title: 'Pitch Prioritization & Drag-and-Drop Reorder',
        description: 'Drag-and-drop reordering and High / Medium / Low priority labels for pitches inside epics. Color-coded PriorityBadge and release version badge on pitch cards. Sort pitches by priority on PitchBoard.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Expanded Color Palette',
        description: '42 colors organized in 7 hue groups (Reds, Oranges, Greens, Teals, Blues, Purples, Neutrals) replacing the previous palette of 10. Improved UX with flex-wrap layout, hover scale effect, and ring indicator.',
      },
    ],
  },
  {
    version: '0.6.0',
    date: 'February 22, 2026',
    title: 'Provider Abstractions, Release Traceability & Inbound Webhooks',
    highlights: [
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'Pluggable VCS & Notification Providers',
        description: 'New VCSProvider and NotificationProvider interfaces let you swap GitHub for GitLab/Bitbucket or Slack for Discord/PagerDuty without touching core logic. GitHubIntegrationService and SlackIntegrationService now implement these contracts.',
      },
      {
        icon: <Webhook className="h-5 w-5" />,
        title: 'Generic Inbound Webhook Infrastructure',
        description: 'Implement InboundWebhookHandler as a Spring @Component to receive events from any provider. O(1) dispatch, signature validation, auto event-type detection from common headers (X-GitHub-Event, X-GitLab-Event, X-Linear-Event…).',
      },
      {
        icon: <Key className="h-5 w-5" />,
        title: 'Public API with Scoped API Keys',
        description: 'Create READ/WRITE/ADMIN-scoped API keys for CI/CD and external integrations. Update task status via PATCH /api/v1/public/tasks/{id}/status from GitHub Actions. Scope enforcement rejects mutating requests on READ-only keys.',
      },
      {
        icon: <Search className="h-5 w-5" />,
        title: 'AI-Powered Help Search',
        description: 'Ask "how do I…" questions in Help Guides and get guardrailed AI answers. Top-5 vector-store retrieval from 10 embedded knowledge-base files. Suggested questions, follow-up chips, and markdown rendering — fully bilingual (EN/FA).',
      },
      {
        icon: <Target className="h-5 w-5" />,
        title: 'Enhanced Release Traceability',
        description: 'Filter bug reports and the backlog by target release. Assign bugs to a release from the Bug Report modal. Release Detail Cockpit shows task/bug breakdowns by status and severity, plus a slipped-bugs warning section.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Separated Dashboards & Reports',
        description: 'Dashboards now live at /dashboards with their own navigation entry. Reports remain at /reports. Cleaner navigation and no more shared routes.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Security Hardening',
        description: 'Blocked /goform/ router exploits, silenced multipart bot-probe flood errors, and excluded /api/ paths from suspicious-path detection to eliminate false-positive 403s.',
      },
    ],
  },
  {
    version: '0.5.3',
    date: 'February 16, 2026',
    title: 'Wise AI & Strategic Planning',
    highlights: [
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'Wise Architecture - Context-Aware AI',
        description: 'AI solutions integrate team skills, Figma designs via MCP, GitHub code analysis, and roadmap context. Get smarter technical recommendations that consider your team\'s expertise and product strategy.',
      },
      {
        icon: <Target className="h-5 w-5" />,
        title: 'Roadmap & Release Planning',
        description: 'Strategic planning with Initiatives (quarterly themes), Epics (feature groups), and Releases (delivery milestones). Timeline visualization for stakeholder communication.',
      },
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'Scope-Task Auto-Bridge',
        description: 'Tasks and scopes auto-sync bidirectionally. Create a task to get a scope, complete subtasks to auto-update hill chart progress. Toggle manual override when needed.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'Bug Report Attachments',
        description: 'Drag-and-drop images and videos (JPG, PNG, GIF, MP4, MOV). Gallery with thumbnails, full-screen preview, download support, and 50MB file limit.',
      },
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Team Capacity & Budget',
        description: 'Configure hours/day and days/week per person. Capacity hierarchy: Organization → Team → Person → Assignment. Accurate per-person budget tracking.',
      },
      {
        icon: <Calendar className="h-5 w-5" />,
        title: 'Pre-Cycle Pitch States',
        description: 'True Shape Up workflow: IDEA → DRAFT → SHAPED without cycles. Betting table shows shaped pitches ready for cycle assignment during betting.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Dashboard Redesign',
        description: 'Tabbed layout with Overview, AI Insights, and Activity. 6 new manageable widgets. Auto-regeneration of AI narratives on status changes.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'API Contract Alignment',
        description: 'Unified frontend/backend DTOs, duplicate i18n key detection in CI, separate Create/Update DTOs for Cooldown API. Full type safety.',
      },
    ],
  },
  {
    version: '0.5.2',
    date: 'February 10, 2026',
    title: 'Scope-Task Bridge & Capacity Management',
    highlights: [
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'Automatic Scope-Task Linking',
        description: 'Tasks and hill chart scopes are now automatically connected. Creating a task creates a matching scope, and completing tasks automatically updates your hill chart progress.',
      },
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Team Capacity Management',
        description: 'Configure working hours and days per team member. Set defaults at organization level and override for specific teams or individuals.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Dashboard Tabs',
        description: 'New tabbed dashboard layout with Overview, AI Insights, and Activity tabs for easier navigation and less scrolling.',
      },
      {
        icon: <RefreshCw className="h-5 w-5" />,
        title: 'Auto-Regenerate AI Narratives',
        description: 'AI summaries now automatically update when cycles or pitches change status. No more manual refreshing needed.',
      },
    ],
  },
  {
    version: '0.5.1',
    date: 'February 9, 2026',
    title: 'Mobile Optimization & Micro-Interactions',
    highlights: [
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Responsive Design Overhaul',
        description: 'New responsive hooks and utilities for perfect mobile experience. Adaptive layouts that work beautifully on phones, tablets, and desktops.',
      },
      {
        icon: <RefreshCw className="h-5 w-5" />,
        title: 'Smart Loading States',
        description: 'Page-specific skeleton screens replace generic spinners. Smooth transitions prevent content flash during project switching.',
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: 'Flexible Retro Actions',
        description: 'Convert retrospective items to pitches, tasks, or just mark as acted on. Batch processing with detailed notes for better follow-through.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Polished Interactions',
        description: 'Enhanced micro-interactions throughout the app. Visual feedback for every action makes the experience feel more responsive and delightful.',
      },
    ],
  },
  {
    version: '0.5.0',
    date: 'February 8, 2026',
    title: 'Insight, Not Metrics',
    highlights: [
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'Cycle Signals',
        description: 'Replace vanity metrics with actionable insights. Get warned about scope creep, slow starts, and stalled work before they become problems.',
      },
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'AI-Powered Summaries',
        description: 'Automatic narrative summaries for cycles and pitches. Let AI write your status updates and retrospective insights.',
      },
      {
        icon: <Target className="h-5 w-5" />,
        title: 'Pitch Health Scores',
        description: 'At-a-glance health indicators showing scope progress, team utilization, and risk levels for every pitch.',
      },
      {
        icon: <Bell className="h-5 w-5" />,
        title: 'Smart Notifications',
        description: 'Configurable alerts for deadline warnings, status changes, and health threshold breaches.',
      },
    ],
  },
  {
    version: '0.4.1',
    date: 'February 7, 2026',
    title: 'Bug Fixes & Polish',
    highlights: [
      {
        icon: <CheckCircle className="h-5 w-5" />,
        title: 'Meeting List Improvements',
        description: 'Fixed meeting list sorting to show newest meetings first. Meeting type names now display correctly throughout the application.',
      },
      {
        icon: <Bug className="h-5 w-5" />,
        title: 'Test Suite Enhancements',
        description: 'Improved reliability of backend and frontend tests. Better error messages and faster test execution.',
      },
      {
        icon: <Settings className="h-5 w-5" />,
        title: 'UI Refinements',
        description: 'Fixed case-sensitive UUID matching and improved meeting type display consistency across all pages.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Stability Updates',
        description: 'Various bug fixes and performance improvements for a more stable experience.',
      },
    ],
  },
  {
    version: '0.4.0',
    date: 'February 5, 2026',
    title: 'Cycle & Betting Excellence',
    highlights: [
      {
        icon: <Calendar className="h-5 w-5" />,
        title: 'Enhanced Betting Table',
        description: 'Compare pitches side-by-side, see team availability at a glance, and make confident betting decisions.',
      },
      {
        icon: <Clock className="h-5 w-5" />,
        title: 'Cooldown Activities',
        description: 'Track what your team does between cycles: learning, bug fixes, exploration, and technical debt.',
      },
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: 'Retrospective Actions',
        description: 'Convert retrospective items into new pitches or tasks. Merge similar feedback and track what gets acted on.',
      },
      {
        icon: <BarChart3 className="h-5 w-5" />,
        title: 'Custom Dashboards',
        description: 'Build your own dashboards with drag-and-drop widgets. Share views with your team.',
      },
    ],
  },
  {
    version: '0.3.11',
    date: 'February 3, 2026',
    title: 'Kanban Mode & Enhanced Tracking',
    highlights: [
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Kanban Project Support',
        description: 'Choose between Shape Up and Kanban methodologies. Kanban projects work without cycles for continuous flow teams.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'Entity Change History',
        description: 'Full audit trail using Hibernate Envers. See who changed what and when for tasks, bugs, pitches, and test cases.',
      },
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Direct Project Association',
        description: 'Bug reports can now be directly associated with projects, supporting both cycle-based and Kanban workflows.',
      },
      {
        icon: <CheckCircle className="h-5 w-5" />,
        title: 'Soft Delete System',
        description: 'Safe deletion with data recovery. Records are marked as deleted rather than permanently removed.',
      },
    ],
  },
  {
    version: '0.3.10',
    date: 'February 2, 2026',
    title: 'Comments & Reactions',
    highlights: [
      {
        icon: <MessageSquare className="h-5 w-5" />,
        title: 'Commenting System',
        description: 'Full commenting support for tasks and bug reports. Edit, delete, and track comment history with timestamps.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Emoji Reactions',
        description: 'React to comments with 8 emoji options. Toggle reactions and see aggregated counts per emoji.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'RTL Text Detection',
        description: 'Automatic right-to-left direction for Arabic, Farsi, and Hebrew content with proper Unicode support.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Permission-Based Controls',
        description: 'Role-based permissions for commenting. Authors can edit, authors and admins can delete.',
      },
    ],
  },
  {
    version: '0.3.0',
    date: 'January 27, 2026',
    title: 'Quality & Testing',
    highlights: [
      {
        icon: <CheckCircle className="h-5 w-5" />,
        title: 'Test Case Management',
        description: 'Create and organize test cases by pitch. Run test sessions and track pass/fail history over time.',
      },
      {
        icon: <Bug className="h-5 w-5" />,
        title: 'Bug Tracking',
        description: 'Report bugs with screenshots and link them to pitches. Kanban board for tracking fix progress.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'AI Test Generation',
        description: 'Generate test case suggestions from your pitch descriptions using AI. Jump-start your QA process.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'QA Dashboard',
        description: 'See test coverage, recent runs, and bug trends for each cycle at a glance.',
      },
    ],
  },
  {
    version: '0.2.0',
    date: 'January 14, 2026',
    title: 'Team Collaboration',
    highlights: [
      {
        icon: <Users className="h-5 w-5" />,
        title: 'Team Management',
        description: 'Create teams, assign members, and track who\'s working on what. See team workload across pitches.',
      },
      {
        icon: <TrendingUp className="h-5 w-5" />,
        title: 'Hill Charts',
        description: 'Visual progress tracking inspired by Basecamp. See where work is stuck and what\'s moving forward.',
      },
      {
        icon: <Brain className="h-5 w-5" />,
        title: 'AI Risk Advisor',
        description: 'Get AI-powered risk assessments for your pitches. Identify potential blockers before they happen.',
      },
      {
        icon: <Github className="h-5 w-5" />,
        title: 'GitHub Integration',
        description: 'Connect your repositories and see commits, PRs, and issues linked to your pitches.',
      },
    ],
  },
  {
    version: '0.1.0',
    date: 'January 10, 2026',
    title: 'Foundation Release',
    highlights: [
      {
        icon: <Rocket className="h-5 w-5" />,
        title: 'Shape Up Workflow',
        description: 'Full Shape Up methodology support: shaping, betting, building cycles with fixed timeboxes.',
      },
      {
        icon: <Layers className="h-5 w-5" />,
        title: 'Project Types',
        description: 'Choose between Shape Up or Scrum project modes. Flexibility for different team workflows.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Role-Based Permissions',
        description: 'Admin, Manager, and Member roles with granular permission controls.',
      },
      {
        icon: <Settings className="h-5 w-5" />,
        title: 'Organization Settings',
        description: 'Configure your workspace: cycle lengths, appetite options, health thresholds, and more.',
      },
    ],
  },
];

export default function ReleaseNotes() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const latestShippedIndex = releases.findIndex((r) => !r.upcoming);

  useSeo({
    title: 'Release Notes & Changelog',
    description:
      'Every ShipFlow release, newest first — features, fixes and upgrade notes for the open-source Shape Up, Scrum and Kanban project management platform.',
    path: '/releases',
    keywords: ['shipflow changelog', 'shipflow release notes'],
    jsonLd: breadcrumbSchema([
      { name: 'Home', path: '/' },
      { name: 'Releases', path: '/releases' },
    ]),
  });

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
        <div className="container mx-auto px-4 max-w-4xl">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <Button variant="ghost" size="icon" onClick={() => navigate('/')}>
                <ArrowLeft className="h-5 w-5" />
              </Button>
              <div className="flex items-center gap-2">
                <img src="/icon.png" alt="ShipFlow" className="w-8 h-8 rounded-lg" />
                <span className="font-semibold text-lg">ShipFlow</span>
              </div>
            </div>
            <Button variant="outline" onClick={() => navigate('/login')}>
              {t('landing.getStarted')}
            </Button>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="py-12 md:py-16 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-4xl text-center">
          <Badge variant="outline" className="mb-4 text-sm px-4 py-1">
            <Sparkles className="h-3.5 w-3.5 mr-1.5" />
            {t('releaseNotes.latestVersion')}: v{latestShippedIndex >= 0 ? releases[latestShippedIndex].version : releases[0].version}
          </Badge>
          <h1 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
            {t('releaseNotes.title')}
          </h1>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            {t('releaseNotes.subtitle')}
          </p>
        </div>
      </section>

      {/* Releases */}
      <section className="py-12 md:py-16">
        <div className="container mx-auto px-4 max-w-4xl">
          <div className="space-y-12">
            {releases.map((release, index) => (
              <div key={release.version}>
                <Card className={index === latestShippedIndex ? 'border-primary/50 bg-primary/5' : ''}>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div className="flex items-center gap-3">
                        <Badge
                          variant={index === latestShippedIndex ? 'default' : 'secondary'}
                          className="text-sm px-3 py-1"
                        >
                          v{release.version}
                        </Badge>
                        {index === latestShippedIndex && (
                          <Badge variant="outline" className="bg-green-500/10 text-green-600 border-green-500/30">
                            {t('releaseNotes.latest')}
                          </Badge>
                        )}
                        {release.upcoming && (
                          <Badge variant="outline" className="bg-amber-500/10 text-amber-600 border-amber-500/30">
                            {t('releaseNotes.upcoming')}
                          </Badge>
                        )}
                      </div>
                      <span className="text-sm text-muted-foreground">{release.date}</span>
                    </div>
                    <CardTitle className="text-xl mt-2">{release.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid sm:grid-cols-2 gap-4">
                      {release.highlights.map((highlight, hIndex) => (
                        <div
                          key={hIndex}
                          className="flex gap-3 p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                        >
                          <div className="flex-shrink-0 text-primary mt-0.5">
                            {highlight.icon}
                          </div>
                          <div>
                            <h4 className="font-medium text-foreground mb-1">
                              {highlight.title}
                            </h4>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                              {highlight.description}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
                {index < releases.length - 1 && (
                  <div className="flex justify-center my-6">
                    <div className="w-px h-8 bg-border" />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-12 md:py-16 bg-muted/30">
        <div className="container mx-auto px-4 max-w-4xl text-center">
          <h2 className="text-2xl font-bold text-foreground mb-4">
            {t('releaseNotes.readyToTry')}
          </h2>
          <p className="text-muted-foreground mb-6 max-w-lg mx-auto">
            {t('releaseNotes.readyToTryDesc')}
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Button size="lg" onClick={() => navigate('/login')}>
              <Rocket className="h-5 w-5 mr-2" />
              {t('landing.getStarted')}
            </Button>
            <Button variant="outline" size="lg" asChild>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/CHANGELOG.md"
                target="_blank"
                rel="noopener noreferrer"
              >
                <FileText className="h-5 w-5 mr-2" />
                {t('releaseNotes.fullChangelog')}
              </a>
            </Button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-6 border-t border-border">
        <div className="container mx-auto px-4 max-w-4xl">
          <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
            <p className="text-sm text-muted-foreground">
              © {new Date().getFullYear()} ShipFlow. Open source under MIT License.
            </p>
            <Button variant="link" onClick={() => navigate('/')} className="text-sm">
              {t('releaseNotes.backToHome')}
            </Button>
          </div>
        </div>
      </footer>
    </div>
  );
}
