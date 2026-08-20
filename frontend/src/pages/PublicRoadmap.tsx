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
  Users,
  Activity,
  FileText,
  Layout,
  TrendingUp,
  Paperclip,
  ListChecks,
  Bell,
  Mail,
  BookmarkCheck,
  Container,
  Link2,
  FlaskConical,
  Layers,
  Brain,
  Globe,
  Workflow,
  Sliders,
  Smartphone,
  Rss,
  Network,
  Pencil,
  GripHorizontal,
  TrendingDown,
  Gauge,
  Target,
  Upload,
  FileSpreadsheet,
  FolderInput,
  BookOpen,
  Key,
  Wand2,
  Zap,
  ClipboardList,
  Puzzle,
  Fingerprint,
  WifiOff,
  Download,
  Kanban,
  Filter,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { useSeo, breadcrumbSchema } from '@/hooks/useSeo';

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

export default function PublicRoadmap() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  useSeo({
    title: 'Public Roadmap',
    description:
      'What ships next in ShipFlow — the open-source Shape Up, Scrum and Kanban platform. Current milestone, planned features, and recently shipped releases.',
    path: '/public-roadmap',
    keywords: ['shipflow roadmap', 'open source project management roadmap'],
    jsonLd: breadcrumbSchema([
      { name: 'Home', path: '/' },
      { name: 'Roadmap', path: '/public-roadmap' },
    ]),
  });

  const statusConfig = {
    'in-progress': {
      label: t('publicRoadmap.statusInProgress'),
      badge: 'bg-blue-500/10 text-blue-600 border-blue-500/30',
      card: 'border-blue-500/30 bg-blue-500/5',
    },
    planned: {
      label: t('publicRoadmap.statusPlanned'),
      badge: 'bg-amber-500/10 text-amber-600 border-amber-500/30',
      card: '',
    },
    future: {
      label: t('publicRoadmap.statusFuture'),
      badge: 'bg-purple-500/10 text-purple-600 border-purple-500/30',
      card: '',
    },
  };

  const recentlyShipped: RoadmapRelease[] = [
    {
      version: '1.11.2',
      date: 'August 5, 2026',
      title: t('publicRoadmap.shipped1112Title'),
      highlights: [
        { icon: <GitBranch className="h-5 w-5" />, title: t('publicRoadmap.shipped1112Item0Title'), description: t('publicRoadmap.shipped1112Item0Desc') },
        { icon: <Kanban className="h-5 w-5" />, title: t('publicRoadmap.shipped1112Item1Title'), description: t('publicRoadmap.shipped1112Item1Desc') },
        { icon: <Filter className="h-5 w-5" />, title: t('publicRoadmap.shipped1112Item2Title'), description: t('publicRoadmap.shipped1112Item2Desc') },
      ],
    },
    {
      version: '1.11.1',
      date: 'July 30, 2026',
      title: t('publicRoadmap.shipped1111Title'),
      highlights: [
        { icon: <Fingerprint className="h-5 w-5" />, title: t('publicRoadmap.shipped1111Item0Title'), description: t('publicRoadmap.shipped1111Item0Desc') },
        { icon: <Download className="h-5 w-5" />, title: t('publicRoadmap.shipped1111Item1Title'), description: t('publicRoadmap.shipped1111Item1Desc') },
        { icon: <Layers className="h-5 w-5" />, title: t('publicRoadmap.shipped1111Item2Title'), description: t('publicRoadmap.shipped1111Item2Desc') },
      ],
    },
    {
      version: '1.11.0',
      date: 'July 28, 2026',
      title: t('publicRoadmap.phaseMobileTheme'),
      highlights: [
        { icon: <WifiOff className="h-5 w-5" />, title: t('publicRoadmap.phaseMobileItem0Title'), description: t('publicRoadmap.phaseMobileItem0Desc') },
        { icon: <RefreshCw className="h-5 w-5" />, title: t('publicRoadmap.phaseMobileItem1Title'), description: t('publicRoadmap.phaseMobileItem1Desc') },
        { icon: <Bell className="h-5 w-5" />, title: t('publicRoadmap.phaseMobileItem2Title'), description: t('publicRoadmap.phaseMobileItem2Desc') },
        { icon: <Fingerprint className="h-5 w-5" />, title: t('publicRoadmap.phaseMobileItem3Title'), description: t('publicRoadmap.phaseMobileItem3Desc') },
      ],
    },
    {
      version: '1.10.0',
      date: 'July 14, 2026',
      title: t('publicRoadmap.shipped1100Title'),
      highlights: [
        { icon: <Link2 className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item0Title'), description: t('publicRoadmap.shipped1100Item0Desc') },
        { icon: <GripHorizontal className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item1Title'), description: t('publicRoadmap.shipped1100Item1Desc') },
        { icon: <Target className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item2Title'), description: t('publicRoadmap.shipped1100Item2Desc') },
        { icon: <Sparkles className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item3Title'), description: t('publicRoadmap.shipped1100Item3Desc') },
        { icon: <ClipboardList className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item4Title'), description: t('publicRoadmap.shipped1100Item4Desc') },
        { icon: <Key className="h-5 w-5" />, title: t('publicRoadmap.shipped1100Item5Title'), description: t('publicRoadmap.shipped1100Item5Desc') },
      ],
    },
    {
      version: '1.9.0',
      date: 'July 1, 2026',
      title: t('publicRoadmap.shipped1900Title'),
      highlights: [
        { icon: <Container className="h-5 w-5" />, title: t('publicRoadmap.shipped1900Item0Title'), description: t('publicRoadmap.shipped1900Item0Desc') },
        { icon: <Activity className="h-5 w-5" />, title: t('publicRoadmap.shipped1900Item1Title'), description: t('publicRoadmap.shipped1900Item1Desc') },
        { icon: <FileText className="h-5 w-5" />, title: t('publicRoadmap.shipped1900Item2Title'), description: t('publicRoadmap.shipped1900Item2Desc') },
        { icon: <Lock className="h-5 w-5" />, title: t('publicRoadmap.shipped1900Item3Title'), description: t('publicRoadmap.shipped1900Item3Desc') },
      ],
    },
    {
      version: '1.8.0',
      date: 'June 27, 2026',
      title: t('publicRoadmap.shipped180Title'),
      highlights: [
        { icon: <Sliders className="h-5 w-5" />, title: t('publicRoadmap.phase180Item0Title'), description: t('publicRoadmap.phase180Item0Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.phase180Item1Title'), description: t('publicRoadmap.phase180Item1Desc') },
        { icon: <BookOpen className="h-5 w-5" />, title: t('publicRoadmap.shipped190Item0Title'), description: t('publicRoadmap.shipped190Item0Desc') },
        { icon: <Brain className="h-5 w-5" />, title: t('publicRoadmap.shipped190Item2Title'), description: t('publicRoadmap.shipped190Item2Desc') },
        { icon: <Layers className="h-5 w-5" />, title: t('publicRoadmap.shipped190Item3Title'), description: t('publicRoadmap.shipped190Item3Desc') },
      ],
    },
    {
      version: '1.7.0',
      date: 'June 15, 2026',
      title: t('publicRoadmap.shipped170Title'),
      highlights: [
        { icon: <Zap className="h-5 w-5" />, title: t('publicRoadmap.shipped170Item0Title'), description: t('publicRoadmap.shipped170Item0Desc') },
        { icon: <Workflow className="h-5 w-5" />, title: t('publicRoadmap.shipped170Item1Title'), description: t('publicRoadmap.shipped170Item1Desc') },
        { icon: <Activity className="h-5 w-5" />, title: t('publicRoadmap.shipped170Item2Title'), description: t('publicRoadmap.shipped170Item2Desc') },
        { icon: <FileText className="h-5 w-5" />, title: t('publicRoadmap.shipped170Item3Title'), description: t('publicRoadmap.shipped170Item3Desc') },
      ],
    },
    {
      version: '1.6.0',
      date: 'June 15, 2026',
      title: t('publicRoadmap.shipped160Title'),
      highlights: [
        { icon: <Brain className="h-5 w-5" />, title: t('publicRoadmap.shipped160Item0Title'), description: t('publicRoadmap.shipped160Item0Desc') },
        { icon: <Plug className="h-5 w-5" />, title: t('publicRoadmap.shipped160Item1Title'), description: t('publicRoadmap.shipped160Item1Desc') },
        { icon: <BookOpen className="h-5 w-5" />, title: t('publicRoadmap.shipped160Item2Title'), description: t('publicRoadmap.shipped160Item2Desc') },
        { icon: <Network className="h-5 w-5" />, title: t('publicRoadmap.shipped160Item3Title'), description: t('publicRoadmap.shipped160Item3Desc') },
      ],
    },
    {
      version: '1.5.0',
      date: 'June 7, 2026',
      title: t('publicRoadmap.shipped150Title'),
      highlights: [
        { icon: <Wand2 className="h-5 w-5" />, title: t('publicRoadmap.shipped150Item0Title'), description: t('publicRoadmap.shipped150Item0Desc') },
        { icon: <Sparkles className="h-5 w-5" />, title: t('publicRoadmap.shipped150Item1Title'), description: t('publicRoadmap.shipped150Item1Desc') },
        { icon: <TrendingUp className="h-5 w-5" />, title: t('publicRoadmap.shipped150Item2Title'), description: t('publicRoadmap.shipped150Item2Desc') },
      ],
    },
    {
      version: '1.4.0',
      date: 'June 7, 2026',
      title: t('publicRoadmap.shipped140Title'),
      highlights: [
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped140Item0Title'), description: t('publicRoadmap.shipped140Item0Desc') },
        { icon: <Users className="h-5 w-5" />, title: t('publicRoadmap.shipped140Item1Title'), description: t('publicRoadmap.shipped140Item1Desc') },
        { icon: <GripHorizontal className="h-5 w-5" />, title: t('publicRoadmap.shipped140Item2Title'), description: t('publicRoadmap.shipped140Item2Desc') },
        { icon: <Pencil className="h-5 w-5" />, title: t('publicRoadmap.shipped140Item3Title'), description: t('publicRoadmap.shipped140Item3Desc') },
        { icon: <Key className="h-5 w-5" />, title: t('publicRoadmap.shipped140Item4Title'), description: t('publicRoadmap.shipped140Item4Desc') },
      ],
    },
    {
      version: '1.3.0',
      date: 'June 5, 2026',
      title: t('publicRoadmap.shipped130Title'),
      highlights: [
        { icon: <Plug className="h-5 w-5" />, title: t('publicRoadmap.shipped130Item0Title'), description: t('publicRoadmap.shipped130Item0Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped130Item1Title'), description: t('publicRoadmap.shipped130Item1Desc') },
        { icon: <Key className="h-5 w-5" />, title: t('publicRoadmap.shipped130Item2Title'), description: t('publicRoadmap.shipped130Item2Desc') },
        { icon: <Lock className="h-5 w-5" />, title: t('publicRoadmap.shipped130Item3Title'), description: t('publicRoadmap.shipped130Item3Desc') },
      ],
    },
    {
      version: '1.2.0',
      date: 'May 23, 2026',
      title: t('publicRoadmap.shipped120Title'),
      highlights: [
        { icon: <Upload className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item0Title'), description: t('publicRoadmap.shipped120Item0Desc') },
        { icon: <FileSpreadsheet className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item1Title'), description: t('publicRoadmap.shipped120Item1Desc') },
        { icon: <FolderInput className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item2Title'), description: t('publicRoadmap.shipped120Item2Desc') },
        { icon: <Workflow className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item3Title'), description: t('publicRoadmap.shipped120Item3Desc') },
        { icon: <Plug className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item4Title'), description: t('publicRoadmap.shipped120Item4Desc') },
        { icon: <Layers className="h-5 w-5" />, title: t('publicRoadmap.shipped120Item5Title'), description: t('publicRoadmap.shipped120Item5Desc') },
      ],
    },
    {
      version: '1.1.0',
      date: 'May 19, 2026',
      title: t('publicRoadmap.shipped110Title'),
      highlights: [
        { icon: <Workflow className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item0Title'), description: t('publicRoadmap.shipped110Item0Desc') },
        { icon: <Target className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item1Title'), description: t('publicRoadmap.shipped110Item1Desc') },
        { icon: <Layers className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item2Title'), description: t('publicRoadmap.shipped110Item2Desc') },
        { icon: <TrendingDown className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item3Title'), description: t('publicRoadmap.shipped110Item3Desc') },
        { icon: <Gauge className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item4Title'), description: t('publicRoadmap.shipped110Item4Desc') },
        { icon: <FileText className="h-5 w-5" />, title: t('publicRoadmap.shipped110Item5Title'), description: t('publicRoadmap.shipped110Item5Desc') },
      ],
    },
    {
      version: '1.0.0',
      date: 'April 21, 2026',
      title: t('publicRoadmap.shipped100Title'),
      highlights: [
        { icon: <Network className="h-5 w-5" />, title: t('publicRoadmap.shipped100Item0Title'), description: t('publicRoadmap.shipped100Item0Desc') },
        { icon: <Brain className="h-5 w-5" />, title: t('publicRoadmap.shipped100Item1Title'), description: t('publicRoadmap.shipped100Item1Desc') },
        { icon: <Rss className="h-5 w-5" />, title: t('publicRoadmap.shipped100Item2Title'), description: t('publicRoadmap.shipped100Item2Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped100Item3Title'), description: t('publicRoadmap.shipped100Item3Desc') },
        { icon: <Container className="h-5 w-5" />, title: t('publicRoadmap.shipped100Item4Title'), description: t('publicRoadmap.shipped100Item4Desc') },
      ],
    },
    {
      version: '1.0.0-rc1',
      date: 'April 14, 2026',
      title: t('publicRoadmap.shipped100rc1Title'),
      highlights: [
        { icon: <FileText className="h-5 w-5" />, title: t('publicRoadmap.shipped100rc1Item0Title'), description: t('publicRoadmap.shipped100rc1Item0Desc') },
        { icon: <Github className="h-5 w-5" />, title: t('publicRoadmap.shipped100rc1Item1Title'), description: t('publicRoadmap.shipped100rc1Item1Desc') },
        { icon: <FlaskConical className="h-5 w-5" />, title: t('publicRoadmap.shipped100rc1Item2Title'), description: t('publicRoadmap.shipped100rc1Item2Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped100rc1Item3Title'), description: t('publicRoadmap.shipped100rc1Item3Desc') },
      ],
    },
    {
      version: '0.9.0',
      date: 'April 14, 2026',
      title: t('publicRoadmap.shipped090Title'),
      highlights: [
        { icon: <Brain className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item0Title'), description: t('publicRoadmap.shipped090Item0Desc') },
        { icon: <Bell className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item1Title'), description: t('publicRoadmap.shipped090Item1Desc') },
        { icon: <BookmarkCheck className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item2Title'), description: t('publicRoadmap.shipped090Item2Desc') },
        { icon: <Mail className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item3Title'), description: t('publicRoadmap.shipped090Item3Desc') },
        { icon: <FlaskConical className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item4Title'), description: t('publicRoadmap.shipped090Item4Desc') },
        { icon: <Layers className="h-5 w-5" />, title: t('publicRoadmap.shipped090Item5Title'), description: t('publicRoadmap.shipped090Item5Desc') },
      ],
    },
    {
      version: '0.8.0',
      date: 'April 5, 2026',
      title: t('publicRoadmap.shipped080Title'),
      highlights: [
        { icon: <Brain className="h-5 w-5" />, title: t('publicRoadmap.shipped080Item0Title'), description: t('publicRoadmap.shipped080Item0Desc') },
        { icon: <Sparkles className="h-5 w-5" />, title: t('publicRoadmap.shipped080Item1Title'), description: t('publicRoadmap.shipped080Item1Desc') },
        { icon: <Paperclip className="h-5 w-5" />, title: t('publicRoadmap.shipped080Item2Title'), description: t('publicRoadmap.shipped080Item2Desc') },
        { icon: <ListChecks className="h-5 w-5" />, title: t('publicRoadmap.shipped080Item3Title'), description: t('publicRoadmap.shipped080Item3Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped080Item4Title'), description: t('publicRoadmap.shipped080Item4Desc') },
      ],
    },
    {
      version: '0.7.0',
      date: 'March 24, 2026',
      title: t('publicRoadmap.shipped070Title'),
      highlights: [
        { icon: <Plug className="h-5 w-5" />, title: t('publicRoadmap.shipped070Item0Title'), description: t('publicRoadmap.shipped070Item0Desc') },
        { icon: <Cpu className="h-5 w-5" />, title: t('publicRoadmap.shipped070Item1Title'), description: t('publicRoadmap.shipped070Item1Desc') },
        { icon: <Lock className="h-5 w-5" />, title: t('publicRoadmap.shipped070Item2Title'), description: t('publicRoadmap.shipped070Item2Desc') },
        { icon: <GitBranch className="h-5 w-5" />, title: t('publicRoadmap.shipped070Item3Title'), description: t('publicRoadmap.shipped070Item3Desc') },
      ],
    },
    {
      version: '0.6.2',
      date: 'February 26, 2026',
      title: t('publicRoadmap.shipped062Title'),
      highlights: [
        { icon: <RefreshCw className="h-5 w-5" />, title: t('publicRoadmap.shipped062Item0Title'), description: t('publicRoadmap.shipped062Item0Desc') },
        { icon: <Shield className="h-5 w-5" />, title: t('publicRoadmap.shipped062Item1Title'), description: t('publicRoadmap.shipped062Item1Desc') },
        { icon: <Activity className="h-5 w-5" />, title: t('publicRoadmap.shipped062Item2Title'), description: t('publicRoadmap.shipped062Item2Desc') },
        { icon: <Command className="h-5 w-5" />, title: t('publicRoadmap.shipped062Item3Title'), description: t('publicRoadmap.shipped062Item3Desc') },
      ],
    },
    {
      version: '0.6.1',
      date: 'February 25, 2026',
      title: t('publicRoadmap.shipped061Title'),
      highlights: [
        { icon: <FileText className="h-5 w-5" />, title: t('publicRoadmap.shipped061Item0Title'), description: t('publicRoadmap.shipped061Item0Desc') },
        { icon: <Layout className="h-5 w-5" />, title: t('publicRoadmap.shipped061Item1Title'), description: t('publicRoadmap.shipped061Item1Desc') },
        { icon: <TrendingUp className="h-5 w-5" />, title: t('publicRoadmap.shipped061Item2Title'), description: t('publicRoadmap.shipped061Item2Desc') },
      ],
    },
  ];

  const upcomingPhases: RoadmapPhase[] = [
    {
      version: 'v1.12.0',
      theme: t('publicRoadmap.phasePluginTheme'),
      status: 'planned',
      items: [
        { icon: <Puzzle className="h-5 w-5" />, title: t('publicRoadmap.phasePluginItem0Title'), description: t('publicRoadmap.phasePluginItem0Desc') },
        { icon: <Command className="h-5 w-5" />, title: t('publicRoadmap.phasePluginItem1Title'), description: t('publicRoadmap.phasePluginItem1Desc') },
        { icon: <GitBranch className="h-5 w-5" />, title: t('publicRoadmap.phasePluginItem2Title'), description: t('publicRoadmap.phasePluginItem2Desc') },
      ],
    },
  ];

  const futureVision = [
    { icon: <Layers className="h-6 w-6" />, title: t('publicRoadmap.futureItem0Title'), description: t('publicRoadmap.futureItem0Desc') },
    { icon: <Globe className="h-6 w-6" />, title: t('publicRoadmap.futureItem1Title'), description: t('publicRoadmap.futureItem1Desc') },
    { icon: <Workflow className="h-6 w-6" />, title: t('publicRoadmap.futureItem2Title'), description: t('publicRoadmap.futureItem2Desc') },
    { icon: <Sliders className="h-6 w-6" />, title: t('publicRoadmap.futureItem3Title'), description: t('publicRoadmap.futureItem3Desc') },
    { icon: <Brain className="h-6 w-6" />, title: t('publicRoadmap.futureItem4Title'), description: t('publicRoadmap.futureItem4Desc') },
    { icon: <Smartphone className="h-6 w-6" />, title: t('publicRoadmap.futureItem5Title'), description: t('publicRoadmap.futureItem5Desc') },
  ];

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
              {/* Below `sm` these two overlapped the "ShipFlow" logo — no
                  breakpoint hiding at all, unlike every other header in the
                  app. Hide the secondary links and keep the primary CTA,
                  matching Landing.tsx's "always show Get Started" pattern. */}
              <Button variant="ghost" size="sm" className="hidden sm:inline-flex" onClick={() => navigate('/releases')}>
                {t('landing.whatsNew')}
              </Button>
              <Button variant="ghost" size="sm" className="hidden sm:inline-flex" onClick={() => navigate('/compare')}>
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
