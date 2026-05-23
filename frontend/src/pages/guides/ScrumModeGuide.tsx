import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft,
    Workflow,
    Target,
    TrendingDown,
    Gauge,
    BarChart3,
    CheckCircle2,
    AlertCircle,
    ArrowRight,
    Calendar,
    Users,
    Layers,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function ScrumModeGuide() {
    const { t } = useTranslation();

    return (
        <div className="w-full max-w-none space-y-6">
            {/* Back Navigation */}
            <Button asChild variant="ghost" size="sm">
                <Link to="/help" className="gap-2">
                    <ArrowLeft className="h-4 w-4" />
                    {t('guides.backToHelp')}
                </Link>
            </Button>

            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.scrumMode.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.scrumMode.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Scrum Mode Only Notice */}
            <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border-2 border-blue-200 dark:border-blue-800 p-4">
                <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-blue-900 dark:text-blue-100">
                            {t('guides.scrumMode.scrumOnlyNoticeTitle')}
                        </p>
                        <p className="text-sm text-blue-800 dark:text-blue-200">
                            {t('guides.scrumMode.scrumOnlyNoticeDesc')}
                            {' '}
                            <Link to="/help/project-types" className="underline font-semibold">
                                {t('guides.scrumMode.scrumOnlyNoticeLink')}
                            </Link>
                        </p>
                    </div>
                </div>
            </div>

            {/* What is Scrum Mode */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Workflow className="h-5 w-5" />
                        {t('guides.scrumMode.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.scrumMode.whatDesc')}</p>
                    <div className="grid gap-3 md:grid-cols-2">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.conceptSprintsTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.conceptSprintsDesc')}</p>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.conceptVelocityTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.conceptVelocityDesc')}</p>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.conceptBacklogTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.conceptBacklogDesc')}</p>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.conceptStoryPointsTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.conceptStoryPointsDesc')}</p>
                        </div>
                    </div>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.scrumMode.whatNote')}</strong> {t('guides.scrumMode.whatNoteDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 1: Creating a Scrum Project */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Layers className="h-5 w-5" />
                        {t('guides.scrumMode.step1Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.scrumMode.step1Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li><MarkdownInline content={t('guides.scrumMode.step1Item1')} /></li>
                        <li><MarkdownInline content={t('guides.scrumMode.step1Item2')} /></li>
                        <li>{t('guides.scrumMode.step1Item3')}
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li>{t('guides.scrumMode.step1Item3a')}</li>
                                <li>{t('guides.scrumMode.step1Item3b')}</li>
                                <li>{t('guides.scrumMode.step1Item3c')}</li>
                            </ul>
                        </li>
                        <li><MarkdownInline content={t('guides.scrumMode.step1Item4')} /></li>
                    </ol>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.scrumMode.step1Tip')}</strong> {t('guides.scrumMode.step1TipDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 2: Sprint Planning */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Calendar className="h-5 w-5" />
                        {t('guides.scrumMode.step2Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.scrumMode.step2Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step2Desc')}</p>
                    <ol className="list-decimal list-inside space-y-3">
                        <li><MarkdownInline content={t('guides.scrumMode.step2Item1')} /></li>
                        <li><MarkdownInline content={t('guides.scrumMode.step2Item2')} /></li>
                        <li><MarkdownInline content={t('guides.scrumMode.step2Item3')} /></li>
                        <li><MarkdownInline content={t('guides.scrumMode.step2Item4')} /></li>
                        <li><MarkdownInline content={t('guides.scrumMode.step2Item5')} /></li>
                    </ol>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.scrumMode.step2Tip')}</strong> {t('guides.scrumMode.step2TipDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 3: Running the Sprint */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Target className="h-5 w-5" />
                        {t('guides.scrumMode.step3Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.scrumMode.step3Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step3Desc')}</p>
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.step3StoryPointsTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step3StoryPointsDesc')}</p>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.scrumMode.step3BoardTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step3BoardDesc')}</p>
                            <ul className="space-y-1 text-sm mt-2 ml-2">
                                <li>• {t('guides.scrumMode.step3BoardItem1')}</li>
                                <li>• {t('guides.scrumMode.step3BoardItem2')}</li>
                                <li>• {t('guides.scrumMode.step3BoardItem3')}</li>
                            </ul>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <TrendingDown className="h-4 w-4 text-blue-500" />
                                {t('guides.scrumMode.step3BurndownTitle')}
                            </h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step3BurndownDesc')}</p>
                        </div>
                    </div>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm font-medium text-amber-900 dark:text-amber-100 mb-1">
                            {t('guides.scrumMode.step3WarnTitle')}
                        </p>
                        <ul className="space-y-1 text-sm text-amber-800 dark:text-amber-200">
                            <li>• {t('guides.scrumMode.step3WarnItem1')}</li>
                            <li>• {t('guides.scrumMode.step3WarnItem2')}</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* Step 4: Sprint Review & Velocity */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Gauge className="h-5 w-5" />
                        {t('guides.scrumMode.step4Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.scrumMode.step4Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step4Desc')}</p>
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <BarChart3 className="h-4 w-4 text-green-500" />
                                {t('guides.scrumMode.step4VelocityTitle')}
                            </h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step4VelocityDesc')}</p>
                            <ol className="list-decimal list-inside space-y-1 text-sm mt-2 ml-2">
                                <li>{t('guides.scrumMode.step4VelocityStep1')}</li>
                                <li>{t('guides.scrumMode.step4VelocityStep2')}</li>
                                <li>{t('guides.scrumMode.step4VelocityStep3')}</li>
                            </ol>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <Users className="h-4 w-4 text-purple-500" />
                                {t('guides.scrumMode.step4RetroTitle')}
                            </h4>
                            <p className="text-sm text-muted-foreground">{t('guides.scrumMode.step4RetroDesc')}</p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Best Practices */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.scrumMode.bestPracticesTitle')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <ul className="space-y-3">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.scrumMode.bp1Title')}</strong>{' '}
                                {t('guides.scrumMode.bp1Desc')}
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.scrumMode.bp2Title')}</strong>{' '}
                                {t('guides.scrumMode.bp2Desc')}
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.scrumMode.bp3Title')}</strong>{' '}
                                {t('guides.scrumMode.bp3Desc')}
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.scrumMode.bp4Title')}</strong>{' '}
                                {t('guides.scrumMode.bp4Desc')}
                            </div>
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Related Guides */}
            <Card className="bg-gradient-to-br from-primary/5 to-primary/10 border-primary/20">
                <CardHeader>
                    <CardTitle>{t('guides.relatedGuides')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/cycle-setup">
                            {t('helpGuides.cycleSetup')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/project-types">
                            {t('helpGuides.projectTypes')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/import">
                            {t('helpGuides.importData')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
