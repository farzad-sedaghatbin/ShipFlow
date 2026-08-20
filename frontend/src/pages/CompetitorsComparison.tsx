import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Check,
  X,
  Minus,
  Crown,
  Sparkles,
  Target,
  DollarSign,
  Lock,
  Cloud,
  Zap,
  Brain,
  BarChart3,
  Users,
  Github,
  TrendingUp,
  Shield,
} from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { useSeo, breadcrumbSchema } from '@/hooks/useSeo';

interface ComparisonFeature {
  category: string;
  feature: string;
  shipflow: boolean | 'partial' | 'coming';
  linear: boolean | 'partial';
  asana: boolean | 'partial';
  monday: boolean | 'partial';
  jira: boolean | 'partial';
  basecamp: boolean | 'partial';
  clickup: boolean | 'partial';
}

const comparisonFeatures: ComparisonFeature[] = [
  // Shape Up Methodology
  { category: 'Shape Up', feature: 'Native Shape Up methodology', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true, clickup: false },
  { category: 'Shape Up', feature: '6-week fixed cycles', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true, clickup: false },
  { category: 'Shape Up', feature: 'Betting table for pitches', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Shape Up', feature: 'Pre-cycle pitch lifecycle', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: 'partial', clickup: false },
  { category: 'Shape Up', feature: 'Appetite-based budgeting', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true, clickup: false },
  { category: 'Shape Up', feature: 'Circuit breaker mechanism', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Shape Up', feature: 'Pitch shaping workflow', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: 'partial', clickup: false },
  { category: 'Shape Up', feature: 'Cooldown periods', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true, clickup: false },
  { category: 'Shape Up', feature: 'Dual mode (Shape Up + Kanban)', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },

  // Progress Visualization
  { category: 'Progress', feature: 'Hill charts', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true, clickup: false },
  { category: 'Progress', feature: 'Interactive hill chart editing', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Progress', feature: 'Scope-Task auto-bridging', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Progress', feature: 'Auto-progress from subtasks', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Progress', feature: 'Gantt charts', shipflow: false, linear: false, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Progress', feature: 'Kanban boards', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },
  { category: 'Progress', feature: 'Sprint burndown', shipflow: false, linear: true, asana: 'partial', monday: 'partial', jira: true, basecamp: false, clickup: true },

  // Reporting
  { category: 'Reporting', feature: 'Detailed Cycle Reports', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Reporting', feature: 'Pitch Health Analytics', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Reporting', feature: 'Release cockpit (task/bug breakdown)', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'Reporting', feature: 'Release traceability (tasks & bugs)', shipflow: true, linear: 'partial', asana: false, monday: false, jira: true, basecamp: false, clickup: 'partial' },

  // AI & Intelligence
  { category: 'AI Features', feature: 'AI risk analysis', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: 'partial' },
  { category: 'AI Features', feature: 'AI test case generation', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'AI pitch document extraction', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'AI technical solution generator', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'Team skills-aware recommendations', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'Figma design analysis (MCP)', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'Work context graph API (MCP)', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'Configurable risk weights', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'Risk trend prediction', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'AI Features', feature: 'AI-powered help search', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Search & Navigation', feature: 'Global search (⌘K)', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },

  // QA & Testing
  { category: 'QA', feature: 'Integrated test management', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'QA', feature: 'Bug tracking', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'QA', feature: 'Test execution & runs', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'QA', feature: 'Traceability links', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false, clickup: 'partial' },

  // Team & Collaboration
  { category: 'Team', feature: 'Configurable team capacity', shipflow: true, linear: false, asana: 'partial', monday: 'partial', jira: 'partial', basecamp: false, clickup: 'partial' },
  { category: 'Team', feature: 'Per-person budget tracking', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Team', feature: 'Markdown descriptions (write/preview)', shipflow: true, linear: true, asana: 'partial', monday: 'partial', jira: true, basecamp: 'partial', clickup: true },
  { category: 'Team', feature: 'Retrospectives', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'Team', feature: 'Anonymous retro submissions', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Team', feature: 'Time tracking', shipflow: true, linear: false, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Team', feature: 'Work log timers', shipflow: true, linear: false, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Team', feature: 'Task dependencies', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Team', feature: 'Interactive Help Guides with AI Search', shipflow: true, linear: false, asana: 'partial', monday: 'partial', jira: 'partial', basecamp: 'partial', clickup: 'partial' },

  // Integrations
  { category: 'Integrations', feature: 'GitHub integration', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Integrations', feature: 'Pluggable VCS providers', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'Integrations', feature: 'Slack notifications', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },
  { category: 'Integrations', feature: 'Pluggable notification providers', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'Integrations', feature: 'Microsoft Teams', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },
  { category: 'Integrations', feature: 'Webhooks (outgoing)', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },
  { category: 'Integrations', feature: 'Generic inbound webhooks', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false, clickup: false },
  { category: 'Integrations', feature: 'Pluggable inbound webhook handlers', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Integrations', feature: 'No-code inbound webhook setup (admin UI)', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },

  // Deployment & Pricing
  { category: 'Deployment', feature: 'Self-hosted option', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false, clickup: false },
  { category: 'Deployment', feature: 'Open source', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'Deployment', feature: 'Docker ready', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false, clickup: false },
  { category: 'Deployment', feature: 'Free tier', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false, clickup: true },

  // Accessibility
  { category: 'Accessibility', feature: 'WCAG 2.1 AA compliant', shipflow: true, linear: 'partial', asana: true, monday: 'partial', jira: true, basecamp: 'partial', clickup: 'partial' },
  { category: 'Accessibility', feature: 'Keyboard navigation', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },
  { category: 'Accessibility', feature: 'Screen reader support', shipflow: true, linear: 'partial', asana: true, monday: 'partial', jira: true, basecamp: 'partial', clickup: 'partial' },
  { category: 'Accessibility', feature: 'Mobile responsive', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },

  // Internationalization
  { category: 'i18n', feature: 'Multilingual UI', shipflow: true, linear: 'partial', asana: 'partial', monday: true, jira: true, basecamp: 'partial', clickup: 'partial' },
  { category: 'i18n', feature: 'RTL language support', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'i18n', feature: 'Farsi/Persian translation', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false, clickup: false },
  { category: 'i18n', feature: 'Date/time localization', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true, clickup: true },
];

const competitorColors = {
  linear: 'purple',
  asana: 'red',
  monday: 'orange',
  jira: 'blue',
  basecamp: 'green',
  clickup: 'pink',
};

const renderFeatureCell = (value: boolean | 'partial' | 'coming') => {
  if (value === true) {
    return <Check className="h-5 w-5 text-green-500" />;
  }
  if (value === false) {
    return <X className="h-5 w-5 text-muted-foreground/50" />;
  }
  if (value === 'partial') {
    return <Minus className="h-5 w-5 text-amber-500" />;
  }
  if (value === 'coming') {
    return <Badge variant="outline" className="text-xs">Soon</Badge>;
  }
  return <X className="h-5 w-5 text-muted-foreground/50" />;
};

export default function CompetitorsComparison() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  useSeo({
    title: 'ShipFlow vs Linear, Jira, Asana & Basecamp',
    description:
      'ShipFlow compared feature-by-feature with Linear, Jira, Asana and Basecamp — Shape Up, Scrum and Kanban support, self-hosting, pricing and licensing.',
    path: '/compare',
    keywords: [
      'linear alternative',
      'jira alternative',
      'asana alternative',
      'open source project management',
      'open source scrum tool',
      'self-hosted kanban board',
      'shape up software',
    ],
    jsonLd: breadcrumbSchema([
      { name: 'Home', path: '/' },
      { name: 'Compare', path: '/compare' },
    ]),
  });

  // Get competitor data from translations
  const competitors = [
    { key: 'linear', color: competitorColors.linear },
    { key: 'asana', color: competitorColors.asana },
    { key: 'monday', color: competitorColors.monday },
    { key: 'jira', color: competitorColors.jira },
    { key: 'basecamp', color: competitorColors.basecamp },
    { key: 'clickup', color: competitorColors.clickup },
  ];

  // Group features by category
  const categories = [...new Set(comparisonFeatures.map(f => f.category))];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b border-border bg-background/95 backdrop-blur sticky top-0 z-50">
        <div className="container mx-auto px-4 max-w-7xl">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-4">
              <Button variant="ghost" size="sm" onClick={() => navigate('/')}>
                <ArrowLeft className="h-4 w-4 mr-2" />
                {t('competitorsComparison.backToDashboard')}
              </Button>
              <Separator orientation="vertical" className="h-6" />
              <div className="flex items-center gap-2">
                <img src="/icon.png" alt="ShipFlow" className="w-8 h-8 rounded-lg" />
                <span className="font-semibold text-lg">ShipFlow</span>
              </div>
            </div>
            <Button onClick={() => navigate('/login')}>
              Get Started Free
            </Button>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="py-16 md:py-24 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-6xl text-center">
          <Badge variant="outline" className="mb-4 bg-primary/10 text-primary border-primary/20">
            <Crown className="h-3 w-3 mr-1" />
            Comparison Guide
          </Badge>
          <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-4">
            {t('competitorsComparison.title')}
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto mb-8">
            {t('competitorsComparison.subtitle')}
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            {competitors.map((comp) => (
              <Badge key={comp.key} variant="outline" className="text-sm">
                vs {t(`competitorsComparison.competitorData.${comp.key}.name`)}
              </Badge>
            ))}
          </div>
        </div>
      </section>

      {/* Key Differentiators */}
      <section className="py-16 md:py-20">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">
              {t('competitorsComparison.whyShipFlow')}
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              ShipFlow stands out with unique features designed for teams following Shape Up.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <Target className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">Native Shape Up</h3>
                <p className="text-sm text-muted-foreground">
                  The only tool with built-in betting tables, appetite budgeting, circuit breakers,
                  and 6-week cycles. No workarounds needed.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <TrendingUp className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">Interactive Hill Charts</h3>
                <p className="text-sm text-muted-foreground">
                  Drag-and-drop hill charts with history tracking. Basecamp has hill charts,
                  but ShipFlow adds real-time collaboration and risk insights.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <Brain className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">AI-Powered Insights</h3>
                <p className="text-sm text-muted-foreground">
                  Pluggable LLM architecture: Choose Ollama (local), OpenAI ChatGPT, or RunPod (cloud GPU).
                  Get automated risk analysis, AI-generated test cases, intelligent pitch
                  document extraction, and Wise Architecture—AI-powered technical solution
                  generation with team skills, Figma design, and roadmap context integration.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <Lock className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">Self-Hosted & Open Source</h3>
                <p className="text-sm text-muted-foreground">
                  Full control over your data. Deploy on your infrastructure with Docker.
                  MIT licensed - no vendor lock-in.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <DollarSign className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">Free Forever</h3>
                <p className="text-sm text-muted-foreground">
                  No per-seat pricing, no premium tiers. All features are free.
                  Save thousands compared to Linear, Asana, or Monday.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <Shield className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">Integrated QA</h3>
                <p className="text-sm text-muted-foreground">
                  Built-in test management, bug tracking, and traceability.
                  No need for separate tools like TestRail or Zephyr.
                </p>
              </CardContent>
            </Card>

            <Card className="border-primary/20 bg-primary/5">
              <CardContent className="p-6">
                <Users className="h-10 w-10 text-primary mb-4" />
                <h3 className="font-semibold text-lg mb-2">RTL Language Support</h3>
                <p className="text-sm text-muted-foreground">
                  Full right-to-left layout support for Farsi, Arabic, and Hebrew.
                  The only Shape Up tool with native RTL capabilities.
                </p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* Detailed Comparison Table */}
      <section className="py-16 md:py-20 bg-muted/30">
        <div className="container mx-auto px-4 max-w-7xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">
              Feature-by-Feature Comparison
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              <Check className="inline h-4 w-4 text-green-500 mr-1" /> Full support
              <Minus className="inline h-4 w-4 text-amber-500 mx-3 mr-1" /> Partial/Limited
              <X className="inline h-4 w-4 text-muted-foreground/50 mx-3 mr-1" /> Not available
            </p>
          </div>

          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/50">
                    <TableHead className="w-[200px] font-semibold">Feature</TableHead>
                    <TableHead className="text-center w-[120px]">
                      <div className="flex flex-col items-center gap-1">
                        <img src="/icon.png" alt="ShipFlow" className="w-6 h-6 rounded" />
                        <span className="text-primary font-bold">ShipFlow</span>
                      </div>
                    </TableHead>
                    <TableHead className="text-center w-[100px]">Linear</TableHead>
                    <TableHead className="text-center w-[100px]">Asana</TableHead>
                    <TableHead className="text-center w-[100px]">Monday</TableHead>
                    <TableHead className="text-center w-[100px]">Jira</TableHead>
                    <TableHead className="text-center w-[100px]">Basecamp</TableHead>
                    <TableHead className="text-center w-[100px]">ClickUp</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {categories.map((category) => (
                    <>
                      <TableRow key={category} className="bg-muted/30">
                        <TableCell colSpan={8} className="font-semibold text-primary py-3">
                          {category}
                        </TableCell>
                      </TableRow>
                      {comparisonFeatures
                        .filter(f => f.category === category)
                        .map((feature, idx) => (
                          <TableRow key={`${category}-${idx}`}>
                            <TableCell className="font-medium">{feature.feature}</TableCell>
                            <TableCell className="text-center bg-primary/5">
                              {renderFeatureCell(feature.shipflow)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.linear)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.asana)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.monday)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.jira)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.basecamp)}
                            </TableCell>
                            <TableCell className="text-center">
                              {renderFeatureCell(feature.clickup)}
                            </TableCell>
                          </TableRow>
                        ))}
                    </>
                  ))}
                </TableBody>
              </Table>
            </div>
          </Card>
        </div>
      </section>

      {/* Competitor Overview Cards */}
      <section className="py-16 md:py-20">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">
              ShipFlow vs Each Competitor
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              A quick summary of how ShipFlow compares to each alternative.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* vs Linear */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-purple-500" />
                  vs Linear
                </CardTitle>
                <CardDescription>Modern issue tracking</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Shape Up methodology, hill charts, AI risk analysis, self-hosted, free</p>
                  <p><strong className="text-amber-500">Linear wins:</strong> Sleeker UI, faster keyboard shortcuts, cycles (Scrum-style)</p>
                  <p><strong>Best for:</strong> Linear for Scrum sprints, ShipFlow for Shape Up cycles</p>
                </div>
              </CardContent>
            </Card>

            {/* vs Asana */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <BarChart3 className="h-5 w-5 text-red-500" />
                  vs Asana
                </CardTitle>
                <CardDescription>Work management platform</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Shape Up support, hill charts, AI features, QA tools, free unlimited users</p>
                  <p><strong className="text-amber-500">Asana wins:</strong> More templates, Gantt charts, larger ecosystem, goals tracking</p>
                  <p><strong>Best for:</strong> Asana for marketing teams, ShipFlow for dev teams using Shape Up</p>
                </div>
              </CardContent>
            </Card>

            {/* vs Monday */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Cloud className="h-5 w-5 text-orange-500" />
                  vs Monday.com
                </CardTitle>
                <CardDescription>Work OS for teams</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Developer-focused, Shape Up, AI test generation, self-hosted, free</p>
                  <p><strong className="text-amber-500">Monday wins:</strong> No-code automations, CRM features, colorful dashboards</p>
                  <p><strong>Best for:</strong> Monday for sales/ops teams, ShipFlow for software development</p>
                </div>
              </CardContent>
            </Card>

            {/* vs Jira */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Zap className="h-5 w-5 text-blue-500" />
                  vs Jira
                </CardTitle>
                <CardDescription>Enterprise project tracking</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Native Shape Up, simpler UX, AI risk analysis, faster setup, truly free</p>
                  <p><strong className="text-amber-500">Jira wins:</strong> Enterprise features, massive plugin ecosystem, Confluence integration</p>
                  <p><strong>Best for:</strong> Jira for enterprise Scrum/SAFe, ShipFlow for Shape Up methodology</p>
                </div>
              </CardContent>
            </Card>

            {/* vs Basecamp */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Users className="h-5 w-5 text-green-500" />
                  vs Basecamp
                </CardTitle>
                <CardDescription>All-in-one project management</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Full Shape Up implementation, AI features, QA tools, free, interactive hill charts</p>
                  <p><strong className="text-amber-500">Basecamp wins:</strong> Message boards, campfire chat, simpler for non-dev teams</p>
                  <p><strong>Best for:</strong> Basecamp invented Shape Up but ShipFlow implements it fully for dev teams</p>
                </div>
              </CardContent>
            </Card>

            {/* vs ClickUp */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Target className="h-5 w-5 text-pink-500" />
                  vs ClickUp
                </CardTitle>
                <CardDescription>Everything app for work</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p><strong className="text-green-500">ShipFlow wins:</strong> Native Shape Up methodology, hill charts, AI test generation, self-hosted, truly free</p>
                  <p><strong className="text-amber-500">ClickUp wins:</strong> More views (List, Board, Calendar, Timeline), docs, whiteboards, extensive customization</p>
                  <p><strong>Best for:</strong> ClickUp for teams wanting everything, ShipFlow for focused Shape Up development</p>
                </div>
              </CardContent>
            </Card>

            {/* Why ShipFlow */}
            <Card className="bg-primary/5 border-primary/20">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Crown className="h-5 w-5 text-primary" />
                  Why ShipFlow?
                </CardTitle>
                <CardDescription>The Shape Up choice</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm">
                  <p>If your team follows <strong>Shape Up methodology</strong>, ShipFlow is the only tool built specifically for it.</p>
                  <ul className="space-y-1 mt-2">
                    <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Betting tables & appetite</li>
                    <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Circuit breaker mechanism</li>
                    <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> AI-powered risk insights</li>
                    <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> 100% free & open source</li>
                  </ul>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 md:py-20 bg-primary text-primary-foreground">
        <div className="container mx-auto px-4 max-w-3xl text-center">
          <h2 className="text-3xl font-bold mb-4">
            {t('competitors.ctaTitle')}
          </h2>
          <p className="text-lg opacity-90 mb-8">
            {t('competitors.ctaSubtitle')}
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Button
              size="lg"
              variant="secondary"
              onClick={() => navigate('/login')}
            >
              {t('competitors.getStarted')}
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="bg-transparent border-white/30 hover:bg-white/10"
              asChild
            >
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Github className="h-5 w-5 mr-2" />
                View Source Code
              </a>
            </Button>
          </div>
          <p className="text-sm mt-6 opacity-70">
            Demo credentials: admin / admin123
          </p>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-6 border-t border-border">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
            <p className="text-sm text-muted-foreground">
              {t('competitors.copyright', { year: new Date().getFullYear() })}
            </p>
            <nav className="flex gap-6">
              <a
                href="/"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                {t('competitors.home')}
              </a>
              <a
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                GitHub
              </a>
              <a
                href="https://basecamp.com/shapeup"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                Shape Up Book
              </a>
            </nav>
          </div>
        </div>
      </footer>
    </div>
  );
}
