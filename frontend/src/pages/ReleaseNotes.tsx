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
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';

interface Release {
  version: string;
  date: string;
  title: string;
  highlights: {
    icon: React.ReactNode;
    title: string;
    description: string;
  }[];
}

const releases: Release[] = [
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
            {t('releaseNotes.latestVersion')}: v{releases[0].version}
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
                <Card className={index === 0 ? 'border-primary/50 bg-primary/5' : ''}>
                  <CardHeader>
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                      <div className="flex items-center gap-3">
                        <Badge
                          variant={index === 0 ? 'default' : 'secondary'}
                          className="text-sm px-3 py-1"
                        >
                          v{release.version}
                        </Badge>
                        {index === 0 && (
                          <Badge variant="outline" className="bg-green-500/10 text-green-600 border-green-500/30">
                            {t('releaseNotes.latest')}
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
