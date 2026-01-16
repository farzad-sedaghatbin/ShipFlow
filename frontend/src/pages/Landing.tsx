import { useNavigate, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LogIn,
  TrendingUp,
  BarChart3,
  Users,
  Brain,
  Target,
  CheckCircle,
  Github,
  Accessibility,
  RotateCcw,
  ArrowRight,
} from 'lucide-react';
import { useAuth } from '../contexts';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Separator } from '../components/ui/separator';

const features = [
  {
    icon: <TrendingUp className="h-10 w-10" />,
    title: 'Cycle Management',
    description: 'Create and manage 6-week development cycles with betting and building phases following Shape Up methodology.',
  },
  {
    icon: <BarChart3 className="h-10 w-10" />,
    title: 'Hill Charts',
    description: 'Visual progress tracking showing work moving from "figuring it out" to "making it happen" with drag-and-drop.',
  },
  {
    icon: <Brain className="h-10 w-10" />,
    title: 'AI Risk Analysis',
    description: 'Get AI-powered risk assessments with actionable insights and smart recommendations for your pitches.',
  },
  {
    icon: <Target className="h-10 w-10" />,
    title: 'Pitch Health Summary',
    description: 'Lightweight dashboard with risk-colored cards, budget progress, and QA status at a glance.',
  },
  {
    icon: <Users className="h-10 w-10" />,
    title: 'Team Management',
    description: 'Organize teams, assign tasks, track time, and collaborate with pair programming support.',
  },
  {
    icon: <RotateCcw className="h-10 w-10" />,
    title: 'Retrospectives',
    description: "Run team retros with voting and merging. Cycles can't close until retrospectives are complete.",
  },
  {
    icon: <CheckCircle className="h-10 w-10" />,
    title: 'AI-Powered Test & QA',
    description: 'Generate test cases with AI, execute test suites, track coverage, and maintain quality with integrated bug reporting.',
  },
  {
    icon: <Accessibility className="h-10 w-10" />,
    title: 'WCAG 2.1 AA Accessible',
    description: 'Full keyboard navigation, screen reader support, high contrast, and focus indicators for all users.',
  },
];

const techStack = [
  { name: 'React 18', color: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20' },
  { name: 'TypeScript', color: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  { name: 'Spring Boot 3', color: 'bg-green-500/10 text-green-400 border-green-500/20' },
  { name: 'Java 17+', color: 'bg-orange-500/10 text-orange-400 border-orange-500/20' },
  { name: 'shadcn/ui', color: 'bg-purple-500/10 text-purple-400 border-purple-500/20' },
  { name: 'LangChain4j', color: 'bg-violet-500/10 text-violet-400 border-violet-500/20' },
];

export default function Landing() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isAuthenticated, isLoading } = useAuth();
  // i18n ready
  if (false) console.log(t('landing.title'));

  // Redirect authenticated users to dashboard
  if (!isLoading && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Hero Section */}
      <section className="relative py-16 md:py-24 bg-gradient-to-br from-primary/5 via-background to-secondary/5">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="grid md:grid-cols-2 gap-8 items-center">
            <div>
              {/* Logo & Title */}
              <div className="flex items-center gap-3 mb-6">
                <img src="/icon.png" alt="ShipFlow" className="w-14 h-14 rounded-xl" />
                <h1 className="text-3xl font-bold text-primary">ShipFlow</h1>
              </div>
              
              <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
                Shape Up Your Software Development
              </h2>
              
              <p className="text-lg text-muted-foreground mb-8 leading-relaxed">
                A full-stack application for tracking development cycles using the{' '}
                <strong className="text-foreground">Shape Up methodology</strong> by Basecamp.
                Features AI-powered risk analysis, interactive hill charts, and comprehensive
                team collaboration tools.
              </p>

              {/* CTA Buttons */}
              <div className="flex flex-wrap gap-3 mb-8">
                <Button size="lg" onClick={() => navigate('/login')}>
                  <LogIn className="h-5 w-5 mr-2" />
                  Get Started
                </Button>
                <Button 
                  variant="outline" 
                  size="lg"
                  asChild
                >
                  <a 
                    href="https://github.com/farzad-sedaghatbin/ShipFlow" 
                    target="_blank" 
                    rel="noopener noreferrer"
                  >
                    <Github className="h-5 w-5 mr-2" />
                    View on GitHub
                  </a>
                </Button>
                <Button 
                  variant="ghost" 
                  size="lg"
                  onClick={() => navigate('/compare')}
                >
                  <ArrowRight className="h-5 w-5 mr-2" />
                  Compare to Competitors
                </Button>
              </div>

              {/* Tech Stack */}
              <div className="flex flex-wrap gap-2">
                {techStack.map((tech) => (
                  <Badge 
                    key={tech.name} 
                    variant="outline" 
                    className={`${tech.color} font-medium`}
                  >
                    {tech.name}
                  </Badge>
                ))}
              </div>
            </div>

            {/* Preview Card */}
            <div className="hidden md:block">
              <Card className="shadow-2xl">
                <CardContent className="p-6">
                  <p className="text-sm text-muted-foreground mb-4">Hill Chart Preview</p>
                  <svg viewBox="0 0 400 150" className="w-full">
                    {/* Hill curve */}
                    <path
                      d="M 0 150 Q 100 150 200 30 Q 300 150 400 150"
                      fill="none"
                      stroke="currentColor"
                      className="text-primary/30"
                      strokeWidth="3"
                    />
                    {/* Center line */}
                    <line
                      x1="200"
                      y1="0"
                      x2="200"
                      y2="150"
                      stroke="currentColor"
                      className="text-border"
                      strokeDasharray="5,5"
                    />
                    {/* Sample points */}
                    <circle cx="80" cy="120" r="10" className="fill-amber-500" />
                    <circle cx="150" cy="60" r="10" className="fill-blue-500" />
                    <circle cx="280" cy="80" r="10" className="fill-green-500" />
                    <circle cx="350" cy="130" r="10" className="fill-green-500" />
                    {/* Labels */}
                    <text x="60" y="145" fontSize="10" className="fill-muted-foreground">
                      Figuring it out
                    </text>
                    <text x="280" y="145" fontSize="10" className="fill-muted-foreground">
                      Making it happen
                    </text>
                  </svg>
                  
                  <Separator className="my-4" />
                  
                  <div className="space-y-2">
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full bg-green-500" />
                      <span className="text-sm">2 tasks completed</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                      <span className="text-sm">1 task in progress</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full bg-amber-500" />
                      <span className="text-sm">1 task in discovery</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 md:py-24">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">
              Everything You Need for Shape Up
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              From cycle planning to risk analysis, ShipFlow provides all the tools
              your team needs to implement Shape Up successfully.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => (
              <Card 
                key={index} 
                className="h-full hover:-translate-y-1 hover:shadow-lg transition-all"
              >
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

      {/* How it Works */}
      <section className="py-16 md:py-24 bg-muted/30">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-foreground mb-4">
              How Shape Up Works
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              Shape Up is a product development methodology created by Basecamp that gives teams
              more autonomy and focuses on shipping meaningful work.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {[
              {
                step: '1',
                title: 'Shape',
                description: 'Senior people work on shaping pitches - defining the problem, setting boundaries, and outlining a solution.',
              },
              {
                step: '2',
                title: 'Bet',
                description: 'Leadership reviews shaped pitches at the betting table and decides which ones to commit to.',
              },
              {
                step: '3',
                title: 'Build',
                description: 'Small teams work autonomously to build and ship within the fixed time appetite.',
              },
              {
                step: '4',
                title: 'Cooldown',
                description: 'Two weeks for bug fixes, technical debt, and exploring new ideas before the next cycle.',
              },
            ].map((item) => (
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
                  <h2 className="text-3xl font-bold text-foreground mb-4">
                    Open Source & Self-Hosted
                  </h2>
                  <p className="text-muted-foreground mb-6">
                    ShipFlow is completely open source under the MIT license. Deploy it on your
                    own infrastructure and maintain full control over your data. Contributions welcome!
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {['MIT License', 'Self-Hosted', 'Docker Ready', 'WCAG 2.1 AA', 'Active Development'].map((label) => (
                      <Badge key={label} variant="outline" className="bg-green-500/10 text-green-400 border-green-500/20">
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
                      Star on GitHub
                    </a>
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 md:py-20 bg-primary text-primary-foreground">
        <div className="container mx-auto px-4 max-w-3xl text-center">
          <h2 className="text-3xl font-bold mb-4">
            Ready to Shape Up Your Projects?
          </h2>
          <p className="text-lg opacity-90 mb-8">
            Start tracking your development cycles with AI-powered insights today.
          </p>
          <Button 
            size="lg" 
            variant="secondary"
            onClick={() => navigate('/login')}
          >
            Get Started - It's Free
          </Button>
          <p className="text-sm mt-4 opacity-70">
            Demo: admin / admin123
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
                href="https://github.com/farzad-sedaghatbin/ShipFlow"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                GitHub
              </a>
              <a 
                href="https://github.com/farzad-sedaghatbin/ShipFlow/blob/main/CONTRIBUTING.md"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-muted-foreground hover:text-primary transition-colors"
              >
                Contributing
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
