import { useNavigate, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LogIn,
  BarChart3,
  Brain,
  Target,
  CheckCircle,
  Github,
  RotateCcw,
  ArrowRight,
  Linkedin,
  Mail,
  Code2,
  Sparkles,
  Layers,
  Heart,
  Command,
  ShieldCheck,
  Server,
  Lock,
  KanbanSquare,
  Workflow,
  BookOpen,
  Database,
  KeyRound,
  ScrollText,
  Globe,
  Boxes,
  Cpu,
  Container,
  Gauge,
  Terminal,
  Bot,
} from 'lucide-react';
import { useAuth } from '../contexts';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import { useSeo, organizationSchema, SITE_URL, DEFAULT_OG_IMAGE } from '@/hooks/useSeo';

export default function Landing() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading } = useAuth();

  useSeo({
    title: 'ShipFlow — Open-Source Project Management (Shape Up, Kanban, Scrum)',
    exactTitle: true,
    // 153 chars — Google truncates around 160, and the truncated tail is where
    // the "Scrum and Kanban too" reassurance lives, so it has to fit.
    description:
      'Open-source project management for Shape Up, Scrum, and Kanban in one workspace. Hill charts, betting tables, six-week cycles. Free and self-hostable.',
    path: '/',
    // Shape Up terms lead because they are the winnable ground — low
    // competition and exact intent match. The Scrum/Kanban entries are
    // deliberately the open-source/self-hosted long tail, not the head terms
    // ("scrum software", "kanban board"), which are unwinnable against Jira,
    // Trello and monday.com. See SEO_GUIDE.md.
    keywords: [
      'shape up software',
      'shape up methodology',
      'shape up project management',
      'hill chart',
      'betting table',
      'open source project management',
      'self-hosted project management',
      'open source scrum tool',
      'self-hosted kanban board',
      'shape up scrum kanban',
    ],
    jsonLd: [
      {
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: 'ShipFlow',
        applicationCategory: 'BusinessApplication',
        applicationSubCategory: 'Project Management Software',
        operatingSystem: 'Web, Docker, Kubernetes',
        url: SITE_URL,
        image: DEFAULT_OG_IMAGE,
        description:
          'Open-source, methodology-agnostic project management. Runs Shape Up, Scrum, and Kanban projects side by side in one workspace, with hill charts, betting tables, sprints, and AI-assisted planning. Self-hostable.',
        license: 'https://opensource.org/licenses/MIT',
        isAccessibleForFree: true,
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'USD',
          description: 'Free and open source. Self-host with Docker or Kubernetes.',
        },
        featureList: [
          'Shape Up cycles, betting table, and appetite budgeting',
          'Hill charts for honest progress tracking',
          'Scrum sprints, velocity, and burndown',
          'Kanban boards with WIP limits',
          'Mixed methodologies side by side in one workspace',
          'AI pitch writer and retrospective summarizer',
          'MCP server for AI assistants',
          'Self-hosting with Helm and Docker',
        ],
        softwareHelp: `${SITE_URL}/blog`,
        // schema.org's dedicated field for separating same-named entities.
        // "ShipFlow" is also the name of unrelated freight/e-commerce shipping
        // platforms and a ship-hull CFD package; this tells search engines
        // which ShipFlow this is.
        disambiguatingDescription:
          'Software-team project management for the Shape Up, Scrum, and Kanban methodologies. Unrelated to shipping, freight, logistics, or naval-architecture products of the same name.',
      },
      organizationSchema,
      {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: 'ShipFlow',
        url: SITE_URL,
        inLanguage: 'en',
        description:
          'Open-source project management for Shape Up, Scrum, and Kanban. Self-hostable and free.',
      },
    ],
  });

  const techStack = [
    { name: 'React 18', color: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20' },
    { name: 'TypeScript', color: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
    { name: 'Spring Boot 3', color: 'bg-green-500/10 text-green-400 border-green-500/20' },
    { name: 'Java 21', color: 'bg-orange-500/10 text-orange-400 border-orange-500/20' },
    { name: 'Ollama', color: 'bg-violet-500/10 text-violet-400 border-violet-500/20' },
    { name: 'PostgreSQL', color: 'bg-sky-500/10 text-sky-400 border-sky-500/20' },
    { name: 'Redis', color: 'bg-red-500/10 text-red-400 border-red-500/20' },
    { name: 'Qdrant', color: 'bg-purple-500/10 text-purple-400 border-purple-500/20' },
  ];

  const trustBadges = [
    { icon: <ShieldCheck className="h-3.5 w-3.5 mr-1" />, label: t('landing.trustMitLicensed') },
    { icon: <CheckCircle className="h-3.5 w-3.5 mr-1" />, label: t('landing.trustNoPerSeat') },
    { icon: <Code2 className="h-3.5 w-3.5 mr-1" />, label: t('landing.trustStack') },
    { icon: <Lock className="h-3.5 w-3.5 mr-1" />, label: t('landing.trustOffline') },
  ];

  const pillars = [
    {
      icon: <Target className="h-8 w-8" />,
      title: t('landing.pillarShapeUpTitle'),
      description: t('landing.pillarShapeUpDesc'),
    },
    {
      icon: <Server className="h-8 w-8" />,
      title: t('landing.pillarSelfHostTitle'),
      description: t('landing.pillarSelfHostDesc'),
    },
    {
      icon: <Lock className="h-8 w-8" />,
      title: t('landing.pillarPrivateAiTitle'),
      description: t('landing.pillarPrivateAiDesc'),
    },
  ];

  const methodologies = [
    {
      icon: <Target className="h-7 w-7" />,
      title: t('landing.methodShapeUp'),
      description: t('landing.methodShapeUpDesc'),
    },
    {
      icon: <KanbanSquare className="h-7 w-7" />,
      title: t('landing.methodKanban'),
      description: t('landing.methodKanbanDesc'),
    },
    {
      icon: <RotateCcw className="h-7 w-7" />,
      title: t('landing.methodScrum'),
      description: t('landing.methodScrumDesc'),
    },
  ];

  const aiCapabilities = [
    {
      icon: <Cpu className="h-7 w-7" />,
      title: t('landing.aiPluggableTitle'),
      description: t('landing.aiPluggableDesc'),
    },
    {
      icon: <Bot className="h-7 w-7" />,
      title: t('landing.aiMcpTitle'),
      description: t('landing.aiMcpDesc'),
    },
    {
      icon: <BookOpen className="h-7 w-7" />,
      title: t('landing.aiRagTitle'),
      description: t('landing.aiRagDesc'),
    },
    {
      icon: <Sparkles className="h-7 w-7" />,
      title: t('landing.aiFeaturesTitle'),
      description: t('landing.aiFeaturesDesc'),
    },
  ];

  const selfHostFeatures = [
    {
      icon: <Container className="h-7 w-7" />,
      title: t('landing.selfHostDockerTitle'),
      description: t('landing.selfHostDockerDesc'),
    },
    {
      icon: <Boxes className="h-7 w-7" />,
      title: t('landing.selfHostHelmTitle'),
      description: t('landing.selfHostHelmDesc'),
    },
    {
      icon: <Gauge className="h-7 w-7" />,
      title: t('landing.selfHostObsTitle'),
      description: t('landing.selfHostObsDesc'),
    },
    {
      icon: <ScrollText className="h-7 w-7" />,
      title: t('landing.selfHostAuditTitle'),
      description: t('landing.selfHostAuditDesc'),
    },
    {
      icon: <KeyRound className="h-7 w-7" />,
      title: t('landing.selfHostSsoTitle'),
      description: t('landing.selfHostSsoDesc'),
    },
    {
      icon: <Database className="h-7 w-7" />,
      title: t('landing.selfHostStorageTitle'),
      description: t('landing.selfHostStorageDesc'),
    },
  ];

  const features = [
    { icon: <Layers className="h-9 w-9" />, title: t('landing.tripleMode'), description: t('landing.tripleModeDesc') },
    { icon: <BarChart3 className="h-9 w-9" />, title: t('landing.hillCharts'), description: t('landing.hillChartsDesc') },
    { icon: <Target className="h-9 w-9" />, title: t('landing.scopeTaskBridge'), description: t('landing.scopeTaskBridgeDesc') },
    { icon: <CheckCircle className="h-9 w-9" />, title: t('landing.bettingTable'), description: t('landing.bettingTableDesc') },
    { icon: <Workflow className="h-9 w-9" />, title: t('landing.workflowAutomations'), description: t('landing.workflowAutomationsDesc') },
    { icon: <BookOpen className="h-9 w-9" />, title: t('landing.builtInWiki'), description: t('landing.builtInWikiDesc') },
    { icon: <Code2 className="h-9 w-9" />, title: t('landing.customFields'), description: t('landing.customFieldsDesc') },
    { icon: <ShieldCheck className="h-9 w-9" />, title: t('landing.advancedRbac'), description: t('landing.advancedRbacDesc') },
    { icon: <Brain className="h-9 w-9" />, title: t('landing.aiRiskAnalysis'), description: t('landing.aiRiskAnalysisDesc') },
    { icon: <BookOpen className="h-9 w-9" />, title: t('landing.knowledgeCenter'), description: t('landing.knowledgeCenterDesc') },
    { icon: <Bot className="h-9 w-9" />, title: t('landing.mcpServer'), description: t('landing.mcpServerDesc') },
    { icon: <KeyRound className="h-9 w-9" />, title: t('landing.ssoScim'), description: t('landing.ssoScimDesc') },
    { icon: <Database className="h-9 w-9" />, title: t('landing.objectStorage'), description: t('landing.objectStorageDesc') },
    { icon: <Heart className="h-9 w-9" />, title: t('landing.reportsAnalytics'), description: t('landing.reportsAnalyticsDesc') },
    { icon: <Command className="h-9 w-9" />, title: t('landing.globalSearch'), description: t('landing.globalSearchDesc') },
    { icon: <Globe className="h-9 w-9" />, title: t('landing.i18nRtl'), description: t('landing.i18nRtlDesc') },
    { icon: <ScrollText className="h-9 w-9" />, title: t('landing.auditTrail'), description: t('landing.auditTrailDesc') },
  ];

  const steps = [
    { step: '1', title: t('landing.shapeStep'), description: t('landing.shapeStepDesc') },
    { step: '2', title: t('landing.betStep'), description: t('landing.betStepDesc') },
    { step: '3', title: t('landing.buildStep'), description: t('landing.buildStepDesc') },
    { step: '4', title: t('landing.cooldownStep'), description: t('landing.cooldownStepDesc') },
  ];

  // Redirect authenticated users to dashboard
  if (!isLoading && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Hero Section */}
      <section className="relative py-16 md:py-24 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="grid lg:grid-cols-2 gap-10 lg:gap-12 items-center">
            <div>
              {/* Logo & Title */}
              <div className="flex items-center gap-3 mb-6">
                <img src="/icon.png" alt="ShipFlow" className="w-12 h-12 rounded-xl" />
                <span className="text-2xl font-bold text-primary">ShipFlow</span>
              </div>

              <p className="inline-flex items-center rounded-full border border-border bg-muted px-3 py-1 text-xs font-medium text-muted-foreground mb-5">
                {t('landing.heroEyebrow')}
              </p>

              <h1 className="text-4xl md:text-5xl font-bold tracking-tight text-foreground mb-4">
                {t('landing.heroHeadline')}
              </h1>

              <p className="text-lg text-muted-foreground mb-8 leading-relaxed">
                {t('landing.heroSubhead')}
              </p>

              {/* CTA Buttons */}
              <div className="flex flex-wrap gap-3 mb-6">
                <Button size="lg" onClick={() => navigate('/login')}>
                  <LogIn className="h-5 w-5 mr-2" />
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
                <Button variant="ghost" size="lg" onClick={() => navigate('/compare')}>
                  <ArrowRight className="h-5 w-5 mr-2" />
                  {t('landing.compareToCompetitors')}
                </Button>
                <Button variant="ghost" size="lg" onClick={() => navigate('/releases')}>
                  <Sparkles className="h-5 w-5 mr-2" />
                  {t('landing.whatsNew')}
                </Button>
                <Button variant="ghost" size="lg" onClick={() => navigate('/public-roadmap')}>
                  <ArrowRight className="h-5 w-5 mr-2" />
                  {t('landing.roadmap')}
                </Button>
              </div>

              {/* Trust row */}
              <div className="flex flex-wrap gap-2">
                {trustBadges.map((badge) => (
                  <Badge
                    key={badge.label}
                    variant="outline"
                    className="bg-muted text-muted-foreground border-border font-medium"
                  >
                    {badge.icon}
                    {badge.label}
                  </Badge>
                ))}
              </div>
            </div>

            {/* Signature terminal card */}
            <div>
              <Card className="shadow-2xl overflow-hidden">
                <div className="flex items-center gap-2 border-b border-border bg-muted px-4 py-3">
                  <span className="h-3 w-3 rounded-full bg-red-500/70" />
                  <span className="h-3 w-3 rounded-full bg-amber-500/70" />
                  <span className="h-3 w-3 rounded-full bg-green-500/70" />
                  <span className="ml-3 flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                    <Terminal className="h-3.5 w-3.5" />
                    {t('landing.terminalTitle')}
                  </span>
                </div>
                <CardContent className="bg-muted/40 p-5 font-mono text-sm leading-relaxed">
                  <p className="text-muted-foreground/80">{t('landing.terminalComment')}</p>
                  <p className="mt-2 text-foreground">
                    <span className="text-primary">$</span> git clone https://github.com/farzad-sedaghatbin/ShipFlow.git
                  </p>
                  <p className="text-foreground">
                    <span className="text-primary">$</span> cd ShipFlow
                  </p>
                  <p className="text-foreground">
                    <span className="text-primary">$</span> docker compose up -d
                  </p>
                  <p className="mt-3 flex items-center gap-2 text-green-500">
                    <CheckCircle className="h-4 w-4 shrink-0" />
                    {t('landing.terminalUp')}
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Three Pillars (the moat) */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.pillarsTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.pillarsDesc')}</p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            {pillars.map((pillar) => (
              <Card key={pillar.title} className="h-full hover:-translate-y-1 hover:shadow-lg transition-all">
                <CardContent className="p-6">
                  <div className="mb-4 inline-flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    {pillar.icon}
                  </div>
                  <h3 className="text-lg font-semibold text-foreground mb-2">{pillar.title}</h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">{pillar.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Three methodologies, one workspace */}
      <section className="py-16 md:py-24">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <p className="text-sm font-semibold uppercase tracking-wide text-primary mb-3">
                {t('landing.methodologiesEyebrow')}
              </p>
              <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.methodologiesTitle')}</h2>
              <p className="text-muted-foreground mb-8 leading-relaxed">{t('landing.methodologiesDesc')}</p>

              <div className="space-y-4">
                {methodologies.map((method) => (
                  <div key={method.title} className="flex gap-4">
                    <div className="mt-0.5 inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                      {method.icon}
                    </div>
                    <div>
                      <h3 className="font-semibold text-foreground">{method.title}</h3>
                      <p className="text-sm text-muted-foreground">{method.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Hill chart preview (secondary visual) */}
            <Card className="shadow-xl">
              <CardContent className="p-6">
                <p className="text-sm text-muted-foreground mb-4">{t('landing.hillChartPreview')}</p>
                <svg viewBox="0 0 400 150" className="w-full" role="img" aria-label={t('landing.hillChartPreview')}>
                  <path
                    d="M 0 150 Q 100 150 200 30 Q 300 150 400 150"
                    fill="none"
                    stroke="currentColor"
                    className="text-primary/30"
                    strokeWidth="3"
                  />
                  <line
                    x1="200"
                    y1="0"
                    x2="200"
                    y2="150"
                    stroke="currentColor"
                    className="text-border"
                    strokeDasharray="5,5"
                  />
                  <circle cx="80" cy="120" r="10" className="fill-amber-500" />
                  <circle cx="150" cy="60" r="10" className="fill-blue-500" />
                  <circle cx="280" cy="80" r="10" className="fill-green-500" />
                  <circle cx="350" cy="130" r="10" className="fill-green-500" />
                  <text x="60" y="145" fontSize="10" className="fill-muted-foreground">
                    {t('landing.figuringOut')}
                  </text>
                  <text x="280" y="145" fontSize="10" className="fill-muted-foreground">
                    {t('landing.makingHappen')}
                  </text>
                </svg>

                <Separator className="my-4" />

                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full bg-green-500" />
                    <span className="text-sm">2 {t('landing.tasksCompleted')}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                    <span className="text-sm">1 {t('landing.taskInProgress')}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                    <span className="text-sm">1 {t('landing.taskInDiscovery')}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* AI that stays on your infrastructure */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <p className="text-sm font-semibold uppercase tracking-wide text-primary mb-3">{t('landing.aiEyebrow')}</p>
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.aiTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.aiDesc')}</p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {aiCapabilities.map((cap) => (
              <Card key={cap.title} className="h-full hover:-translate-y-1 hover:shadow-lg transition-all">
                <CardContent className="p-6">
                  <div className="text-primary mb-4">{cap.icon}</div>
                  <h3 className="font-semibold text-foreground mb-2">{cap.title}</h3>
                  <p className="text-sm text-muted-foreground">{cap.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Production-grade self-hosting */}
      <section className="py-16 md:py-24">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <p className="text-sm font-semibold uppercase tracking-wide text-primary mb-3">{t('landing.selfHostEyebrow')}</p>
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.selfHostTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.selfHostDesc')}</p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {selfHostFeatures.map((feat) => (
              <Card key={feat.title} className="h-full hover:-translate-y-1 hover:shadow-lg transition-all">
                <CardContent className="p-6">
                  <div className="text-primary mb-4">{feat.icon}</div>
                  <h3 className="font-semibold text-foreground mb-2">{feat.title}</h3>
                  <p className="text-sm text-muted-foreground">{feat.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Curated Feature Grid */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <p className="text-sm font-semibold uppercase tracking-wide text-primary mb-3">{t('landing.featuresEyebrow')}</p>
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.featuresTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.featuresDesc')}</p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => (
              <Card key={index} className="h-full hover:-translate-y-1 hover:shadow-lg transition-all">
                <CardContent className="p-6">
                  <div className="text-primary mb-4">{feature.icon}</div>
                  <h3 className="font-semibold text-foreground mb-2">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Honest Comparison Teaser */}
      <section className="py-16 md:py-24">
        <div className="container mx-auto px-4 max-w-4xl">
          <Card className="bg-gradient-to-br from-primary/5 to-secondary/10 border-primary/10">
            <CardContent className="p-8 md:p-12 text-center">
              <p className="text-sm font-semibold uppercase tracking-wide text-primary mb-4">
                {t('landing.comparisonEyebrow')}
              </p>
              <h2 className="text-2xl md:text-3xl font-bold text-foreground mb-4">{t('landing.comparisonTitle')}</h2>
              <p className="text-muted-foreground mb-2 leading-relaxed">{t('landing.comparisonLead')}</p>
              <p className="text-lg font-semibold text-foreground mb-8 leading-relaxed">{t('landing.comparisonPunch')}</p>
              <Button size="lg" onClick={() => navigate('/compare')}>
                <ArrowRight className="h-5 w-5 mr-2" />
                {t('landing.seeComparison')}
              </Button>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* How it Works */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.howItWorksTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.howItWorksDesc')}</p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {steps.map((item) => (
              <div key={item.step} className="text-center">
                <div className="w-14 h-14 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xl font-bold mx-auto mb-4">
                  {item.step}
                </div>
                <h3 className="font-semibold text-foreground mb-2">{item.title}</h3>
                <p className="text-sm text-muted-foreground">{item.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Open Source Section */}
      <section className="py-16 md:py-24">
        <div className="container mx-auto px-4 max-w-6xl">
          <Card className="bg-gradient-to-br from-primary/5 to-secondary/10 border-primary/10">
            <CardContent className="p-8 md:p-12">
              <div className="grid md:grid-cols-3 gap-8 items-center">
                <div className="md:col-span-2">
                  <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.openSourceTitle')}</h2>
                  <p className="text-muted-foreground mb-6">{t('landing.openSourceDesc')}</p>
                  <div className="flex flex-wrap gap-2">
                    {[
                      t('landing.mitLicense'),
                      t('landing.selfHosted'),
                      t('landing.dockerReady'),
                      t('landing.wcag21AA'),
                      t('landing.trustOffline'),
                      t('landing.activeDevelopment'),
                    ].map((label) => (
                      <Badge
                        key={label}
                        variant="outline"
                        className="bg-green-500/10 text-green-400 border-green-500/20"
                      >
                        <CheckCircle className="h-3 w-3 mr-1" />
                        {label}
                      </Badge>
                    ))}
                  </div>
                </div>
                <div className="text-center">
                  <Button size="lg" asChild>
                    <a
                      href="https://github.com/farzad-sedaghatbin/ShipFlow"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Github className="h-5 w-5 mr-2" />
                      {t('common.github')}
                    </a>
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* About Author Section */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">{t('landing.aboutAuthorTitle')}</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">{t('landing.aboutAuthorSubtitle')}</p>
          </div>

          <Card className="max-w-3xl mx-auto">
            <CardContent className="p-8">
              <div className="flex flex-col md:flex-row items-center gap-8">
                <div className="flex-shrink-0">
                  <img
                    src="https://avatars.githubusercontent.com/farzad-sedaghatbin?v=4"
                    alt="Farzad Sedaghatbin"
                    className="w-32 h-32 rounded-full object-cover border-4 border-primary/20"
                  />
                </div>

                <div className="flex-1 text-center md:text-left">
                  <h3 className="text-2xl font-bold text-foreground mb-2">{t('landing.authorName')}</h3>
                  <p className="text-primary font-medium mb-4">{t('landing.authorRole')}</p>
                  <p className="text-muted-foreground mb-6 leading-relaxed">{t('landing.authorBio')}</p>

                  <div className="flex flex-wrap justify-center md:justify-start gap-3">
                    <Button variant="outline" size="sm" asChild>
                      <a
                        href="https://github.com/farzad-sedaghatbin"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <Github className="h-4 w-4 mr-2" />
                        GitHub
                      </a>
                    </Button>
                    <Button variant="outline" size="sm" asChild>
                      <a
                        href="https://www.linkedin.com/in/farzad-sedaghatbin"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <Linkedin className="h-4 w-4 mr-2" />
                        LinkedIn
                      </a>
                    </Button>
                    <Button variant="outline" size="sm" asChild>
                      <a href="mailto:farzad.sedaghatbin@gmail.com">
                        <Mail className="h-4 w-4 mr-2" />
                        {t('landing.contact')}
                      </a>
                    </Button>
                  </div>
                </div>
              </div>

              <Separator className="my-8" />

              <div>
                <h4 className="text-sm font-semibold text-muted-foreground mb-4 flex items-center gap-2">
                  <Code2 className="h-4 w-4" />
                  {t('landing.authorExpertise')}
                </h4>
                <div className="flex flex-wrap gap-2">
                  {[
                    'Digital Banking',
                    'Fintech',
                    'AI',
                    'Java',
                    'Java EE',
                    'Spring Boot',
                    'Quarkus',
                    'React',
                    'React Native',
                    'Cloud-Native',
                    'Solution Architecture',
                    'Nexus/Scrum',
                    'Shape Up',
                  ].map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Tech Stack Section */}
      <section className="py-12">
        <div className="container mx-auto px-4 max-w-6xl text-center">
          <p className="text-sm font-semibold uppercase tracking-wide text-muted-foreground mb-4">
            {t('landing.techStack')}
          </p>
          <div className="flex flex-wrap justify-center gap-2">
            {techStack.map((tech) => (
              <Badge key={tech.name} variant="outline" className={`${tech.color} font-medium`}>
                {tech.name}
              </Badge>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 md:py-20 bg-primary text-primary-foreground">
        <div className="container mx-auto px-4 max-w-3xl text-center">
          <h2 className="text-3xl font-bold mb-4">{t('landing.readyToShapeUp')}</h2>
          <p className="text-lg opacity-90 mb-8">{t('landing.heroSubhead')}</p>
          <Button size="lg" variant="secondary" onClick={() => navigate('/login')}>
            {t('landing.getStarted')}
          </Button>
          <p className="text-sm mt-4 opacity-70">{t('landing.demoCredentials')}</p>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-6 border-t border-border">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
            <p className="text-sm text-muted-foreground">
              {t('landing.copyright', { year: new Date().getFullYear() })}
            </p>
            <nav className="flex gap-6">
              <button
                onClick={() => navigate('/blog')}
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('landing.blog')}
              </button>
              <button
                onClick={() => navigate('/releases')}
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('landing.whatsNew')}
              </button>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('landing.github')}
              </a>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/CONTRIBUTING.md"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('landing.contributing')}
              </a>
              <a
                href="https://basecamp.com/shapeup"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('landing.shapeUpBook')}
              </a>
            </nav>
          </div>
        </div>
      </footer>
    </div>
  );
}
