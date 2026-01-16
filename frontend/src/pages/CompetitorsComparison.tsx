import { useNavigate } from 'react-router-dom';
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

interface ComparisonFeature {
  category: string;
  feature: string;
  shipflow: boolean | 'partial' | 'coming';
  linear: boolean | 'partial';
  asana: boolean | 'partial';
  monday: boolean | 'partial';
  jira: boolean | 'partial';
  basecamp: boolean | 'partial';
}

const comparisonFeatures: ComparisonFeature[] = [
  // Shape Up Methodology
  { category: 'Shape Up', feature: 'Native Shape Up methodology', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true },
  { category: 'Shape Up', feature: '6-week fixed cycles', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true },
  { category: 'Shape Up', feature: 'Betting table for pitches', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'Shape Up', feature: 'Appetite-based budgeting', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true },
  { category: 'Shape Up', feature: 'Circuit breaker mechanism', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'Shape Up', feature: 'Pitch shaping workflow', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: 'partial' },
  { category: 'Shape Up', feature: 'Cooldown periods', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true },
  
  // Progress Visualization
  { category: 'Progress', feature: 'Hill charts', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: true },
  { category: 'Progress', feature: 'Interactive hill chart editing', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'Progress', feature: 'Gantt charts', shipflow: false, linear: false, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'Progress', feature: 'Kanban boards', shipflow: false, linear: true, asana: true, monday: true, jira: true, basecamp: true },
  { category: 'Progress', feature: 'Sprint burndown', shipflow: false, linear: true, asana: 'partial', monday: 'partial', jira: true, basecamp: false },
  
  // AI & Intelligence
  { category: 'AI Features', feature: 'AI risk analysis', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false },
  { category: 'AI Features', feature: 'AI test case generation', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'AI Features', feature: 'AI pitch document extraction', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'AI Features', feature: 'Configurable risk weights', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'AI Features', feature: 'Risk trend prediction', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  
  // QA & Testing
  { category: 'QA', feature: 'Integrated test management', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false },
  { category: 'QA', feature: 'Bug tracking', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'QA', feature: 'Test execution & runs', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false },
  { category: 'QA', feature: 'Traceability links', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false },
  
  // Team & Collaboration
  { category: 'Team', feature: 'Retrospectives', shipflow: true, linear: false, asana: false, monday: false, jira: 'partial', basecamp: false },
  { category: 'Team', feature: 'Anonymous retro submissions', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'Team', feature: 'Time tracking', shipflow: true, linear: false, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'Team', feature: 'Work log timers', shipflow: true, linear: false, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'Team', feature: 'Task dependencies', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false },
  
  // Integrations
  { category: 'Integrations', feature: 'GitHub integration', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'Integrations', feature: 'Slack notifications', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true },
  { category: 'Integrations', feature: 'Microsoft Teams', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false },
  { category: 'Integrations', feature: 'Webhooks', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true },
  
  // Deployment & Pricing
  { category: 'Deployment', feature: 'Self-hosted option', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false },
  { category: 'Deployment', feature: 'Open source', shipflow: true, linear: false, asana: false, monday: false, jira: false, basecamp: false },
  { category: 'Deployment', feature: 'Docker ready', shipflow: true, linear: false, asana: false, monday: false, jira: true, basecamp: false },
  { category: 'Deployment', feature: 'Free tier', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: false },
  
  // Accessibility
  { category: 'Accessibility', feature: 'WCAG 2.1 AA compliant', shipflow: true, linear: 'partial', asana: true, monday: 'partial', jira: true, basecamp: 'partial' },
  { category: 'Accessibility', feature: 'Keyboard navigation', shipflow: true, linear: true, asana: true, monday: true, jira: true, basecamp: true },
  { category: 'Accessibility', feature: 'Screen reader support', shipflow: true, linear: 'partial', asana: true, monday: 'partial', jira: true, basecamp: 'partial' },
];

const competitors = [
  {
    name: 'Linear',
    description: 'Modern issue tracking for high-performance teams',
    pricing: 'From $8/user/mo',
    bestFor: 'Fast-moving startups',
    color: 'purple',
  },
  {
    name: 'Asana',
    description: 'Work management platform for teams',
    pricing: 'From $10.99/user/mo',
    bestFor: 'Marketing & creative teams',
    color: 'red',
  },
  {
    name: 'Monday.com',
    description: 'Work OS for teams of all sizes',
    pricing: 'From $9/user/mo',
    bestFor: 'Non-technical teams',
    color: 'orange',
  },
  {
    name: 'Jira',
    description: 'Project tracking for software teams',
    pricing: 'From $7.75/user/mo',
    bestFor: 'Enterprise Agile/Scrum',
    color: 'blue',
  },
  {
    name: 'Basecamp',
    description: 'All-in-one project management',
    pricing: '$299/mo flat',
    bestFor: 'Small agencies',
    color: 'green',
  },
];

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
  const navigate = useNavigate();

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
                Back to Home
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
            How ShipFlow Compares
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto mb-8">
            ShipFlow is the only project management tool built specifically for the Shape Up methodology.
            See how we compare to popular alternatives.
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            {competitors.map((comp) => (
              <Badge key={comp.name} variant="outline" className="text-sm">
                vs {comp.name}
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
              Why Choose ShipFlow?
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
                  Get automated risk analysis, AI-generated test cases, and intelligent pitch
                  document extraction. No other Shape Up tool offers this.
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
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {categories.map((category) => (
                    <>
                      <TableRow key={category} className="bg-muted/30">
                        <TableCell colSpan={7} className="font-semibold text-primary py-3">
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
            Ready to Try Shape Up with ShipFlow?
          </h2>
          <p className="text-lg opacity-90 mb-8">
            Start for free. No credit card required. Deploy on your infrastructure or use our demo.
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Button 
              size="lg" 
              variant="secondary"
              onClick={() => navigate('/login')}
            >
              Get Started Free
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
              © {new Date().getFullYear()} ShipFlow. Open source under MIT License.
            </p>
            <nav className="flex gap-6">
              <a 
                href="/"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                Home
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
