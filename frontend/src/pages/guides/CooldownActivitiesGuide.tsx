import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Coffee, Bug, Wrench, Lightbulb, FlaskConical, GraduationCap, FileText, Repeat, Users, BookOpen } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { MarkdownInline } from '@/components/ui/markdown';

export default function CooldownActivitiesGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.cooldownActivities.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.cooldownActivities.subtitle')}
                </p>
            </div>

            <Separator />

            {/* What is Cooldown Period? */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Coffee className="h-5 w-5" />
                        {t('guides.cooldownActivities.whatIs')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.cooldownActivities.whatIsDesc1')} /></p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.cooldownActivities.shapeUpQuote')}</strong> {t('guides.cooldownActivities.shapeUpQuoteText')}
                        </p>
                    </div>
                    <p><MarkdownInline content={t('guides.cooldownActivities.whatIsDesc2')} /></p>
                </CardContent>
            </Card>

            {/* Cooldown Activities vs Backlog Tasks */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.cooldownActivities.vsBacklog')}</CardTitle>
                    <CardDescription>{t('guides.cooldownActivities.vsBacklogDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid md:grid-cols-2 gap-4">
                        {/* Backlog Tasks */}
                        <div className="space-y-3 p-4 rounded-lg border bg-card">
                            <div className="flex items-center gap-2">
                                <Badge variant="default">{t('guides.cooldownActivities.backlogTasks')}</Badge>
                            </div>
                            <ul className="space-y-2 text-sm">
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.backlogPoint1')} />
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.backlogPoint2')} />
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <span>{t('guides.cooldownActivities.backlogPoint3')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.backlogPoint4')} />
                                </li>
                            </ul>
                        </div>

                        {/* Cooldown Activities */}
                        <div className="space-y-3 p-4 rounded-lg border bg-card">
                            <div className="flex items-center gap-2">
                                <Badge variant="secondary">{t('guides.cooldownActivities.cooldownActivities')}</Badge>
                            </div>
                            <ul className="space-y-2 text-sm">
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.cooldownPoint1')} />
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.cooldownPoint2')} />
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <span>{t('guides.cooldownActivities.cooldownPoint3')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <span className="text-primary">•</span>
                                    <MarkdownInline content={t('guides.cooldownActivities.cooldownPoint4')} />
                                </li>
                            </ul>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Types of Cooldown Activities */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.cooldownActivities.typesTitle')}</CardTitle>
                    <CardDescription>{t('guides.cooldownActivities.typesDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid gap-4">
                        {/* Bug Fixes */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <Bug className="h-5 w-5 text-red-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.bug_fix')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.bugFixDesc')}</p>
                            </div>
                        </div>

                        {/* Tech Debt */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <Wrench className="h-5 w-5 text-orange-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.tech_debt')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.techDebtDesc')}</p>
                            </div>
                        </div>

                        {/* Research */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <Lightbulb className="h-5 w-5 text-yellow-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.research')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.researchDesc')}</p>
                            </div>
                        </div>

                        {/* Experiments */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <FlaskConical className="h-5 w-5 text-purple-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.experiment')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.experimentDesc')}</p>
                            </div>
                        </div>

                        {/* Training */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <GraduationCap className="h-5 w-5 text-blue-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.training')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.trainingDesc')}</p>
                            </div>
                        </div>

                        {/* Documentation */}
                        <div className="flex items-start gap-3 p-3 rounded-lg border bg-card">
                            <FileText className="h-5 w-5 text-green-500 mt-0.5" />
                            <div className="space-y-1">
                                <h4 className="font-semibold">{t('cooldownActivity.types.documentation')}</h4>
                                <p className="text-sm text-muted-foreground">{t('guides.cooldownActivities.documentationDesc')}</p>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* How to Access */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.cooldownActivities.accessTitle')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="space-y-3 list-decimal list-inside">
                        <li><MarkdownInline content={t('guides.cooldownActivities.accessStep1')} /></li>
                        <li><MarkdownInline content={t('guides.cooldownActivities.accessStep2')} /></li>
                        <li>{t('guides.cooldownActivities.accessStep3')}</li>
                    </ol>
                </CardContent>
            </Card>

            {/* Best Practices */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.cooldownActivities.bestPractices')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ul className="space-y-2">
                        <li className="flex items-start gap-2">
                            <span className="text-green-500 font-bold">✓</span>
                            <MarkdownInline content={t('guides.cooldownActivities.practice1')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-green-500 font-bold">✓</span>
                            <MarkdownInline content={t('guides.cooldownActivities.practice2')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-green-500 font-bold">✓</span>
                            <MarkdownInline content={t('guides.cooldownActivities.practice3')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-green-500 font-bold">✓</span>
                            <MarkdownInline content={t('guides.cooldownActivities.practice4')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="text-green-500 font-bold">✓</span>
                            <MarkdownInline content={t('guides.cooldownActivities.practice5')} />
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Related Guides */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.relatedGuides')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-2">
                        <Button asChild variant="outline" className="w-full justify-start">
                            <Link to="/help/cycle-setup">
                                <Repeat className="h-4 w-4 me-2" />
                                {t('helpGuides.cycleSetup')}
                            </Link>
                        </Button>
                        <Button asChild variant="outline" className="w-full justify-start">
                            <Link to="/help/betting-meeting">
                                <Users className="h-4 w-4 me-2" />
                                {t('helpGuides.bettingMeeting')}
                            </Link>
                        </Button>
                        <Button asChild variant="outline" className="w-full justify-start">
                            <Link to="/help/getting-started">
                                <BookOpen className="h-4 w-4 me-2" />
                                {t('helpGuides.gettingStarted')}
                            </Link>
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
