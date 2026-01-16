import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Users, Dices, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function BettingMeetingGuide() {
    const { t } = useTranslation();
    if (false) console.log(t);
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
                <h1 className="text-4xl font-bold tracking-tight">Running a Betting Meeting</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Learn how to conduct effective betting meetings and make informed cycle commitments
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Dices className="h-5 w-5" />
                        What is a Betting Meeting?
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        A <strong>betting meeting</strong> is where stakeholders decide which pitches to commit to
                        for the next cycle. It's called "betting" because you're placing bets on what will deliver
                        the most value given the fixed time constraint.
                    </p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Key Principle:</strong> In Shape Up, you bet on pitches, not tasks.
                            Each pitch should be well-shaped with clear boundaries and appetite (time budget).
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Before the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle>Before the Meeting: Preparation</CardTitle>
                    <CardDescription>Set yourself up for success</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">1. Review Available Pitches</h4>
                    <p className="text-sm text-muted-foreground">
                        Navigate to the <strong>Pitch Board</strong> to see all available pitches for consideration.
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

                    <h4 className="font-semibold mt-6">2. Evaluate Each Pitch</h4>
                    <p className="text-sm text-muted-foreground mb-2">
                        Click on each pitch to review its details. Look for:
                    </p>
                    <ul className="space-y-2 ml-6">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Problem statement:</strong> Is the problem clearly defined?</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Appetite:</strong> How much time are we willing to spend?</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Solution sketch:</strong> Is there a clear approach?</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Risks:</strong> What could go wrong?</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>No-gos:</strong> What's explicitly out of scope?</span>
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

                    <h4 className="font-semibold mt-6">3. Check AI Risk Assessments</h4>
                    <p className="text-sm text-muted-foreground">
                        Use the AI Risk Advisor to identify potential issues before committing. This can reveal
                        hidden complexities or dependencies.
                    </p>
                </CardContent>
            </Card>

            {/* During the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Users className="h-5 w-5" />
                        During the Meeting: Making Decisions
                    </CardTitle>
                    <CardDescription>The betting table in action</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">Using the Betting Table</h4>
                    <p className="text-sm text-muted-foreground mb-2">
                        Navigate to <strong>Betting Table</strong> to see all pitches in one view:
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

                    <h4 className="font-semibold mt-6">Decision Framework</h4>
                    <div className="space-y-3">
                        <div className="rounded-lg border-2 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 p-4">
                            <h5 className="font-semibold text-green-900 dark:text-green-100 mb-2">✅ Bet On It</h5>
                            <p className="text-sm text-green-800 dark:text-green-200">
                                The pitch is well-shaped, valuable, and fits within the cycle's capacity.
                                Mark it as "Approved" or "Committed" in the betting table.
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950 p-4">
                            <h5 className="font-semibold text-yellow-900 dark:text-yellow-100 mb-2">⏸️ Table It</h5>
                            <p className="text-sm text-yellow-800 dark:text-yellow-200">
                                Good idea but needs more shaping, or not the right time. Keep it in the backlog
                                for future consideration.
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950 p-4">
                            <h5 className="font-semibold text-red-900 dark:text-red-100 mb-2">❌ Pass</h5>
                            <p className="text-sm text-red-800 dark:text-red-200">
                                Not aligned with current priorities or too risky. Mark as "Rejected" with a clear reason.
                            </p>
                        </div>
                    </div>

                    <h4 className="font-semibold mt-6">Key Questions to Ask</h4>
                    <ul className="space-y-2 ml-6 text-sm">
                        <li>• Does this pitch solve a real problem for our users?</li>
                        <li>• Is the appetite (time budget) realistic?</li>
                        <li>• Do we have the right people available?</li>
                        <li>• Are there any blocking dependencies?</li>
                        <li>• What's the opportunity cost of choosing this over other pitches?</li>
                        <li>• Can we ship something valuable within the time box?</li>
                    </ul>
                </CardContent>
            </Card>

            {/* After the Meeting */}
            <Card>
                <CardHeader>
                    <CardTitle>After the Meeting: Finalizing Commitments</CardTitle>
                    <CardDescription>Setting up the cycle for success</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">1. Create or Update the Cycle</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>Navigate to <strong>Cycles</strong> in the sidebar</li>
                        <li>Click <strong>"New Cycle"</strong> or select an upcoming cycle</li>
                        <li>Set the cycle dates (typically 6 weeks)</li>
                        <li>Add the approved pitches to the cycle</li>
                    </ol>

                    <h4 className="font-semibold mt-6">2. Assign Teams</h4>
                    <p className="text-sm text-muted-foreground">
                        For each approved pitch, assign the team members who will work on it. Each pitch
                        typically gets a small team (1-2 people).
                    </p>

                    <h4 className="font-semibold mt-6">3. Communicate Decisions</h4>
                    <p className="text-sm text-muted-foreground">
                        Make sure everyone knows:
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• What was approved and why</li>
                        <li>• What was deferred and why</li>
                        <li>• Who is working on what</li>
                        <li>• When the cycle starts and ends</li>
                    </ul>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4 mt-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Pro Tip:</strong> Use the Meetings feature to document the betting meeting
                            decisions and share notes with the team.
                        </p>
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
                                <strong>Keep it small:</strong> Limit betting meetings to key stakeholders (2-4 people)
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>Time-box discussions:</strong> Spend 5-10 minutes per pitch maximum
                            </div>
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
