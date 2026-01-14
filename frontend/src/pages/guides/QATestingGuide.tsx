import { Link } from 'react-router-dom';
import { ArrowLeft, CheckSquare, Bug, Sparkles, Beaker } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function QATestingGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">QA & Testing</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Manage test cases, run manual tests, and track bugs within your cycles
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Beaker className="h-5 w-5" />
                        Integrated QA
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow integrates QA directly into the development cycle. Instead of treating testing
                        as an afterthought, you can define test cases alongside your scopes and track quality
                        in real-time.
                    </p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Best Practice:</strong> Create test cases during the "Shaping" phase or early
                            in the "Build" phase to clarify requirements before code is written.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Test Cases */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <CheckSquare className="h-5 w-5" />
                        Managing Test Cases
                    </CardTitle>
                    <CardDescription>Define how features should be verified</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Navigate to <strong>QA & Testing {'>'} Test Cases</strong></li>
                        <li>Click <strong>"New Test Case"</strong></li>
                        <li>Fill in the details:
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li><strong>Title:</strong> Clear, concise summary of what's being tested</li>
                                <li><strong>Pre-conditions:</strong> State required before testing starts</li>
                                <li><strong>Steps:</strong> Numbered actions to perform</li>
                                <li><strong>Expected Result:</strong> What should happen if successful</li>
                                <li><strong>Priority:</strong> Critical, High, Medium, or Low</li>
                            </ul>
                        </li>
                        <li>Link it to a specific <strong>Pitch</strong> or <strong>Cycle</strong> to track coverage</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/qa-test-cases.png"
                            alt="Test Cases list view showing status and priorities"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Test Cases List
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* AI Generation */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Sparkles className="h-5 w-5 text-purple-500" />
                        AI Test Generation
                    </CardTitle>
                    <CardDescription>Speed up test creation with AI</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow can automatically generate test cases based on your pitch descriptions and scope definitions.
                    </p>
                    <div className="grid gap-4 md:grid-cols-2">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">How to use it:</h4>
                            <ol className="list-decimal list-inside space-y-2 text-sm">
                                <li>Go to <strong>Generate with AI</strong> in the Test Cases page</li>
                                <li>Select the <strong>Pitch</strong> you want to test</li>
                                <li>Review the suggested test cases</li>
                                <li>Click <strong>"Save Selected"</strong> to add them to your library</li>
                            </ol>
                        </div>
                        <div className="rounded-lg bg-purple-50 dark:bg-purple-950/20 border border-purple-200 dark:border-purple-900 p-4">
                            <h4 className="font-semibold text-purple-900 dark:text-purple-100 mb-2">Why use AI?</h4>
                            <ul className="list-disc list-inside space-y-1 text-sm text-purple-800 dark:text-purple-200">
                                <li>Discover edge cases you might miss</li>
                                <li>Save time on writing boilerplate steps</li>
                                <li>Standardize test case format</li>
                            </ul>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Bug Tracking */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Bug className="h-5 w-5" />
                        Bug Reports
                    </CardTitle>
                    <CardDescription>Track issues found during testing</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        When a test fails or you find an issue during exploratory testing, log a bug report.
                    </p>
                    <ul className="space-y-2 text-sm">
                        <li className="flex items-start gap-2">
                            <span className="font-semibold min-w-[80px]">Severity:</span>
                            <span>From "Low" (cosmetic) to "Critical" (system down)</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <span className="font-semibold min-w-[80px]">Status:</span>
                            <span>Open → In Progress → Resolved → Verified</span>
                        </li>
                    </ul>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/qa-bug-reports.png"
                            alt="Bug Reports dashboard"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Bug Reports List
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
