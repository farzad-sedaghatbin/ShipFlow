import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft,
    Zap,
    Play,
    ArrowRight,
    CheckCircle2,
    AlertCircle,
    History,
    LayoutTemplate,
    Settings2,
    Webhook,
    Bell,
    MessageSquare,
    ListChecks,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';

export default function WorkflowAutomationsGuide() {
    const { t } = useTranslation();

    const triggers = [
        { name: 'Task Created', desc: 'A new task is added to the project' },
        { name: 'Task Status Changed', desc: 'A task moves to any new status' },
        { name: 'Task Assigned', desc: 'A task is assigned to a user' },
        { name: 'Task Completed', desc: 'A task transitions to DONE/COMPLETED' },
        { name: 'Pitch Created', desc: 'A new pitch is added' },
        { name: 'Pitch Status Changed', desc: 'A pitch moves between lifecycle stages' },
        { name: 'Cycle Started', desc: 'A cycle transitions to IN_PROGRESS' },
        { name: 'Cycle Ended', desc: 'A cycle is marked COMPLETED or CANCELLED' },
        { name: 'Cycle Status Changed', desc: 'Any cycle status transition' },
        { name: 'Comment Added', desc: 'A comment is posted on any task or pitch' },
        { name: 'Betting Table Locked', desc: 'The betting table is locked for the cycle (Shape Up)', badge: 'Shape Up' },
        { name: 'Hill Chart Moved', desc: 'Any scope dot is dragged on the hill chart (Shape Up)', badge: 'Shape Up' },
        { name: 'Appetite Exceeded', desc: 'Actual hours exceed the pitch appetite (Shape Up)', badge: 'Shape Up' },
        { name: 'Scope Creep Detected', desc: 'Tasks added to a cycle after it started (Shape Up)', badge: 'Shape Up' },
    ];

    const actions = [
        { icon: Bell, name: 'Notify Assignee', desc: 'Sends an in-app notification to the task assignee' },
        { icon: Bell, name: 'Notify Project Members', desc: 'Sends an in-app notification to all project members' },
        { icon: Webhook, name: 'Send Webhook', desc: 'POSTs the event payload to a URL you provide' },
        { icon: MessageSquare, name: 'Send Email', desc: 'Sends an email notification (requires SMTP in Org Settings)' },
        { icon: MessageSquare, name: 'Add Comment', desc: 'Posts an automatic comment on the triggering task or pitch' },
        { icon: Settings2, name: 'Change Task Status', desc: 'Updates the task status to a value you specify' },
        { icon: ListChecks, name: 'Create Task', desc: 'Creates a new task in the project' },
    ];

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
                <h1 className="text-4xl font-bold tracking-tight">Workflow Automations</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Define trigger-based rules that react automatically to events in your project — no code required.
                </p>
            </div>

            <Separator />

            {/* Getting started callout */}
            <div className="rounded-lg bg-primary/5 border border-primary/20 p-4">
                <div className="flex items-start gap-3">
                    <Zap className="h-5 w-5 text-primary mt-0.5 shrink-0" />
                    <div className="space-y-1">
                        <p className="text-sm font-semibold">Where to find it</p>
                        <p className="text-sm text-muted-foreground">
                            Click <strong>Automations</strong> (⚡) in the sidebar. You must have a project selected.
                            Project Managers and Admins can create, edit, and delete rules. Developers and Viewers can
                            view rules and execution history.
                        </p>
                    </div>
                </div>
            </div>

            {/* How it works */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Play className="h-5 w-5 text-primary" />
                        How it works
                    </CardTitle>
                    <CardDescription>Every automation rule has two parts</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="flex items-start gap-4">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-bold shrink-0">1</div>
                        <div>
                            <p className="font-semibold">Trigger</p>
                            <p className="text-sm text-muted-foreground">The project event that starts the rule — e.g. a task is completed, a cycle starts, scope creep is detected.</p>
                        </div>
                    </div>
                    <div className="flex items-start gap-4">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-bold shrink-0">2</div>
                        <div>
                            <p className="font-semibold">Action</p>
                            <p className="text-sm text-muted-foreground">What happens when the trigger fires — e.g. notify the team, send a webhook, add a comment. Runs asynchronously and never slows down the triggering action.</p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Creating your first rule */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LayoutTemplate className="h-5 w-5 text-primary" />
                        Creating your first rule
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="flex items-start gap-3">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 shrink-0" />
                            <p className="text-sm"><strong>From a template</strong> — click <strong>Browse Templates</strong> in the top-right. Choose a category (Tasks, Shape Up, Automation, Notifications), find a template, and click <strong>Use Template</strong>. The rule is added instantly and can be customised.</p>
                        </div>
                        <div className="flex items-start gap-3">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 shrink-0" />
                            <p className="text-sm"><strong>From scratch</strong> — click <strong>New Rule</strong>, choose a trigger type and action type, optionally fill in the action config (webhook URL, message, etc.), and save.</p>
                        </div>
                    </div>

                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-3">
                        <p className="text-sm text-amber-800 dark:text-amber-200">
                            <strong>20 built-in templates</strong> are available out of the box — covering task notifications, Shape Up-specific events, webhooks, and more. Start there before building from scratch.
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Trigger types */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Play className="h-5 w-5 text-primary" />
                        Trigger types
                    </CardTitle>
                    <CardDescription>14 triggers available — 4 are unique to Shape Up projects</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="space-y-2">
                        {triggers.map((trigger) => (
                            <div key={trigger.name} className="flex items-start gap-3 py-2 border-b last:border-b-0">
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 flex-wrap">
                                        <span className="text-sm font-medium">{trigger.name}</span>
                                        {trigger.badge && (
                                            <Badge variant="secondary" className="text-xs">{trigger.badge}</Badge>
                                        )}
                                    </div>
                                    <p className="text-xs text-muted-foreground mt-0.5">{trigger.desc}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* Action types */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Zap className="h-5 w-5 text-primary" />
                        Action types
                    </CardTitle>
                    <CardDescription>7 actions available</CardDescription>
                </CardHeader>
                <CardContent>
                    <div className="grid gap-3 sm:grid-cols-2">
                        {actions.map((action) => {
                            const Icon = action.icon;
                            return (
                                <div key={action.name} className="flex items-start gap-3 rounded-lg border p-3">
                                    <Icon className="h-4 w-4 text-primary mt-0.5 shrink-0" />
                                    <div>
                                        <p className="text-sm font-medium">{action.name}</p>
                                        <p className="text-xs text-muted-foreground mt-0.5">{action.desc}</p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </CardContent>
            </Card>

            {/* Placeholder interpolation */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Settings2 className="h-5 w-5 text-primary" />
                        Dynamic message templates
                    </CardTitle>
                    <CardDescription>Inject event context into messages and webhook payloads using {'{{key}}'} placeholders</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="rounded-lg bg-muted p-3 font-mono text-sm">
                        Task <span className="text-primary">{'{{taskName}}'}</span> was completed by <span className="text-primary">{'{{assignee}}'}</span> in <span className="text-primary">{'{{projectName}}'}</span>
                    </div>
                    <div className="grid gap-2 sm:grid-cols-2 text-sm">
                        {[
                            ['{{taskName}}', 'Name of the triggering task'],
                            ['{{assignee}}', 'Username of the assignee'],
                            ['{{status}}', 'New status value'],
                            ['{{projectName}}', 'Project name'],
                            ['{{cycleName}}', 'Cycle name (cycle triggers)'],
                            ['{{pitchName}}', 'Pitch name (pitch triggers)'],
                        ].map(([key, desc]) => (
                            <div key={key} className="flex items-center gap-2">
                                <code className="rounded bg-muted px-1 py-0.5 text-xs text-primary">{key}</code>
                                <span className="text-muted-foreground">{desc}</span>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* Execution history */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <History className="h-5 w-5 text-primary" />
                        Execution History
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <p className="text-sm text-muted-foreground">Every automation run is logged with status, trigger payload, result message, and timestamp.</p>
                    <div className="space-y-2">
                        <div className="flex items-center gap-2">
                            <Badge className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 hover:bg-green-100">SUCCESS</Badge>
                            <span className="text-sm text-muted-foreground">Action completed normally</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Badge className="bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200 hover:bg-red-100">FAILURE</Badge>
                            <span className="text-sm text-muted-foreground">Action failed — check the result message for the reason</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <Badge className="bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200 hover:bg-yellow-100">SKIPPED</Badge>
                            <span className="text-sm text-muted-foreground">Trigger fired but the rule was disabled or conditions not met</span>
                        </div>
                    </div>
                    <p className="text-sm text-muted-foreground">
                        View logs two ways: click the ⏱ icon on any rule card for that rule's history, or open the <strong>Execution History</strong> tab for a project-wide view of all runs.
                    </p>
                </CardContent>
            </Card>

            {/* FAQ */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <AlertCircle className="h-5 w-5 text-primary" />
                        Common questions
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    {[
                        {
                            q: "Why isn't my automation firing?",
                            a: "Check that the rule is enabled (toggle is on) and the correct project is selected. Look at the Execution History for FAILURE or SKIPPED entries — they include the reason.",
                        },
                        {
                            q: "Can I test a rule without the real trigger?",
                            a: "Not directly from the UI. Perform the triggering action manually (e.g. change a task to DONE to test a Task Completed rule), then check Execution History.",
                        },
                        {
                            q: "What happens when a webhook action fails?",
                            a: "A FAILURE entry is logged with the HTTP status and error. Automations don't retry automatically — fix the URL and the next real trigger will try again.",
                        },
                        {
                            q: "Can rules trigger other rules (chaining)?",
                            a: "No — actions taken by an automation don't themselves trigger other automations. This prevents infinite loops.",
                        },
                    ].map((item) => (
                        <div key={item.q} className="space-y-1">
                            <p className="text-sm font-semibold">{item.q}</p>
                            <p className="text-sm text-muted-foreground">{item.a}</p>
                        </div>
                    ))}
                </CardContent>
            </Card>

            {/* Next steps */}
            <Card>
                <CardHeader>
                    <CardTitle>Related guides</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    {[
                        { label: 'Webhooks', path: '/help/webhooks' },
                        { label: 'Hill Charts', path: '/help/hill-charts' },
                        { label: 'Setting Up Your First Cycle', path: '/help/cycle-setup' },
                    ].map((link) => (
                        <Button key={link.path} asChild variant="ghost" className="w-full justify-between">
                            <Link to={link.path}>
                                {link.label}
                                <ArrowRight className="h-4 w-4" />
                            </Link>
                        </Button>
                    ))}
                </CardContent>
            </Card>
        </div>
    );
}
