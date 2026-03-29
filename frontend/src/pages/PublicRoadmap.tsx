import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Rocket,
  Sparkles,
  Github,
  Plug,
  Cpu,
  Lock,
  GitBranch,
  Command,
  RefreshCw,
  Shield,
  Activity,
  FileText,
  Layout,
  TrendingUp,
  Paperclip,
  ListChecks,
  Bell,
  Mail,
  BookmarkCheck,
  Wrench,
  Container,
  Zap,
  FlaskConical,
  Layers,
  Brain,
  Globe,
  Workflow,
  Sliders,
  Smartphone,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';

interface RoadmapRelease {
  version: string;
  date: string;
  title: string;
  highlights: { icon: React.ReactNode; title: string; description: string }[];
}

interface RoadmapPhase {
  version: string;
  theme: string;
  status: 'in-progress' | 'planned' | 'future';
  items: { icon: React.ReactNode; title: string; description: string }[];
}

const recentlyShipped: RoadmapRelease[] = [
  {
    version: '0.7.0',
    date: 'March 24, 2026',
    title: 'MCP Server — AI Editor Integration',
    highlights: [
      {
        icon: <Plug className="h-5 w-5" />,
        title: 'ShipFlow as an MCP Server (opt-in)',
        description:
          'Claude Code, Cursor, Claude Desktop, and any MCP-compatible AI assistant can now query ShipFlow directly from the editor. Enable with MCP_SERVER_ENABLED=true.',
      },
      {
        icon: <Cpu className="h-5 w-5" />,
        title: '10 Read Tools + 1 Write Tool',
        description:
          'list_projects, get_cycles, get_tasks, get_pitches, get_betting_candidates, and more. Plus update_task_status write tool (opt-in).',
      },
      {
        icon: <Lock className="h-5 w-5" />,
        title: 'API Key Auth on All MCP Endpoints',
        description:
          'Bearer token authentication on /mcp/** reuses existing API key scopes (READ / WRITE / ADMIN).',
      },
      {
        icon: <GitBranch className="h-5 w-5" />,
        title: 'HTTP + SSE Transport (JSON-RPC 2.0)',
        description:
          'Standard MCP transport over Spring Boot. Zero additional dependencies.',
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
        description:
          'ShallowEtagHeaderFilter computes ETags; browsers receive 304 Not Modified when resources are unchanged.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Spring Service-Layer Caching with Redis',
        description:
          '@Cacheable / @CacheEvict on eight domain services with per-domain TTLs. Redis in production with in-memory fallback.',
      },
      {
        icon: <Activity className="h-5 w-5" />,
        title: 'React Query & Axios ETag Interceptor',
        description:
          'Per-domain staleTime constants. Dashboard widgets migrated to useQuery / useQueries.',
      },
      {
        icon: <Command className="h-5 w-5" />,
        title: 'Global Search (⌘K)',
        description:
          'Instantly search tasks, bug reports, pitches, and epics using ⌘K. Powered by pg_trgm GIN indexes.',
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
        description:
          'All description fields support Markdown with Write/Preview toggle. Rich rendering across pitches, epics, tasks, and bug reports.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Project Selection Dialog',
        description:
          'A modal guides users to select a project when navigating to pages that require one.',
      },
      {
        icon: <TrendingUp className="h-5 w-5" />,
        title: 'Pitch Prioritization & Drag-and-Drop Reorder',
        description:
          'High / Medium / Low priority labels and drag-and-drop reordering for pitches inside epics.',
      },
    ],
  },
];

const upcomingPhases: RoadmapPhase[] = [
  {
    version: 'v0.8.0',
    theme: 'Core Product + Hardening',
    status: 'in-progress',
    items: [
      {
        icon: <Rocket className="h-5 w-5" />,
        title: 'Public Roadmap Page',
        description: 'This page — built in public, updated every commit.',
      },
      {
        icon: <Wrench className="h-5 w-5" />,
        title: 'Version & Config Alignment',
        description: 'pom.xml version bump, java.version=21, prod CORS fix.',
      },
      {
        icon: <Container className="h-5 w-5" />,
        title: 'Spring Boot Upgrade 3.2.1 → 3.4.x',
        description: 'Latest stable Spring Boot with all compatibility fixes.',
      },
      {
        icon: <Shield className="h-5 w-5" />,
        title: 'Rate Limiting + Security Headers',
        description:
          'Bucket4j rate limiting on auth/search/AI endpoints. CSP headers and startup secret validation.',
      },
      {
        icon: <Zap className="h-5 w-5" />,
        title: 'Docker GHCR CI/CD + Code Splitting',
        description:
          'Automated Docker image push to GHCR on tag. React.lazy code splitting for all 50+ pages.',
      },
      {
        icon: <Paperclip className="h-5 w-5" />,
        title: 'File Attachments on Tasks',
        description:
          'Drag-and-drop file uploads on tasks. Every PM tool has this — now ShipFlow does too.',
      },
      {
        icon: <ListChecks className="h-5 w-5" />,
        title: 'Bulk Task Operations',
        description:
          'Multi-select tasks and bulk assign, change status, change priority, add tag, or delete.',
      },
      {
        icon: <Bell className="h-5 w-5" />,
        title: '@Mention Notifications',
        description:
          '@mentions in comments now trigger real in-app notifications for the mentioned user.',
      },
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'CSV Export for Task Backlog',
        description:
          'Export filtered task lists as CSV directly from the backlog page.',
      },
      {
        icon: <Sparkles className="h-5 w-5" />,
        title: 'Interactive Onboarding Tour',
        description:
          'Step-by-step tour wired with driver.js. Guides new users through Shape Up and Kanban flows.',
      },
    ],
  },
  {
    version: 'v0.9.0',
    theme: 'Power User Features + Quality',
    status: 'planned',
    items: [
      {
        icon: <BookmarkCheck className="h-5 w-5" />,
        title: 'Saved Filter Views',
        description:
          'Save named filter presets in the backlog. Load them with one click. No more recreating the same filters daily.',
      },
      {
        icon: <Bell className="h-5 w-5" />,
        title: 'Real-Time Notifications via SSE',
        description:
          'Replace 30-second polling with Server-Sent Events for instant push notifications.',
      },
      {
        icon: <Mail className="h-5 w-5" />,
        title: 'Email Notifications',
        description:
          'SMTP email support for task assignment, @mentions, and pitch status changes.',
      },
      {
        icon: <Cpu className="h-5 w-5" />,
        title: 'MCP Write Tools (Phase 2)',
        description:
          'create_task, add_comment, create_pitch, update_pitch_status — fully bidirectional AI editor integration.',
      },
      {
        icon: <FlaskConical className="h-5 w-5" />,
        title: 'Playwright E2E Tests',
        description:
          'End-to-end test suites for login, Shape Up lifecycle, hill chart, betting table, and task management.',
      },
      {
        icon: <Layout className="h-5 w-5" />,
        title: 'Component Decomposition',
        description:
          'BacklogPage, OrganizationSettings, and PitchDetail split into focused sub-components.',
      },
    ],
  },
  {
    version: 'v1.0.0',
    theme: 'First Open Source Release',
    status: 'planned',
    items: [
      {
        icon: <FileText className="h-5 w-5" />,
        title: 'VitePress Documentation Site',
        description:
          'Dedicated docs site with Getting Started, User Guide, Admin Guide, and Developer Guide.',
      },
      {
        icon: <Github className="h-5 w-5" />,
        title: 'Community Infrastructure',
        description:
          'GitHub Discussions, GOVERNANCE.md, and Discussion templates for support and ideas.',
      },
      {
        icon: <Container className="h-5 w-5" />,
        title: 'Docker Image on GHCR',
        description:
          'ghcr.io/farzad-sedaghatbin/shipflow:latest available for one-command self-hosted setup.',
      },
      {
        icon: <Rocket className="h-5 w-5" />,
        title: 'Public Launch',
        description:
          'Hacker News, Reddit (r/selfhosted, r/projectmanagement), blog post, and live demo.',
      },
    ],
  },
];

const futureVision = [
  {
    icon: <Layers className="h-6 w-6" />,
    title: 'Scrum Mode (v1.1)',
    description:
      'Sprint entity reusing the Cycle infrastructure — story points, velocity tracking, burndown charts, and sprint planning. The third ProjectType after Shape Up and Kanban.',
  },
  {
    icon: <Globe className="h-6 w-6" />,
    title: 'SSO / SAML / OIDC (v1.1)',
    description:
      'Enterprise-grade authentication via Keycloak, Auth0, or Okta. For organizations running self-hosted at scale.',
  },
  {
    icon: <Workflow className="h-6 w-6" />,
    title: 'Workflow Automations (v1.2)',
    description:
      'Trigger/action rules: "when task → DONE, notify Slack", "when pitch → SHAPED, add to betting table". No more manual coordination.',
  },
  {
    icon: <Sliders className="h-6 w-6" />,
    title: 'Custom Fields (v1.2)',
    description:
      'Per-project configurable fields on tasks and pitches. Story Points, Sprint Goal, Customer — whatever your team needs.',
  },
  {
    icon: <Brain className="h-6 w-6" />,
    title: 'MCP Phases 3–4 (v1.3)',
    description:
      'AI agent tools, Spring AI MCP starter migration, and plugin system for custom integrations.',
  },
  {
    icon: <Smartphone className="h-6 w-6" />,
    title: 'Mobile / PWA (v1.4)',
    description:
      'Progressive Web App with offline support. Helm chart for Kubernetes. OpenTelemetry + Prometheus + Grafana.',
  },
];

const statusConfig = {
  'in-progress': {
    label: 'In Progress',
    badge: 'bg-blue-500/10 text-blue-600 border-blue-500/30',
    card: 'border-blue-500/30 bg-blue-500/5',
  },
  planned: {
    label: 'Planned',
    badge: 'bg-amber-500/10 text-amber-600 border-amber-500/30',
    card: '',
  },
  future: {
    label: 'Future',
    badge: 'bg-purple-500/10 text-purple-600 border-purple-500/30',
    card: '',
  },
};

export default function PublicRoadmap() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
        <div className="container mx-auto px-4 max-w-5xl">
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
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="sm" onClick={() => navigate('/releases')}>
                {t('landing.whatsNew')}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => navigate('/compare')}>
                {t('landing.compareToCompetitors')}
              </Button>
              <Button size="sm" onClick={() => navigate('/login')}>
                {t('landing.getStarted')}
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="py-12 md:py-16 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-5xl text-center">
          <Badge variant="outline" className="mb-4 text-sm px-4 py-1">
            <Rocket className="h-3.5 w-3.5 mr-1.5" />
            {t('publicRoadmap.badge')}
          </Badge>
          <h1 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
            {t('publicRoadmap.heroTitle')}
          </h1>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto mb-6">
            {t('publicRoadmap.heroSubtitle')}
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Button variant="outline" asChild>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Github className="h-4 w-4 mr-2" />
                {t('landing.viewOnGitHub')}
              </a>
            </Button>
            <Button variant="ghost" onClick={() => navigate('/releases')}>
              <Sparkles className="h-4 w-4 mr-2" />
              {t('publicRoadmap.viewReleaseNotes')}
            </Button>
          </div>
        </div>
      </section>

      {/* Upcoming Phases */}
      <section className="py-12 md:py-16">
        <div className="container mx-auto px-4 max-w-5xl">
          <h2 className="text-2xl font-bold text-foreground mb-2">
            {t('publicRoadmap.upcomingTitle')}
          </h2>
          <p className="text-muted-foreground mb-8">{t('publicRoadmap.upcomingSubtitle')}</p>

          <div className="space-y-8">
            {upcomingPhases.map((phase) => {
              const cfg = statusConfig[phase.status];
              return (
                <Card key={phase.version} className={cfg.card}>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div className="flex items-center gap-3">
                        <Badge variant="outline" className="text-sm px-3 py-1">
                          {phase.version}
                        </Badge>
                        <Badge variant="outline" className={cfg.badge}>
                          {cfg.label}
                        </Badge>
                      </div>
                    </div>
                    <CardTitle className="text-xl mt-2">{phase.theme}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid sm:grid-cols-2 gap-4">
                      {phase.items.map((item, i) => (
                        <div
                          key={i}
                          className="flex gap-3 p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                        >
                          <div className="flex-shrink-0 text-primary mt-0.5">{item.icon}</div>
                          <div>
                            <h4 className="font-medium text-foreground mb-1">{item.title}</h4>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                              {item.description}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        </div>
      </section>

      {/* Recently Shipped */}
      <section className="py-12 md:py-16 bg-muted/20">
        <div className="container mx-auto px-4 max-w-5xl">
          <h2 className="text-2xl font-bold text-foreground mb-2">
            {t('publicRoadmap.recentlyShippedTitle')}
          </h2>
          <p className="text-muted-foreground mb-8">
            {t('publicRoadmap.recentlyShippedSubtitle')}
          </p>

          <div className="space-y-8">
            {recentlyShipped.map((release, index) => (
              <div key={release.version}>
                <Card className={index === 0 ? 'border-green-500/30 bg-green-500/5' : ''}>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div className="flex items-center gap-3">
                        <Badge variant={index === 0 ? 'default' : 'secondary'} className="text-sm px-3 py-1">
                          v{release.version}
                        </Badge>
                        {index === 0 && (
                          <Badge variant="outline" className="bg-green-500/10 text-green-600 border-green-500/30">
                            {t('publicRoadmap.latest')}
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
                          <div className="flex-shrink-0 text-primary mt-0.5">{highlight.icon}</div>
                          <div>
                            <h4 className="font-medium text-foreground mb-1">{highlight.title}</h4>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                              {highlight.description}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
                {index < recentlyShipped.length - 1 && (
                  <div className="flex justify-center my-6">
                    <div className="w-px h-8 bg-border" />
                  </div>
                )}
              </div>
            ))}
          </div>

          <div className="mt-8 text-center">
            <Button variant="outline" onClick={() => navigate('/releases')}>
              <Sparkles className="h-4 w-4 mr-2" />
              {t('publicRoadmap.viewAllReleases')}
            </Button>
          </div>
        </div>
      </section>

      {/* Future Vision */}
      <section className="py-12 md:py-16">
        <div className="container mx-auto px-4 max-w-5xl">
          <h2 className="text-2xl font-bold text-foreground mb-2">
            {t('publicRoadmap.futureVisionTitle')}
          </h2>
          <p className="text-muted-foreground mb-8">{t('publicRoadmap.futureVisionSubtitle')}</p>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {futureVision.map((item, i) => (
              <div
                key={i}
                className="flex gap-3 p-4 rounded-lg border bg-card hover:bg-muted/50 transition-colors"
              >
                <div className="flex-shrink-0 text-muted-foreground mt-0.5">{item.icon}</div>
                <div>
                  <h4 className="font-medium text-foreground mb-1">{item.title}</h4>
                  <p className="text-sm text-muted-foreground leading-relaxed">{item.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-12 md:py-16 bg-muted/30">
        <div className="container mx-auto px-4 max-w-5xl text-center">
          <h2 className="text-2xl font-bold text-foreground mb-4">
            {t('publicRoadmap.ctaTitle')}
          </h2>
          <p className="text-muted-foreground mb-6 max-w-lg mx-auto">
            {t('publicRoadmap.ctaSubtitle')}
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Button size="lg" onClick={() => navigate('/login')}>
              <Rocket className="h-5 w-5 mr-2" />
              {t('landing.getStarted')}
            </Button>
            <Button variant="outline" size="lg" asChild>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Github className="h-5 w-5 mr-2" />
                {t('landing.viewOnGitHub')}
              </a>
            </Button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-6 border-t border-border">
        <div className="container mx-auto px-4 max-w-5xl">
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
