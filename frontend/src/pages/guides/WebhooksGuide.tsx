import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Webhook, Zap, ArrowDownToLine, List, ShieldCheck, Code } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';

export default function WebhooksGuide() {
    const { t } = useTranslation();

    return (
        <div className="w-full max-w-none space-y-6">
            {/* Back Navigation */}
            <Button asChild variant="ghost" size="sm">
                <Link to="/help" className="gap-2">
                    <ArrowLeft className="h-4 w-4" />
                    {t('guides.backToHelp', 'Back to Help')}
                </Link>
            </Button>

            {/* Header */}
            <div>
                <h1 className="text-4xl font-bold tracking-tight">{t('helpGuides.webhooks', 'Webhooks')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('helpGuides.webhooksDesc', 'Configure incoming and outgoing webhooks to integrate ShipFlow events with external systems.')}
                </p>
            </div>

            <Separator />

            {/* Outgoing Webhooks */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Webhook className="h-5 w-5" />
                        {t('webhooksGuide.outgoing', 'Outgoing Webhooks')}
                    </CardTitle>
                    <CardDescription>{t('webhooksGuide.outgoingDesc', 'Send real-time alerts to external tools')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('webhooksGuide.outgoingIntro', 'ShipFlow can send instant notifications to other platforms (like Slack, Microsoft Teams, or custom servers) when important events occur.')}
                    </p>

                    <h3 className="font-semibold text-lg mt-4">{t('webhooksGuide.slackSetup', 'Setting up a Slack Notification Integration')}</h3>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>{t('webhooksGuide.slackStep1', 'Go to Organization Settings > Integrations.')}</li>
                        <li>{t('webhooksGuide.slackStep2', 'Click on the Slack configuration.')}</li>
                        <li>{t('webhooksGuide.slackStep3', 'Paste your Slack Incoming Webhook URL.')}</li>
                        <li>{t('webhooksGuide.slackStep4', 'Select which events should trigger a message (e.g., Pitch Created, Bet Made, Stagnation Detected).')}</li>
                    </ol>

                    <h3 className="font-semibold text-lg mt-4">{t('webhooksGuide.stagnation', 'Stagnation Alerts')}</h3>
                    <p>
                        {t('webhooksGuide.stagnationDesc', "ShipFlow's stagnation detection engine actively monitors active cycles. If a scope is stuck on the Hill Chart for too long, it will dispatch a webhook to your configured channels to alert the team.")}
                    </p>

                    <h3 className="font-semibold text-lg mt-4">{t('webhooksGuide.supportedEvents', 'Supported Outgoing Events')}</h3>
                    <div className="flex flex-wrap gap-2">
                        {['Pitch created/updated', 'Pitch approved/rejected', 'Cycle phase changed', 'Circuit breaker triggered', 'Retrospective completed', 'Test case failed'].map((event) => (
                            <Badge key={event} variant="secondary" className="text-xs">{event}</Badge>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* Inbound Webhooks — NEW */}
            <Card className="border-primary/20">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <ArrowDownToLine className="h-5 w-5" />
                        {t('webhooksGuide.inbound', 'Inbound Webhooks')}
                        <Badge variant="outline" className="text-xs ml-2">{t('common.new', 'New')}</Badge>
                    </CardTitle>
                    <CardDescription>{t('webhooksGuide.inboundDesc', 'Receive events from external services into ShipFlow')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('webhooksGuide.inboundIntro', 'ShipFlow exposes a generic, vendor-agnostic inbound webhook endpoint. Any external service (Intercom, Zendesk, PagerDuty, GitHub, etc.) can push events into ShipFlow without any vendor-specific setup.')}
                    </p>

                    <h3 className="font-semibold text-lg mt-4 flex items-center gap-2">
                        <Code className="h-4 w-4" />
                        {t('webhooksGuide.endpoint', 'Endpoint')}
                    </h3>
                    <div className="rounded-lg bg-muted p-4 font-mono text-sm">
                        <p><strong>POST</strong> /api/inbound/{'{'}<em>provider</em>{'}'}</p>
                        <p className="text-muted-foreground mt-1">{t('webhooksGuide.endpointExample', 'Example: POST /api/inbound/intercom, POST /api/inbound/zendesk')}</p>
                    </div>

                    <h3 className="font-semibold text-lg mt-4 flex items-center gap-2">
                        <ShieldCheck className="h-4 w-4" />
                        {t('webhooksGuide.howItWorks', 'How It Works')}
                    </h3>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>{t('webhooksGuide.inboundStep1', 'External service sends a POST with payload to /api/inbound/{provider}')}</li>
                        <li>{t('webhooksGuide.inboundStep2', 'InboundWebhookRouter looks up the handler for that provider')}</li>
                        <li>{t('webhooksGuide.inboundStep3', 'Handler validates the request signature (HMAC, shared secret, etc.)')}</li>
                        <li>{t('webhooksGuide.inboundStep4', 'Handler maps the payload to a ShipFlow action (create bug, create task, etc.)')}</li>
                        <li>{t('webhooksGuide.inboundStep5', 'Controller returns HTTP 200 (success), 401 (invalid signature), or 404 (unknown provider)')}</li>
                    </ol>

                    <h3 className="font-semibold text-lg mt-4 flex items-center gap-2">
                        <List className="h-4 w-4" />
                        {t('webhooksGuide.eventHeaders', 'Auto-Detected Event Headers')}
                    </h3>
                    <p className="text-sm text-muted-foreground mb-2">
                        {t('webhooksGuide.eventHeadersDesc', 'ShipFlow automatically detects the event type from common provider headers:')}
                    </p>
                    <div className="grid gap-2 sm:grid-cols-2 text-sm">
                        {[
                            { header: 'X-Event-Type', provider: 'Generic' },
                            { header: 'X-GitHub-Event', provider: 'GitHub' },
                            { header: 'X-Intercom-Event', provider: 'Intercom' },
                            { header: 'X-Hook-Event', provider: 'Hook systems' },
                            { header: 'X-PagerDuty-Event', provider: 'PagerDuty' },
                            { header: 'X-GitLab-Event', provider: 'GitLab' },
                            { header: 'X-Linear-Event', provider: 'Linear' },
                        ].map(({ header, provider }) => (
                            <div key={header} className="p-2 rounded border bg-muted/30 font-mono text-xs">
                                <strong>{header}</strong> <span className="text-muted-foreground">— {provider}</span>
                            </div>
                        ))}
                    </div>

                    <h3 className="font-semibold text-lg mt-4">{t('webhooksGuide.listProviders', 'Listing Active Providers')}</h3>
                    <div className="rounded-lg bg-muted p-4 font-mono text-sm">
                        <p><strong>GET</strong> /api/inbound</p>
                        <p className="text-muted-foreground mt-1">{t('webhooksGuide.listProvidersDesc', 'Returns a JSON list of active inbound provider names.')}</p>
                    </div>
                </CardContent>
            </Card>

            {/* External Providers */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Zap className="h-5 w-5" />
                        {t('webhooksGuide.providers', 'Pluggable Provider Architecture')}
                    </CardTitle>
                    <CardDescription>{t('webhooksGuide.providersDesc', 'Vendor-agnostic VCS, Notification, and Inbound Webhook systems')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('webhooksGuide.providersIntro', 'ShipFlow uses abstract interfaces for all external integrations. Implement the matching interface as a Spring @Component and it auto-registers — zero changes to existing code.')}
                    </p>
                    <div className="grid gap-2 sm:grid-cols-3 text-sm mt-4">
                        <div className="p-3 rounded-lg border bg-muted/30">
                            <strong>{t('webhooksGuide.vcsLabel', 'Version Control')}:</strong>
                            <p className="text-muted-foreground text-xs mt-1">VCSProvider — GitHub (built-in), GitLab, Bitbucket</p>
                        </div>
                        <div className="p-3 rounded-lg border bg-muted/30">
                            <strong>{t('webhooksGuide.notifLabel', 'Notifications')}:</strong>
                            <p className="text-muted-foreground text-xs mt-1">NotificationProvider — Slack (built-in), Teams, Discord</p>
                        </div>
                        <div className="p-3 rounded-lg border bg-primary/5 border-primary/20">
                            <strong>{t('webhooksGuide.inboundLabel', 'Inbound Events')}:</strong>
                            <p className="text-muted-foreground text-xs mt-1">InboundWebhookHandler — Any service via /api/inbound/*</p>
                        </div>
                    </div>
                </CardContent>
            </Card>

        </div>
    );
}
