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



export default function HelpGuides() {
    const { t } = useTranslation();

    const guides: GuideCard[] = [
        {
            title: t('helpGuides.gettingStarted'),
            description: t('helpGuides.gettingStartedDesc'),
            icon: BookOpen,
            path: '/help/getting-started',
            color: 'text-blue-500',
        },
        {
            title: t('helpGuides.hillCharts'),
            description: t('helpGuides.hillChartsDesc'),
            icon: TrendingUp,
            path: '/help/hill-charts',
            color: 'text-green-500',
        },
        {
            title: t('helpGuides.bettingMeeting'),
            description: t('helpGuides.bettingMeetingDesc'),
            icon: Users,
            path: '/help/betting-meeting',
            color: 'text-purple-500',
        },
        {
            title: t('helpGuides.aiRiskAdvisor'),
            description: t('helpGuides.aiRiskAdvisorDesc'),
            icon: Brain,
            path: '/help/ai-risk-advisor',
            color: 'text-orange-500',
        },
        {
            title: t('helpGuides.cycleSetup'),
            description: t('helpGuides.cycleSetupDesc'),
            icon: Repeat,
            path: '/help/cycle-setup',
            color: 'text-cyan-500',
        },
        {
            title: t('helpGuides.qaTesting'),
            description: t('helpGuides.qaTestingDesc'),
            icon: Beaker,
            path: '/help/qa-testing',
            color: 'text-indigo-500',
        },
        {
            title: t('helpGuides.retrospectives'),
            description: t('helpGuides.retrospectivesDesc'),
            icon: RotateCcw,
            path: '/help/retrospectives',
            color: 'text-pink-500',
        },
        {
            title: t('helpGuides.circuitBreaker'),
            description: t('helpGuides.circuitBreakerDesc'),
            icon: Zap,
            path: '/help/circuit-breaker',
            color: 'text-amber-500',
        },
        {
            title: t('helpGuides.reports'),
            description: t('helpGuides.reportsDesc'),
            icon: BarChart3,
            path: '/help/reports',
            color: 'text-yellow-500',
        },
    ];
    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                    <HelpCircle className="h-6 w-6 text-primary" />
                </div>
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">{t('helpGuides.title')}</h1>
                    <p className="text-muted-foreground">
                        {t('helpGuides.subtitle')}
                    </p>
                </div>
            </div>

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('helpGuides.welcome')}</CardTitle>
                    <CardDescription>
                        {t('helpGuides.welcomeDesc')}
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="rounded-lg bg-muted/50 p-4">
                        <p className="text-sm text-muted-foreground">
                            💡 <strong>{t('helpGuides.tip')}</strong> {t('helpGuides.newToShipFlow')}
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
                                        {t('helpGuides.readGuide')}
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
                    <CardTitle>{t('helpGuides.needMoreHelp')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="flex items-start gap-3">
                        <HelpCircle className="h-5 w-5 text-muted-foreground mt-0.5" />
                        <div>
                            <p className="font-medium">{t('helpGuides.interactiveTour')}</p>
                            <p className="text-sm text-muted-foreground">
                                {t('helpGuides.interactiveTourDesc')}
                            </p>
                        </div>
                    </div>
                    <div className="flex items-start gap-3">
                        <Brain className="h-5 w-5 text-muted-foreground mt-0.5" />
                        <div>
                            <p className="font-medium">{t('helpGuides.aiAssistant')}</p>
                            <p className="text-sm text-muted-foreground">
                                {t('helpGuides.aiAssistantDesc')}
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
