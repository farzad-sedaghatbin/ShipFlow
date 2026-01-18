import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, RotateCcw, MessageSquare, ThumbsUp, ThumbsDown, Lightbulb, CheckCircle2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function RetrospectivesGuide() {
    const { t } = useTranslation();
    return (
        <div className="w-full max-w-none space-y-6">
            {/* Back Navigation */}
            <Button asChild variant="ghost" size="sm">
                <Link to="/help" className="gap-2">
                    <ArrowLeft className="h-4 w-4" />
                    Back to Help & Guides
                </Link>
            </Button>

            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.retrospectives.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.retrospectives.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Concept */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <RotateCcw className="h-5 w-5" />
                        {t('guides.retrospectives.cooldownTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.retrospectives.cooldownDesc')}
                    </p>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>⚠️ {t('guides.retrospectives.requirement')}</strong> {t('guides.retrospectives.requirementDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Creating a Retro */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <MessageSquare className="h-5 w-5" />
                        {t('guides.retrospectives.runningTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.retrospectives.runningSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Navigate to <strong>Cycles {'>'} Retros</strong></li>
                        <li>Click <strong>"Create Retro"</strong></li>
                        <li>Select the <strong>Cycle</strong> you just finished</li>
                        <li>(Optional) Add initial notes or focus areas for the team</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/retro-list.png"
                            alt="Retrospectives list showing active and past retros"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Retrospectives List
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* The Board */}
            <Card>
                <CardHeader>
                    <CardTitle>The Retro Board</CardTitle>
                    <CardDescription>Collaborate in real-time</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The retro board is divided into three standard columns. Team members can add cards with their name
                        attached or submit anonymously for psychological safety.
                    </p>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>🔒 Anonymous Submissions:</strong> When creating a retro item, check the "Post anonymously"
                            checkbox to share feedback without attribution. This encourages honest, candid input on sensitive topics.
                        </p>
                    </div>

                    <div className="grid gap-4 md:grid-cols-3">
                        <div className="p-4 rounded-lg bg-green-50 dark:bg-green-950/30 border border-green-100 dark:border-green-900">
                            <div className="flex items-center gap-2 font-semibold text-green-700 dark:text-green-300 mb-2">
                                <ThumbsUp className="h-4 w-4" />
                                Went Well
                            </div>
                            <p className="text-sm text-muted-foreground">Successes, shout-outs, and things to keep doing.</p>
                        </div>
                        <div className="p-4 rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-100 dark:border-red-900">
                            <div className="flex items-center gap-2 font-semibold text-red-700 dark:text-red-300 mb-2">
                                <ThumbsDown className="h-4 w-4" />
                                Needs Improvement
                            </div>
                            <p className="text-sm text-muted-foreground">Blockers, frustrations, and process failures.</p>
                        </div>
                        <div className="p-4 rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-100 dark:border-blue-900">
                            <div className="flex items-center gap-2 font-semibold text-blue-700 dark:text-blue-300 mb-2">
                                <Lightbulb className="h-4 w-4" />
                                Action Items
                            </div>
                            <p className="text-sm text-muted-foreground">Concrete steps to take for the next cycle.</p>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-2">
                        <img
                            src="/guides/retro-board.png"
                            alt="Interactive retrospective board"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Retro Board
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Closing */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <CheckCircle2 className="h-5 w-5" />
                        Closing the Loop
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        Once the discussion is done and action items are assigned:
                    </p>
                    <ol className="list-decimal list-inside space-y-2">
                        <li>The facilitator clicks <strong>"Close Retro"</strong></li>
                        <li>Navigate back to the Cycle Detail page</li>
                        <li>You will now see the "Retrospective Completed" checkmark</li>
                        <li>You can now safely <strong>Close the Cycle</strong></li>
                    </ol>
                </CardContent>
            </Card>
        </div>
    );
}
