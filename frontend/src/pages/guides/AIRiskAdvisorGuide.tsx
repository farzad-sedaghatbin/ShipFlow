import { Link } from 'react-router-dom';
import { ArrowLeft, Brain, AlertTriangle, CheckCircle2, ThumbsUp, ThumbsDown } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function AIRiskAdvisorGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">Using AI Risk Advisor</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Leverage AI-powered insights to identify risks and improve your planning
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Brain className="h-5 w-5" />
                        What is the AI Risk Advisor?
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The <strong>AI Risk Advisor</strong> is an intelligent assistant that analyzes your pitches,
                        cycles, and projects to identify potential risks, dependencies, and areas of concern before
                        they become problems.
                    </p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 How it works:</strong> The AI analyzes your pitch descriptions, scope items,
                            historical data, and team capacity to provide contextual risk assessments and recommendations.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Where to Find It */}
            <Card>
                <CardHeader>
                    <CardTitle>Where to Find AI Risk Assessments</CardTitle>
                    <CardDescription>Accessing risk insights throughout ShipFlow</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">📄 Pitch Detail Pages</h4>
                            <p className="text-sm text-muted-foreground">
                                View AI-generated risk assessments for individual pitches, including scope risks,
                                technical complexity, and dependency warnings.
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">🔄 Cycle Overview</h4>
                            <p className="text-sm text-muted-foreground">
                                See aggregate risk scores and health metrics for entire cycles, helping you
                                understand overall cycle health.
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">🎲 Betting Table</h4>
                            <p className="text-sm text-muted-foreground">
                                Risk indicators appear next to each pitch during betting meetings, helping
                                inform your commitment decisions.
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">💬 Q&A Panel</h4>
                            <p className="text-sm text-muted-foreground">
                                Ask the AI specific questions about risks, dependencies, or concerns using
                                the floating Q&A button.
                            </p>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/ai-risk-advisor.png"
                            alt="AI Risk Advisor panel showing risk assessment and recommendations"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: AI Risk Advisor panel
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Understanding Risk Scores */}
            <Card>
                <CardHeader>
                    <CardTitle>Understanding Risk Scores</CardTitle>
                    <CardDescription>Interpreting AI assessments</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground mb-4">
                        The AI assigns risk scores on a scale, typically categorized as:
                    </p>

                    <div className="space-y-3">
                        <div className="rounded-lg border-2 border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                                <h5 className="font-semibold text-green-900 dark:text-green-100">Low Risk</h5>
                            </div>
                            <p className="text-sm text-green-800 dark:text-green-200">
                                Well-defined scope, clear approach, no major dependencies. Proceed with confidence.
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-yellow-200 dark:border-yellow-800 bg-yellow-50 dark:bg-yellow-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                                <h5 className="font-semibold text-yellow-900 dark:text-yellow-100">Medium Risk</h5>
                            </div>
                            <p className="text-sm text-yellow-800 dark:text-yellow-200">
                                Some uncertainties or dependencies identified. Review recommendations and consider
                                mitigation strategies.
                            </p>
                        </div>

                        <div className="rounded-lg border-2 border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950 p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
                                <h5 className="font-semibold text-red-900 dark:text-red-100">High Risk</h5>
                            </div>
                            <p className="text-sm text-red-800 dark:text-red-200">
                                Significant concerns about scope, complexity, or feasibility. May need reshaping
                                or breaking into smaller pitches.
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Common Risk Types */}
            <Card>
                <CardHeader>
                    <CardTitle>Common Risk Types Identified</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-3">
                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">Scope Creep Risk</h4>
                                <p className="text-sm text-muted-foreground">
                                    The pitch may be too broad or lacks clear boundaries, increasing the chance of
                                    expanding beyond the appetite.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">Technical Complexity</h4>
                                <p className="text-sm text-muted-foreground">
                                    The solution involves complex technical challenges that may require more time
                                    or expertise than estimated.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">Dependency Risk</h4>
                                <p className="text-sm text-muted-foreground">
                                    The pitch depends on other work, external systems, or third-party integrations
                                    that could cause delays.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">Resource Constraints</h4>
                                <p className="text-sm text-muted-foreground">
                                    Team capacity or expertise may not align with the pitch requirements.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
                            <AlertTriangle className="h-5 w-5 text-orange-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <h4 className="font-semibold">Unclear Requirements</h4>
                                <p className="text-sm text-muted-foreground">
                                    The pitch lacks sufficient detail or has ambiguous acceptance criteria.
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
