import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Brain, AlertTriangle, CheckCircle2, ThumbsUp, ThumbsDown } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function AIRiskAdvisorGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.aiRiskAdvisor.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.aiRiskAdvisor.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Brain className="h-5 w-5" />
                        {t('guides.aiRiskAdvisor.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.aiRiskAdvisor.whatDesc')} /></p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.aiRiskAdvisor.howWorks')}</strong> {t('guides.aiRiskAdvisor.howWorksDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Where to Find It */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.aiRiskAdvisor.whereTitle')}</CardTitle>
                    <CardDescription>{t('guides.aiRiskAdvisor.whereSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.aiRiskAdvisor.pitchDetail')}</h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.aiRiskAdvisor.pitchDetailDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.aiRiskAdvisor.cycleOverview')}</h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.aiRiskAdvisor.cycleOverviewDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.aiRiskAdvisor.bettingTable')}</h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.aiRiskAdvisor.bettingTableDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.aiRiskAdvisor.qaPanel')}</h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.aiRiskAdvisor.qaPanelDesc')}
                            </p>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/ai-risk-advisor.png"
                            alt={t('guides.aiRiskAdvisor.screenshotAlt')}
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            {t('guides.aiRiskAdvisor.screenshotPlaceholder')}
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Understanding Risk Scores */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.aiRiskAdvisor.scoresTitle')}</CardTitle>
                    <CardDescription>{t('guides.aiRiskAdvisor.scoresSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground mb-4">
                        {t('guides.aiRiskAdvisor.scoresIntro')}
                    </p>

                    <div className="space-y-3">
                        <div className="rounded-lg border-2 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                                <h5 className="font-semibold text-green-900 dark:text-green-100">{t('guides.aiRiskAdvisor.lowRisk')}</h5>
                            </div>
                            <p className="text-sm text-green-800 dark:text-green-200">
                                {t('guides.aiRiskAdvisor.lowRiskDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                                <h5 className="font-semibold text-yellow-900 dark:text-yellow-100">{t('guides.aiRiskAdvisor.mediumRisk')}</h5>
                            </div>
                            <p className="text-sm text-yellow-800 dark:text-yellow-200">
                                {t('guides.aiRiskAdvisor.mediumRiskDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
                                <h5 className="font-semibold text-red-900 dark:text-red-100">{t('guides.aiRiskAdvisor.highRisk')}</h5>
                            </div>
                            <p className="text-sm text-red-800 dark:text-red-200">
                                {t('guides.aiRiskAdvisor.highRiskDesc')}
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Common Risk Types */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.aiRiskAdvisor.typesTitle')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-3">
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">{t('guides.aiRiskAdvisor.scopeRisk')}</h4>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.aiRiskAdvisor.scopeRiskDesc')}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">{t('guides.aiRiskAdvisor.complexityRisk')}</h4>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.aiRiskAdvisor.complexityRiskDesc')}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">{t('guides.aiRiskAdvisor.dependencyRisk')}</h4>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.aiRiskAdvisor.dependencyRiskDesc')}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">{t('guides.aiRiskAdvisor.capacityRisk')}</h4>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.aiRiskAdvisor.capacityRiskDesc')}
                                </p>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Using AI Recommendations */}
            <Card>
                <CardHeader>
                    <CardTitle>Acting on AI Recommendations</CardTitle>
                    <CardDescription>Making the most of AI insights</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">Review Recommendations Carefully</h4>
                    <p className="text-sm text-muted-foreground">
                        The AI provides specific, actionable recommendations for each identified risk. These might include:
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• Breaking the pitch into smaller, more manageable pieces</li>
                        <li>• Clarifying scope boundaries and adding "no-gos"</li>
                        <li>• Identifying and documenting dependencies upfront</li>
                        <li>• Adjusting the appetite (time budget) to be more realistic</li>
                        <li>• Adding specific expertise to the team</li>
                    </ul>

                    <h4 className="font-semibold mt-6">Provide Feedback</h4>
                    <p className="text-sm text-muted-foreground mb-2">
                        Help improve the AI by providing feedback on its assessments:
                    </p>
                    <div className="flex gap-3">
                        <div className="flex-1 rounded-lg border p-3">
                            <div className="flex items-center gap-2 mb-1">
                                <ThumbsUp className="h-4 w-4 text-green-500" />
                                <span className="font-semibold text-sm">Helpful</span>
                            </div>
                            <p className="text-xs text-muted-foreground">
                                The assessment was accurate and useful
                            </p>
                        </div>
                        <div className="flex-1 rounded-lg border p-3">
                            <div className="flex items-center gap-2 mb-1">
                                <ThumbsDown className="h-4 w-4 text-red-500" />
                                <span className="font-semibold text-sm">Not Helpful</span>
                            </div>
                            <p className="text-xs text-muted-foreground">
                                The assessment missed the mark
                            </p>
                        </div>
                    </div>
                    <p className="text-xs text-muted-foreground mt-2">
                        Your feedback helps the AI learn and improve future assessments for your team.
                    </p>
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
                                <strong>Check before betting:</strong> Review AI risk assessments before betting meetings
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Don't ignore warnings:</strong> High-risk assessments deserve serious consideration
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Use as a guide, not gospel:</strong> AI provides insights, but you make the final call
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Provide context:</strong> More detailed pitches lead to better AI assessments
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Track accuracy:</strong> Note when AI predictions were right or wrong to calibrate trust
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
                        <Link to="/help/betting-meeting">
                            🎲 Running a Betting Meeting
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-start">
                        <Link to="/help/cycle-setup">
                            📅 Setting Up Your First Cycle
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
