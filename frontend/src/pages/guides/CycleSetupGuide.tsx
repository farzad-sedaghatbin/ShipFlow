import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Repeat, Calendar, FileText, Users, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function CycleSetupGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.cycleSetup.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.cycleSetup.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Shape Up Only Notice */}
            <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border-2 border-amber-200 dark:border-amber-800 p-4">
                <div className="flex items-start gap-3">
                    <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
                    <div className="space-y-1">
                        <p className="text-sm font-semibold text-amber-900 dark:text-amber-100">
                            Shape Up Projects Only
                        </p>
                        <p className="text-sm text-amber-800 dark:text-amber-200">
                            This guide applies to <strong>Shape Up</strong> projects with 6-week cycles. Kanban projects use continuous flow without fixed cycles. 
                            See the <Link to="/help/project-types" className="underline font-semibold">Project Types Guide</Link> to learn about the differences.
                        </p>
                    </div>
                </div>
            </div>

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Repeat className="h-5 w-5" />
                        {t('guides.cycleSetup.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.cycleSetup.whatDesc')} /></p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.cycleSetup.keyConcept')}</strong> {t('guides.cycleSetup.keyConceptDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 1: Creating a Cycle */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Calendar className="h-5 w-5" />
                        {t('guides.cycleSetup.step1Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.cycleSetup.step1Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>{t('guides.cycleSetup.step1Item1')}</li>
                        <li>{t('guides.cycleSetup.step1Item2')}</li>
                        <li>{t('guides.cycleSetup.step1Item3')}
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li><MarkdownInline content={t('guides.cycleSetup.step1Item3a')} /></li>
                                <li><MarkdownInline content={t('guides.cycleSetup.step1Item3b')} /></li>
                                <li><MarkdownInline content={t('guides.cycleSetup.step1Item3c')} /></li>
                                <li><MarkdownInline content={t('guides.cycleSetup.step1Item3d')} /></li>
                                <li><MarkdownInline content={t('guides.cycleSetup.step1Item3e')} /></li>
                            </ul>
                        </li>
                        <li>{t('guides.cycleSetup.step1Item4')}</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/cycle-form.png"
                            alt="Cycle creation form with name, dates, and description fields"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Cycle creation form
                        </div>
                    </div>

                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.cycleSetup.important')}</strong> {t('guides.cycleSetup.importantDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 2: Adding Pitches */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FileText className="h-5 w-5" />
                        {t('guides.cycleSetup.step2Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.cycleSetup.step2Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        {t('guides.cycleSetup.step2Desc')}
                    </p>

                    <h4 className="font-semibold">{t('guides.cycleSetup.step2Option1Title')}</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>{t('guides.cycleSetup.step2Option1Step1')}</li>
                        <li>{t('guides.cycleSetup.step2Option1Step2')}</li>
                        <li>{t('guides.cycleSetup.step2Option1Step3')}</li>
                        <li>{t('guides.cycleSetup.step2Option1Step4')}</li>
                    </ol>

                    <h4 className="font-semibold mt-4">{t('guides.cycleSetup.step2Option2Title')}</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>{t('guides.cycleSetup.step2Option2Step1')}</li>
                        <li>{t('guides.cycleSetup.step2Option2Step2')}</li>
                        <li>{t('guides.cycleSetup.step2Option2Step3')}</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/cycle-detail.png"
                            alt="Cycle detail page showing assigned pitches and team members"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Cycle detail view
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Step 3: Assigning Teams */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Users className="h-5 w-5" />
                        {t('guides.cycleSetup.step3Title')}
                    </CardTitle>
                    <CardDescription>{t('guides.cycleSetup.step3Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        {t('guides.cycleSetup.step3Desc')}
                    </p>

                    <ol className="list-decimal list-inside space-y-3">
                        <li>{t('guides.cycleSetup.step3Step1')}</li>
                        <li>{t('guides.cycleSetup.step3Step2')}</li>
                        <li>{t('guides.cycleSetup.step3Step3')}
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li>{t('guides.cycleSetup.step3Step3a')}</li>
                                <li>{t('guides.cycleSetup.step3Step3b')}</li>
                            </ul>
                        </li>
                        <li>{t('guides.cycleSetup.step3Step4')}</li>
                    </ol>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.cycleSetup.step3Tip')}</strong> {t('guides.cycleSetup.step3TipDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 4: Tracking Progress */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.cycleSetup.step4Title')}</CardTitle>
                    <CardDescription>{t('guides.cycleSetup.step4Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">{t('guides.cycleSetup.step4UseHillTitle')}</h4>
                    <p className="text-sm text-muted-foreground">
                        {t('guides.cycleSetup.step4UseHillDesc')}
                    </p>

                    <h4 className="font-semibold mt-4">{t('guides.cycleSetup.step4HealthTitle')}</h4>
                    <p className="text-sm text-muted-foreground">
                        {t('guides.cycleSetup.step4HealthDesc')}
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• {t('guides.cycleSetup.step4HealthItem1')}</li>
                        <li>• {t('guides.cycleSetup.step4HealthItem2')}</li>
                        <li>• {t('guides.cycleSetup.step4HealthItem3')}</li>
                        <li>• {t('guides.cycleSetup.step4HealthItem4')}</li>
                    </ul>

                    <h4 className="font-semibold mt-4">Daily Check-ins</h4>
                    <p className="text-sm text-muted-foreground">
                        Use <strong>Work Logs</strong> to track daily progress and blockers. This helps identify
                        issues early.
                    </p>

                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4 mt-4">
                        <h4 className="font-semibold text-amber-900 dark:text-amber-100 mb-2">⚠️ Warning Signs</h4>
                        <ul className="space-y-1 text-sm text-amber-900 dark:text-amber-100">
                            <li>• Pitches stuck on the uphill side of the hill chart</li>
                            <li>• High AI risk scores that aren't decreasing</li>
                            <li>• Team members reporting consistent blockers</li>
                            <li>• Scope expanding beyond original pitch boundaries</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* Step 5: Handling Issues */}
            <Card>
                <CardHeader>
                    <CardTitle>Step 5: Handling Mid-Cycle Issues</CardTitle>
                    <CardDescription>What to do when things go wrong</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <AlertCircle className="h-5 w-5 text-orange-500" />
                                Pitch is Taking Longer Than Expected
                            </h4>
                            <p className="text-sm text-muted-foreground mb-2">
                                <strong>Don't extend the cycle.</strong> Instead:
                            </p>
                            <ul className="space-y-1 text-sm ml-6">
                                <li>• Cut scope - what can you remove and still ship value?</li>
                                <li>• Focus on the core problem being solved</li>
                                <li>• Document what's being cut for potential future cycles</li>
                            </ul>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <AlertCircle className="h-5 w-5 text-orange-500" />
                                Major Blocker Discovered
                            </h4>
                            <p className="text-sm text-muted-foreground mb-2">
                                If a pitch becomes truly impossible:
                            </p>
                            <ul className="space-y-1 text-sm ml-6">
                                <li>• Document why it's blocked</li>
                                <li>• Consider if it can be reshaped for a future cycle</li>
                                <li>• Don't add new work mid-cycle to replace it</li>
                                <li>• Use freed capacity for cool-down activities</li>
                            </ul>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <AlertCircle className="h-5 w-5 text-orange-500" />
                                Urgent Bug or Issue
                            </h4>
                            <p className="text-sm text-muted-foreground mb-2">
                                For critical production issues:
                            </p>
                            <ul className="space-y-1 text-sm ml-6">
                                <li>• Use the <strong>Bug Reports</strong> feature to track</li>
                                <li>• Allocate minimal time to fix</li>
                                <li>• Consider if it can wait until cool-down</li>
                                <li>• Don't let bugs derail the entire cycle</li>
                            </ul>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Step 6: Closing the Cycle */}
            <Card>
                <CardHeader>
                    <CardTitle>Step 6: Closing the Cycle</CardTitle>
                    <CardDescription>Wrapping up and reflecting</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">When the Cycle Ends</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>Ship what's done - even if not 100% complete</li>
                        <li>Mark the cycle as <strong>"Completed"</strong> in ShipFlow</li>
                        <li>Schedule a retrospective meeting</li>
                        <li>Use the <strong>Retrospectives</strong> feature to document:
                            <ul className="list-disc list-inside ml-6 mt-1 space-y-1 text-muted-foreground">
                                <li>What went well</li>
                                <li>What could be improved</li>
                                <li>Action items for next cycle</li>
                            </ul>
                        </li>
                        <li>Enter cool-down period (2 weeks)</li>
                    </ol>

                    <h4 className="font-semibold mt-6">During Cool-Down</h4>
                    <p className="text-sm text-muted-foreground">
                        Use this time for:
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• Fixing bugs and polish</li>
                        <li>• Exploring new ideas</li>
                        <li>• Shaping pitches for the next cycle</li>
                        <li>• Technical debt and improvements</li>
                        <li>• Team learning and development</li>
                    </ul>
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
                                <strong>Respect the time box:</strong> Never extend a cycle - it undermines the methodology
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Don't overload:</strong> Better to commit to 2-3 solid pitches than 5 risky ones
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Update hill charts regularly:</strong> Daily or every few days keeps everyone aligned
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Communicate early:</strong> If a pitch is in trouble, raise it immediately
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Celebrate wins:</strong> Acknowledge what shipped, even if scope was cut
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
                        <Link to="/help/hill-charts">
                            📈 Understanding Hill Charts
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-start">
                        <Link to="/help/ai-risk-advisor">
                            🤖 Using AI Risk Advisor
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
