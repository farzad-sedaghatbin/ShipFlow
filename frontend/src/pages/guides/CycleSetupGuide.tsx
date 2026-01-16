import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Repeat, Calendar, FileText, Users, CheckCircle2, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function CycleSetupGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">Setting Up Your First Cycle</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Complete walkthrough of creating and managing a work cycle in ShipFlow
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Repeat className="h-5 w-5" />
                        What is a Cycle?
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        In Shape Up, a <strong>cycle</strong> is a fixed period of time (typically 6 weeks) during
                        which teams work on committed pitches. Cycles provide rhythm and focus, with clear start
                        and end dates.
                    </p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Key Concept:</strong> After each 6-week cycle, there's a 2-week "cool-down"
                            period for bug fixes, exploration, and planning the next cycle.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 1: Creating a Cycle */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Calendar className="h-5 w-5" />
                        Step 1: Creating a New Cycle
                    </CardTitle>
                    <CardDescription>Set up the basic cycle structure</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Navigate to <strong>Cycles</strong> in the sidebar</li>
                        <li>Click the <strong>"New Cycle"</strong> button</li>
                        <li>Fill in the cycle details:
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li><strong>Name:</strong> e.g., "Cycle 1 - Q1 2026" or "January Cycle"</li>
                                <li><strong>Start Date:</strong> When the cycle begins</li>
                                <li><strong>End Date:</strong> Typically 6 weeks from start</li>
                                <li><strong>Project:</strong> Associate with a project (optional)</li>
                                <li><strong>Description:</strong> Brief overview of cycle goals</li>
                            </ul>
                        </li>
                        <li>Click <strong>"Create Cycle"</strong></li>
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
                            <strong>⚠️ Important:</strong> Once a cycle starts, avoid changing the end date.
                            Fixed time is a core principle of Shape Up.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 2: Adding Pitches */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FileText className="h-5 w-5" />
                        Step 2: Adding Pitches to the Cycle
                    </CardTitle>
                    <CardDescription>Commit to the work you'll tackle</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        After creating a cycle, add the pitches that were approved during the betting meeting:
                    </p>

                    <h4 className="font-semibold">Option 1: From the Cycle Detail Page</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>Open the cycle you just created</li>
                        <li>Look for the <strong>"Add Pitch"</strong> or <strong>"Manage Pitches"</strong> button</li>
                        <li>Select pitches from the available list</li>
                        <li>Click <strong>"Add to Cycle"</strong></li>
                    </ol>

                    <h4 className="font-semibold mt-4">Option 2: From the Betting Table</h4>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li>Navigate to <strong>Betting Table</strong></li>
                        <li>For each approved pitch, select the cycle from the dropdown</li>
                        <li>The pitch is automatically added to that cycle</li>
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
                        Step 3: Assigning Team Members
                    </CardTitle>
                    <CardDescription>Allocate people to pitches</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        Each pitch in the cycle needs a small team (typically 1-2 people) assigned to it:
                    </p>

                    <ol className="list-decimal list-inside space-y-3">
                        <li>From the cycle detail page, click on a pitch</li>
                        <li>In the pitch detail view, look for the <strong>"Assign Team"</strong> section</li>
                        <li>Add team members:
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li>1 designer (for UI/UX work)</li>
                                <li>1-2 developers (for implementation)</li>
                            </ul>
                        </li>
                        <li>Save the assignments</li>
                    </ol>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Shape Up Principle:</strong> Keep teams small. Two people working together
                            can move faster and communicate better than larger groups.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 4: Tracking Progress */}
            <Card>
                <CardHeader>
                    <CardTitle>Step 4: Tracking Progress During the Cycle</CardTitle>
                    <CardDescription>Monitor work and identify risks</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <h4 className="font-semibold">Use Hill Charts</h4>
                    <p className="text-sm text-muted-foreground">
                        Navigate to the cycle's hill chart to see visual progress of all pitches. Team members
                        should update their positions regularly.
                    </p>

                    <h4 className="font-semibold mt-4">Check Health Overview</h4>
                    <p className="text-sm text-muted-foreground">
                        Visit <strong>Health Overview</strong> to see aggregate metrics:
                    </p>
                    <ul className="space-y-1 ml-6 text-sm">
                        <li>• Overall cycle health score</li>
                        <li>• Pitches at risk</li>
                        <li>• Velocity and burn-down</li>
                        <li>• Team capacity utilization</li>
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
