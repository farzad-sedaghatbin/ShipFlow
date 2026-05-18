import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Users, Dices, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function BettingMeetingGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.bettingMeeting.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.bettingMeeting.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Dices className="h-5 w-5" />
                        {t('guides.bettingMeeting.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.bettingMeeting.whatDesc')} /></p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.bettingMeeting.keyPrinciple')}</strong> {t('guides.bettingMeeting.keyPrincipleDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Before the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.bettingMeeting.beforeTitle')}</CardTitle>
                    <CardDescription>{t('guides.bettingMeeting.beforeSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">{t('guides.bettingMeeting.step0Title')}</h4>
                    <p className="text-sm text-muted-foreground">
                        <MarkdownInline content={t('guides.bettingMeeting.step0Desc')} />
                    </p>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.bettingMeeting.step0Tip')}</strong>{' '}
                            {t('guides.bettingMeeting.step0TipDesc')}
                        </p>
                    </div>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.step1Title')}</h4>
                    <p className="text-sm text-muted-foreground">
                        <MarkdownInline content={t('guides.bettingMeeting.step1Desc')} />
                    </p>
                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/pitch-board.png"
                            alt="Pitch board showing available pitches for betting"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Pitch board
                        </div>
                    </div>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.step2Title')}</h4>
                    <p className="text-sm text-muted-foreground mb-2">
                        {t('guides.bettingMeeting.step2Desc')}
                    </p>
                    <ul className="space-y-2 ml-6">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.problemStatement')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.appetite')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.solutionSketch')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.risks')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.noGos')} />
                        </li>
                    </ul>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/pitch-detail.png"
                            alt="Pitch detail page showing problem, solution, and scope"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Pitch detail view
                        </div>
                    </div>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.step3Title')}</h4>
                    <p className="text-sm text-muted-foreground">
                        {t('guides.bettingMeeting.step3Desc')}
                    </p>
                </CardContent>
            </Card>

            {/* During the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Users className="h-5 w-5" />
                        {t('guides.bettingMeeting.duringTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.bettingMeeting.duringSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">{t('guides.bettingMeeting.usingTableTitle')}</h4>
                    <p className="text-sm text-muted-foreground mb-2">
                        <MarkdownInline content={t('guides.bettingMeeting.usingTableDesc')} />
                    </p>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/betting-table.png"
                            alt="Betting table showing all pitches with status and actions"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Betting table
                        </div>
                    </div>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.decisionFramework')}</h4>
                    <div className="space-y-3">
                        <div className="rounded-lg border-2 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 p-4">
                            <h5 className="font-semibold text-green-900 dark:text-green-100 mb-2">{t('guides.bettingMeeting.betOnIt')}</h5>
                            <p className="text-sm text-green-800 dark:text-green-200">
                                {t('guides.bettingMeeting.betOnItDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950 p-4">
                            <h5 className="font-semibold text-yellow-900 dark:text-yellow-100 mb-2">{t('guides.bettingMeeting.tableIt')}</h5>
                            <p className="text-sm text-yellow-800 dark:text-yellow-200">
                                {t('guides.bettingMeeting.tableItDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950 p-4">
                            <h5 className="font-semibold text-red-900 dark:text-red-100 mb-2">{t('guides.bettingMeeting.pass')}</h5>
                            <p className="text-sm text-red-800 dark:text-red-200">
                                {t('guides.bettingMeeting.passDesc')}
                            </p>
                        </div>
                    </div>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.keyQuestions')}</h4>
                    <ul className="space-y-2 ml-6 text-sm">
                        <li>• {t('guides.bettingMeeting.question1')}</li>
                        <li>• {t('guides.bettingMeeting.question2')}</li>
                        <li>• {t('guides.bettingMeeting.question3')}</li>
                        <li>• {t('guides.bettingMeeting.question4')}</li>
                        <li>• {t('guides.bettingMeeting.question5')}</li>
                        <li>• {t('guides.bettingMeeting.question6')}</li>
                    </ul>
                </CardContent>
            </Card>

            {/* After the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.bettingMeeting.afterTitle')}</CardTitle>
                    <CardDescription>{t('guides.bettingMeeting.afterSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">{t('guides.bettingMeeting.after1Title')}</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li><MarkdownInline content={t('guides.bettingMeeting.after1Step1')} /></li>
                        <li><MarkdownInline content={t('guides.bettingMeeting.after1Step2')} /></li>
                        <li>{t('guides.bettingMeeting.after1Step3')}</li>
                        <li>{t('guides.bettingMeeting.after1Step4')}</li>
                    </ol>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.after2Title')}</h4>
                    <p className="text-sm text-muted-foreground">
                        {t('guides.bettingMeeting.after2Desc')}
                    </p>

                    <h4 className="font-semibold mt-6">{t('guides.bettingMeeting.after3Title')}</h4>
                    <p className="text-sm text-muted-foreground">
                        {t('guides.bettingMeeting.after3Desc')}
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• {t('guides.bettingMeeting.after3Item1')}</li>
                        <li>• {t('guides.bettingMeeting.after3Item2')}</li>
                        <li>• {t('guides.bettingMeeting.after3Item3')}</li>
                        <li>• {t('guides.bettingMeeting.after3Item4')}</li>
                    </ul>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4 mt-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.bettingMeeting.after3ProTip')}</strong> {t('guides.bettingMeeting.after3ProTipDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Best Practices */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.bettingMeeting.bestPracticesTitle')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <ul className="space-y-3">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.bestPractice1')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <MarkdownInline content={t('guides.bettingMeeting.bestPractice2')} />
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Don't overcommit:</strong> Leave buffer capacity for unexpected issues
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Trust the shapers:</strong> If a pitch is well-shaped, trust the team to execute
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <AlertCircle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Avoid scope creep:</strong> Stick to what's in the pitch - no adding features mid-cycle
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
                        <Link to="/help/ai-risk-advisor">
                            🤖 Using AI Risk Advisor
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-start">
                        <Link to="/help/hill-charts">
                            📈 Understanding Hill Charts
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
