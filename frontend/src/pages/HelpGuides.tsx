import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    BookOpen,
    TrendingUp,
    Users,
    Brain,
    Repeat,
    ArrowRight,
    HelpCircle,
    Beaker,
    RotateCcw,
    BarChart3,
    Zap
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface GuideCard {
    title: string;
    description: string;
    icon: React.ElementType;
    path: string;
    color: string;
}

const guides: GuideCard[] = [
    {
        title: 'Getting Started with ShipFlow',
        description: 'Learn the basics of ShipFlow, from logging in to navigating the interface and understanding core concepts.',
        icon: BookOpen,
        path: '/help/getting-started',
        color: 'text-blue-500',
    },
    {
        title: 'Understanding Hill Charts',
        description: 'Master the hill chart visualization to track progress through the figuring-out and making-it-happen phases.',
        icon: TrendingUp,
        path: '/help/hill-charts',
        color: 'text-green-500',
    },
    {
        title: 'Running a Betting Meeting',
        description: 'Step-by-step guide to conducting effective betting meetings and making informed cycle commitments.',
        icon: Users,
        path: '/help/betting-meeting',
        color: 'text-purple-500',
    },
    {
        title: 'Using AI Risk Advisor',
        description: 'Leverage AI-powered risk assessments to identify potential issues and improve your planning.',
        icon: Brain,
        path: '/help/ai-risk-advisor',
        color: 'text-orange-500',
    },
    {
        title: 'Setting Up Your First Cycle',
        description: 'Complete walkthrough of creating a cycle, adding pitches, and managing your team\'s work.',
        icon: Repeat,
        path: '/help/cycle-setup',
        color: 'text-cyan-500',
    },
    {
        title: 'QA & Testing',
        description: 'Learn how to manage test cases, generate tests with AI, and track bugs.',
        icon: Beaker,
        path: '/help/qa-testing',
        color: 'text-indigo-500',
    },
    {
        title: 'Retrospectives',
        description: 'Run effective retrospectives to capture learnings and improve team processes.',
        icon: RotateCcw,
        path: '/help/retrospectives',
        color: 'text-pink-500',
    },
    {
        title: 'Circuit Breaker',
        description: 'Detect and respond to scope overflow using Shape Up\'s fixed-time safety valve.',
        icon: Zap,
        path: '/help/circuit-breaker',
        color: 'text-amber-500',
    },
    {
        title: 'Reports & Dashboards',
        description: 'Visualize metrics, track velocity, and create custom reporting dashboards.',
        icon: BarChart3,
        path: '/help/reports',
        color: 'text-yellow-500',
    },
];

export default function HelpGuides() {
    const { t } = useTranslation();
    // i18n ready
    if (false) console.log(t('helpGuides.title'));
    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                    <HelpCircle className="h-6 w-6 text-primary" />
                </div>
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Help & Guides</h1>
                    <p className="text-muted-foreground">
                        Learn how to make the most of ShipFlow with our comprehensive guides
                    </p>
                </div>
            </div>

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle>Welcome to ShipFlow Documentation</CardTitle>
                    <CardDescription>
                        ShipFlow helps teams implement the Shape Up methodology for product development.
                        These guides will help you understand and effectively use all features of the platform.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="rounded-lg bg-muted/50 p-4">
                        <p className="text-sm text-muted-foreground">
                            💡 <strong>Tip:</strong> New to ShipFlow? Start with "Getting Started with ShipFlow"
                            to learn the fundamentals, then explore other guides based on your needs.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Guide Cards */}
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {guides.map((guide) => {
                    const Icon = guide.icon;
                    return (
                        <Card key={guide.path} className="group hover:shadow-lg transition-shadow">
                            <CardHeader>
                                <div className="flex items-start justify-between">
                                    <div className={`flex h-10 w-10 items-center justify-center rounded-lg bg-muted ${guide.color}`}>
                                        <Icon className="h-5 w-5" />
                                    </div>
                                </div>
                                <CardTitle className="mt-4">{guide.title}</CardTitle>
                                <CardDescription>{guide.description}</CardDescription>
                            </CardHeader>
                            <CardContent>
                                <Button asChild variant="ghost" className="w-full justify-between group-hover:bg-accent">
                                    <Link to={guide.path}>
                                        Read Guide
                                        <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                                    </Link>
                                </Button>
                            </CardContent>
                        </Card>
                    );
                })}
            </div>

            {/* Additional Resources */}
            <Card>
                <CardHeader>
                    <CardTitle>Need More Help?</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="flex items-start gap-3">
                        <HelpCircle className="h-5 w-5 text-muted-foreground mt-0.5" />
                        <div>
                            <p className="font-medium">Interactive Tour</p>
                            <p className="text-sm text-muted-foreground">
                                Click the help icon in the header to start an interactive tour of the application.
                            </p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <Brain className="h-5 w-5 text-muted-foreground mt-0.5" />
                        <div>
                            <p className="font-medium">AI Assistant</p>
                            <p className="text-sm text-muted-foreground">
                                Use the floating Q&A button on any page to ask questions about your cycles and projects.
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
