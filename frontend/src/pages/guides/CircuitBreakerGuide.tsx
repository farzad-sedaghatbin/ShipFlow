import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Zap, AlertTriangle, XCircle, TrendingUp, CheckCircle2, Shield } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function CircuitBreakerGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">Circuit Breaker</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    The Shape Up safety valve - detect and respond to scope overflow
                </p>
            </div>

            <Separator />

            {/* Shape Up Principle */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Shield className="h-5 w-5" />
                        The Fixed Time, Variable Scope Principle
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        In Shape Up methodology, <strong>time is fixed</strong>, not scope. When a pitch consistently
                        goes over its appetite (time budget), it signals a fundamental problem: the scope wasn't
                        properly shaped or unexpected complexity emerged.
                    </p>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>🎯 Shape Up Wisdom:</strong> "If a pitch is taking too long, something is wrong.
                            You don't extend the deadline—you either cut scope or kill the pitch."
                        </p>
                    </div>
                    <p>
                        The Circuit Breaker is ShipFlow's automated mechanism to flag these situations and help teams
                        make the tough decisions that Shape Up requires.
                    </p>
                </CardContent>
            </Card>

            {/* How It Works */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Zap className="h-5 w-5" />
                        How Circuit Breaker Works
                    </CardTitle>
                    <CardDescription>Automated overflow detection and alerts</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                1
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">Continuous Monitoring</p>
                                <p className="text-sm text-muted-foreground">
                                    ShipFlow tracks work logs against each pitch's appetite (time budget) in real-time.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                2
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">Threshold Detection</p>
                                <p className="text-sm text-muted-foreground">
                                    When a pitch reaches your configured threshold (default 80% of appetite), it appears
                                    in the overflow detection list with color-coded severity.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                3
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">Team Notification</p>
                                <p className="text-sm text-muted-foreground">
                                    When you trigger a circuit breaker, all team members assigned to the pitch receive
                                    a dashboard notification to discuss the situation.
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4 space-y-2">
                        <p className="text-sm font-medium">Severity Levels:</p>
                        <div className="grid gap-2">
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-blue-500"></div>
                                <span className="text-muted-foreground">&lt; 80%: Within budget</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-yellow-500"></div>
                                <span className="text-muted-foreground">80-89%: Warning zone</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-orange-500"></div>
                                <span className="text-muted-foreground">90-99%: Critical</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-red-500"></div>
                                <span className="text-muted-foreground">≥ 100%: Over budget</span>
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Accessing Circuit Breaker */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <TrendingUp className="h-5 w-5" />
                        Accessing the Circuit Breaker Monitor
                    </CardTitle>
                    <CardDescription>Where to find and configure overflow detection</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Navigate to your <strong>Active Cycle</strong></li>
                        <li>Look for the <strong>Circuit Breaker</strong> link in the cycle navigation</li>
                        <li>Or access via <code>/cycles/:cycleId/circuit-breaker</code></li>
                    </ol>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Tip:</strong> Adjust the detection threshold slider (50-150%) to match your
                            team's tolerance for scope expansion. Conservative teams use 80%, more flexible teams may use 100%.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Triggering Circuit Breaker */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <AlertTriangle className="h-5 w-5" />
                        Triggering a Circuit Breaker
                    </CardTitle>
                    <CardDescription>When to flag a pitch for team discussion</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        When a pitch appears in the overflow list, you can <strong>trigger the circuit breaker</strong> to:
                    </p>
                    <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground">
                        <li>Alert the team that this pitch needs immediate attention</li>
                        <li>Document the specific overflow reason (e.g., "Unexpected API complexity")</li>
                        <li>Force a conversation about scope reduction or pitch cancellation</li>
                        <li>Generate dashboard notifications for all pitch stakeholders</li>
                    </ul>

                    <div className="rounded-lg border-2 border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950/30 p-4">
                        <p className="text-sm font-medium text-orange-900 dark:text-orange-100 mb-2">
                            ⚠️ What Triggering Does:
                        </p>
                        <ul className="text-sm text-orange-900 dark:text-orange-100 space-y-1">
                            <li>✓ Flags the pitch with a red "Circuit Breaker" badge</li>
                            <li>✓ Sends notifications to all team members</li>
                            <li>✓ Adds 50 risk points in risk analysis</li>
                            <li>✓ Moves pitch to high-priority discussion queue</li>
                        </ul>
                    </div>

                    <p className="text-sm text-muted-foreground">
                        After triggering, the team should meet to decide: <strong>cut scope</strong> or <strong>kill the pitch</strong>.
                    </p>
                </CardContent>
            </Card>

            {/* Killing a Pitch */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <XCircle className="h-5 w-5 text-destructive" />
                        Killing a Pitch
                    </CardTitle>
                    <CardDescription>The hardest but sometimes necessary decision</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        When scope can't be cut enough to finish within the appetite, Shape Up says: <strong>kill it</strong>.
                        This isn't failure—it's discipline.
                    </p>

                    <div className="rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 p-4">
                        <p className="text-sm text-red-900 dark:text-red-100 mb-2">
                            <strong>🛑 Killing a Pitch:</strong>
                        </p>
                        <ul className="text-sm text-red-900 dark:text-red-100 space-y-1">
                            <li>• Changes pitch status to <code>CANCELLED</code></li>
                            <li>• Notifies all team members with the cancellation reason</li>
                            <li>• Preserves work logs and history for future reference</li>
                            <li>• Frees up team capacity for other work</li>
                        </ul>
                    </div>

                    <div className="space-y-2">
                        <p className="text-sm font-medium">When to Kill a Pitch:</p>
                        <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                            <li>It's consistently running 50%+ over appetite with no end in sight</li>
                            <li>The core problem turned out to be different than shaped</li>
                            <li>Technical complexity emerged that makes the appetite unrealistic</li>
                            <li>The business value no longer justifies the expanded investment</li>
                        </ul>
                    </div>

                    <div className="rounded-lg bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800 p-4">
                        <p className="text-sm text-green-900 dark:text-green-100">
                            <strong>✅ Remember:</strong> A killed pitch isn't wasted work. The learnings inform better
                            shaping for future cycles. You can always re-pitch with a better understanding and more realistic appetite.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Resolving Circuit Breaker */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <CheckCircle2 className="h-5 w-5" />
                        Resolving a Circuit Breaker
                    </CardTitle>
                    <CardDescription>When you've addressed the overflow</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        If the team successfully cuts scope and gets back on track, you can <strong>resolve the circuit breaker</strong>:
                    </p>
                    <ol className="list-decimal list-inside space-y-2">
                        <li>Select the new status (typically <code>IN_PROGRESS</code> or <code>TESTING</code>)</li>
                        <li>Click "Resolve Circuit Breaker"</li>
                        <li>The pitch returns to normal tracking without the warning flag</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <p className="text-sm text-muted-foreground">
                            <strong>Pro Tip:</strong> In your retrospective, discuss what went wrong with the shaping
                            that led to the overflow. This is how teams get better at estimating appetite.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Best Practices */}
            <Card>
                <CardHeader>
                    <CardTitle>Best Practices</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="space-y-2">
                        <p className="font-medium text-sm">✓ Do:</p>
                        <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                            <li>Check the Circuit Breaker monitor regularly (weekly for 6-week cycles)</li>
                            <li>Set realistic thresholds based on your team's historical performance</li>
                            <li>Trigger circuit breakers early when you see patterns forming</li>
                            <li>Have honest conversations about scope vs. appetite</li>
                            <li>Document reasons for kills to inform future shaping</li>
                        </ul>
                    </div>

                    <div className="space-y-2">
                        <p className="font-medium text-sm">✗ Don't:</p>
                        <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                            <li>Ignore overflow signals hoping things will improve</li>
                            <li>Extend appetites without understanding root causes</li>
                            <li>Use circuit breakers punitively against team members</li>
                            <li>Kill pitches without extracting learnings for the next cycle</li>
                            <li>Set thresholds so high that they never trigger</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
