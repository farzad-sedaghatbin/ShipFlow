import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, TrendingUp, Circle, CheckCircle2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function HillChartsGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.hillCharts.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.hillCharts.subtitle')}
                </p>
            </div>

            <Separator />

            {/* What is a Hill Chart */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <TrendingUp className="h-5 w-5" />
                        {t('guides.hillCharts.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.hillCharts.whatDesc')}</p>
                    <p>{t('guides.hillCharts.unlike')}</p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.hillCharts.keyInsight')}</strong> {t('guides.hillCharts.keyInsightDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* The Two Phases */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.hillCharts.twoPhasesTitle')}</CardTitle>
                    <CardDescription>{t('guides.hillCharts.twoPhasesSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    <div className="grid md:grid-cols-2 gap-4">
                        {/* Uphill Phase */}
                        <div className="rounded-lg border-2 border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950 p-4">
                            <div className="flex items-center gap-2 mb-3">
                                <div className="h-8 w-8 rounded-full bg-orange-500 flex items-center justify-center">
                                    <Circle className="h-4 w-4 text-white" />
                                </div>
                                <h3 className="font-semibold text-lg">{t('guides.hillCharts.uphillTitle')}</h3>
                            </div>
                            <ul className="space-y-2 text-sm">
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-orange-600 dark:text-orange-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.uphillItem1')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-orange-600 dark:text-orange-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.uphillItem2')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-orange-600 dark:text-orange-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.uphillItem3')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-orange-600 dark:text-orange-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.uphillItem4')}</span>
                                </li>
                            </ul>
                        </div>

                        {/* Downhill Phase */}
                        <div className="rounded-lg border-2 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 p-4">
                            <div className="flex items-center gap-2 mb-3">
                                <div className="h-8 w-8 rounded-full bg-green-500 flex items-center justify-center">
                                    <CheckCircle2 className="h-4 w-4 text-white" />
                                </div>
                                <h3 className="font-semibold text-lg">{t('guides.hillCharts.downhillTitle')}</h3>
                            </div>
                            <ul className="space-y-2 text-sm">
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.downhillItem1')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.downhillItem2')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.downhillItem3')}</span>
                                </li>
                                <li className="flex items-start gap-2">
                                    <CheckCircle2 className="h-4 w-4 text-green-600 dark:text-green-400 mt-0.5 flex-shrink-0" />
                                    <span>{t('guides.hillCharts.downhillItem4')}</span>
                                </li>
                            </ul>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/hill-chart.png"
                            alt="Hill chart showing items in uphill and downhill phases"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Hill chart visualization
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Reading a Hill Chart */}
            <Card>
                <CardHeader>
                    <CardTitle>How to Read a Hill Chart</CardTitle>
                    <CardDescription>Interpreting positions and movement</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-4">
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <div className="h-6 w-6 rounded-full bg-red-500 flex items-center justify-center flex-shrink-0 mt-1">
                                <span className="text-xs text-white font-bold">1</span>
                            </div>
                            <div>
                                <h4 className="font-semibold">Starting Point (Far Left)</h4>
                                <p className="text-sm text-muted-foreground">
                                    Work just started, lots of unknowns, team is exploring and researching
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <div className="h-6 w-6 rounded-full bg-orange-500 flex items-center justify-center flex-shrink-0 mt-1">
                                <span className="text-xs text-white font-bold">2</span>
                            </div>
                            <div>
                                <h4 className="font-semibold">Climbing Uphill</h4>
                                <p className="text-sm text-muted-foreground">
                                    Making progress on figuring things out, but still facing uncertainties
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <div className="h-6 w-6 rounded-full bg-yellow-500 flex items-center justify-center flex-shrink-0 mt-1">
                                <span className="text-xs text-white font-bold">3</span>
                            </div>
                            <div>
                                <h4 className="font-semibold">Top of the Hill</h4>
                                <p className="text-sm text-muted-foreground">
                                    Critical transition point - unknowns are resolved, ready to execute
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <div className="h-6 w-6 rounded-full bg-green-500 flex items-center justify-center flex-shrink-0 mt-1">
                                <span className="text-xs text-white font-bold">4</span>
                            </div>
                            <div>
                                <h4 className="font-semibold">Going Downhill</h4>
                                <p className="text-sm text-muted-foreground">
                                    Confident execution, clear path to completion, predictable timeline
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <div className="h-6 w-6 rounded-full bg-blue-500 flex items-center justify-center flex-shrink-0 mt-1">
                                <span className="text-xs text-white font-bold">5</span>
                            </div>
                            <div>
                                <h4 className="font-semibold">Finish Line (Far Right)</h4>
                                <p className="text-sm text-muted-foreground">
                                    Work is complete and ready for delivery
                                </p>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Using Hill Charts */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.hillCharts.usingTitle')}</CardTitle>
                    <CardDescription>{t('guides.hillCharts.usingSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">{t('guides.hillCharts.movingTitle')}</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>{t('guides.hillCharts.movingStep1')}</li>
                        <li>{t('guides.hillCharts.movingStep2')}</li>
                        <li>{t('guides.hillCharts.movingStep3')}
                            <ul className="list-disc list-inside ml-6 mt-1 space-y-1 text-sm text-muted-foreground">
                                <li>{t('guides.hillCharts.movingStep3a')}</li>
                                <li>{t('guides.hillCharts.movingStep3b')}</li>
                            </ul>
                        </li>
                        <li>{t('guides.hillCharts.movingStep4')}</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/hill-chart-interactive.png"
                            alt="Interactive hill chart showing drag and drop functionality"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Interactive hill chart
                        </div>
                    </div>

                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4 mt-4">
                        <h4 className="font-semibold text-amber-900 dark:text-amber-100 mb-2">{t('guides.hillCharts.warningTitle')}</h4>
                        <ul className="space-y-1 text-sm text-amber-900 dark:text-amber-100">
                            <li>• <span dangerouslySetInnerHTML={{ __html: t('guides.hillCharts.warnStuckUphill') }} /></li>
                            <li>• <span dangerouslySetInnerHTML={{ __html: t('guides.hillCharts.warnMovingBackward') }} /></li>
                            <li>• <span dangerouslySetInnerHTML={{ __html: t('guides.hillCharts.warnTooManyUphill') }} /></li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* Best Practices */}
            <Card>
                <CardHeader>
                    <CardTitle>Best Practices</CardTitle>
                </CardHeader>
                <CardContent>
                    <ul className="space-y-3">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Update regularly:</strong> Move items as your understanding changes, ideally daily or every few days
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Be honest:</strong> Don't move items downhill prematurely - it masks real risks
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Use in standups:</strong> Review the hill chart with your team to identify blockers
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Track movement:</strong> Items that don't move indicate problems worth discussing
                            </div>
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Next Steps */}
            <Card className="bg-gradient-to-br from-primary/5 to-primary/10 border-primary/20">
                <CardHeader>
                    <CardTitle>Related Guides</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <Button asChild variant="outline" className="w-full justify-start">
                        <Link to="/help/cycle-setup">
                            📅 Setting Up Your First Cycle
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-start">
                        <Link to="/help/betting-meeting">
                            🎲 Running a Betting Meeting
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
