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
                    {t('guides.backToHelp')}
                </Link>
            </Button>

            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.circuitBreaker.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.circuitBreaker.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Shape Up Principle */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Shield className="h-5 w-5" />
                        {t('guides.circuitBreaker.principleTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.circuitBreaker.principleDesc')}</p>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.circuitBreaker.wisdom')}</strong> {t('guides.circuitBreaker.wisdomDesc')}
                        </p>
                    </div>
                    <p>{t('guides.circuitBreaker.mechanismDesc')}</p>
                </CardContent>
            </Card>

            {/* How It Works */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Zap className="h-5 w-5" />
                        {t('guides.circuitBreaker.howTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.circuitBreaker.howSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                1
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">{t('guides.circuitBreaker.step1Title')}</p>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.circuitBreaker.step1Desc')}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                2
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">{t('guides.circuitBreaker.step2Title')}</p>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.circuitBreaker.step2Desc')}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-semibold mt-0.5">
                                3
                            </div>
                            <div className="flex-1">
                                <p className="font-medium">{t('guides.circuitBreaker.step3Title')}</p>
                                <p className="text-sm text-muted-foreground">
                                    {t('guides.circuitBreaker.step3Desc')}
                                </p>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-lg border bg-muted/50 p-4 space-y-2">
                        <p className="text-sm font-medium">{t('guides.circuitBreaker.severityTitle')}</p>
                        <div className="grid gap-2">
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-blue-500"></div>
                                <span className="text-muted-foreground">{t('guides.circuitBreaker.severityLow')}</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-yellow-500"></div>
                                <span className="text-muted-foreground">{t('guides.circuitBreaker.severityWarning')}</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-orange-500"></div>
                                <span className="text-muted-foreground">{t('guides.circuitBreaker.severityCritical')}</span>
                            </div>
                            <div className="flex items-center gap-2 text-sm">
                                <div className="h-3 w-3 rounded-full bg-red-500"></div>
                                <span className="text-muted-foreground">{t('guides.circuitBreaker.severityOver')}</span>
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
                        {t('guides.circuitBreaker.accessTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.circuitBreaker.accessSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>{t('guides.circuitBreaker.accessStep1')}</li>
                        <li>{t('guides.circuitBreaker.accessStep2')}</li>
                        <li>{t('guides.circuitBreaker.accessStep3')}</li>
                    </ol>

                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.circuitBreaker.accessTip')}</strong> {t('guides.circuitBreaker.accessTipDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Triggering Circuit Breaker */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <AlertTriangle className="h-5 w-5" />
                        {t('guides.circuitBreaker.triggerTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.circuitBreaker.triggerSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.circuitBreaker.triggerDesc')}
                    </p>
                    <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground">
                        <li>{t('guides.circuitBreaker.triggerItem1')}</li>
                        <li>{t('guides.circuitBreaker.triggerItem2')}</li>
                        <li>{t('guides.circuitBreaker.triggerItem3')}</li>
                        <li>{t('guides.circuitBreaker.triggerItem4')}</li>
                    </ul>

                    <div className="rounded-lg border-2 border-orange-200 dark:border-orange-800 bg-orange-50 dark:bg-orange-950/30 p-4">
                        <p className="text-sm font-medium text-orange-900 dark:text-orange-100 mb-2">
                            {t('guides.circuitBreaker.triggerDoesTitle')}
                        </p>
                        <ul className="text-sm text-orange-900 dark:text-orange-100 space-y-1">
                            <li>{t('guides.circuitBreaker.triggerDoes1')}</li>
                            <li>{t('guides.circuitBreaker.triggerDoes2')}</li>
                            <li>{t('guides.circuitBreaker.triggerDoes3')}</li>
                            <li>{t('guides.circuitBreaker.triggerDoes4')}</li>
                        </ul>
                    </div>

                    <p className="text-sm text-muted-foreground">
                        {t('guides.circuitBreaker.triggerAfter')}
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
                        {t('guides.circuitBreaker.killDesc')}
                    </p>

                    <div className="rounded-lg bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800 p-4">
                        <p className="text-sm text-red-900 dark:text-red-100 mb-2">
                            <strong>{t('guides.circuitBreaker.killDoesTitle')}</strong>
                        </p>
                        <ul className="text-sm text-red-900 dark:text-red-100 space-y-1">
                            <li>{t('guides.circuitBreaker.killDoes1')}</li>
                            <li>{t('guides.circuitBreaker.killDoes2')}</li>
                            <li>{t('guides.circuitBreaker.killDoes3')}</li>
                            <li>{t('guides.circuitBreaker.killDoes4')}</li>
                        </ul>
                    </div>

                    <div className="space-y-2">
                        <p className="text-sm font-medium">{t('guides.circuitBreaker.killWhenTitle')}</p>
                        <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                            <li>{t('guides.circuitBreaker.killWhen1')}</li>
                            <li>{t('guides.circuitBreaker.killWhen2')}</li>
                            <li>{t('guides.circuitBreaker.killWhen3')}</li>
                            <li>{t('guides.circuitBreaker.killWhen4')}</li>
                        </ul>
                    </div>

                    <div className="rounded-lg bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800 p-4">
                        <p className="text-sm text-green-900 dark:text-green-100">
                            <strong>{t('guides.circuitBreaker.killRemember')}</strong> {t('guides.circuitBreaker.killRememberDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Resolving Circuit Breaker */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <CheckCircle2 className="h-5 w-5" />
                        {t('guides.circuitBreaker.resolveTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.circuitBreaker.resolveSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.circuitBreaker.resolveDesc')}
                    </p>
                    <ol className="list-decimal list-inside space-y-2">
                        <li>{t('guides.circuitBreaker.resolveStep1')}</li>
                        <li>{t('guides.circuitBreaker.resolveStep2')}</li>
                        <li>{t('guides.circuitBreaker.resolveStep3')}</li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4">
                        <p className="text-sm text-muted-foreground">
                            <strong>Pro Tip:</strong> In your retrospective, discuss what went wrong with the shaping
                            that led to the overflow. This is how teams get better at estimating appetite.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Re-pitching Killed Work */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <TrendingUp className="h-5 w-5" />
                        {t('guides.circuitBreaker.repitchTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.circuitBreaker.repitchSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.circuitBreaker.repitchDesc')}</p>

                    <div className="space-y-3">
                        <p className="font-medium text-sm">{t('guides.circuitBreaker.repitchHowTitle')}</p>
                        <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground">
                            <li>{t('guides.circuitBreaker.repitchHow1')}</li>
                            <li>{t('guides.circuitBreaker.repitchHow2')}</li>
                            <li>{t('guides.circuitBreaker.repitchHow3')}</li>
                            <li>{t('guides.circuitBreaker.repitchHow4')}</li>
                        </ol>
                    </div>

                    <div className="rounded-lg bg-purple-50 dark:bg-purple-950/30 border border-purple-200 dark:border-purple-800 p-4">
                        <p className="text-sm font-medium text-purple-900 dark:text-purple-100 mb-2">
                            {t('guides.circuitBreaker.repitchExampleTitle')}
                        </p>
                        <div className="text-sm text-purple-900 dark:text-purple-100 space-y-1">
                            <p><strong>{t('guides.circuitBreaker.repitchEx1Label')}</strong> {t('guides.circuitBreaker.repitchEx1Text')}</p>
                            <p><strong>{t('guides.circuitBreaker.repitchEx2Label')}</strong> {t('guides.circuitBreaker.repitchEx2Text')}</p>
                            <p><strong>{t('guides.circuitBreaker.repitchEx3Label')}</strong> {t('guides.circuitBreaker.repitchEx3Text')}</p>
                        </div>
                    </div>

                    <p className="text-sm text-muted-foreground">
                        {t('guides.circuitBreaker.repitchWisdom')}
                    </p>
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
