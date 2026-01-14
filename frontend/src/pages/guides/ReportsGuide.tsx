import { Link } from 'react-router-dom';
import { ArrowLeft, LayoutDashboard, BarChart3, LineChart, PieChart } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function ReportsGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">Reports & Dashboards</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    Visualize your team's performance with custom dashboards and metrics
                </p>
            </div>

            <Separator />

            {/* Overview */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LayoutDashboard className="h-5 w-5" />
                        Dashboard Manager
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The Dashboard Manager allows you to create custom views of your project data. You can have multiple dashboards
                        for different purposes (e.g., "Executive Overview," "Team Velocity," "QA Metrics").
                    </p>
                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/dashboard-manager.png"
                            alt="Dashboard Manager interface showing available reports"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Dashboard Manager
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Cycle Reports */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <BarChart3 className="h-5 w-5" />
                        Cycle Reports
                    </CardTitle>
                    <CardDescription>Deep dive into cycle performance</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        Cycle reports provide standardized metrics for every 6-week cycle, including:
                    </p>
                    <ul className="grid gap-2 sm:grid-cols-2 text-sm">
                        <li className="flex items-center gap-2 p-2 rounded bg-muted/50">
                            <PieChart className="h-4 w-4 text-blue-500" />
                            <span>Scope Completion Rate</span>
                        </li>
                        <li className="flex items-center gap-2 p-2 rounded bg-muted/50">
                            <LineChart className="h-4 w-4 text-green-500" />
                            <span>Hill Chart Velocity</span>
                        </li>
                        <li className="flex items-center gap-2 p-2 rounded bg-muted/50">
                            <BarChart3 className="h-4 w-4 text-purple-500" />
                            <span>Team Capacity vs. Actual</span>
                        </li>
                    </ul>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/cycle-report.png"
                            alt="Detailed cycle report view"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Cycle Report
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Dashboard Templates */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LayoutDashboard className="h-5 w-5" />
                        Dashboard Templates
                    </CardTitle>
                    <CardDescription>Start quickly with pre-built reporting boards</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow comes with several default templates to help you get started without building from scratch.
                        Click <strong>"Use Template"</strong> on the main Reports page to browse:
                    </p>
                    <ul className="grid gap-2 sm:grid-cols-2 text-sm">
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Weekly Sync:</strong> Overview of active pitches and blockers for standups.
                        </li>
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Cycle Review:</strong> Comprehensive metrics for the end-of-cycle retrospective.
                        </li>
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Executive Summary:</strong> High-level view of project health and risks.
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Dashboard Templates */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LayoutDashboard className="h-5 w-5" />
                        Dashboard Templates
                    </CardTitle>
                    <CardDescription>Start quickly with pre-built reporting boards</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow comes with several default templates to help you get started without building from scratch.
                        Click <strong>"Use Template"</strong> on the main Reports page to browse:
                    </p>
                    <ul className="grid gap-2 sm:grid-cols-2 text-sm">
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Weekly Sync:</strong> Overview of active pitches and blockers for standups.
                        </li>
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Cycle Review:</strong> Comprehensive metrics for the end-of-cycle retrospective.
                        </li>
                        <li className="p-3 rounded-lg border bg-muted/30">
                            <strong>Executive Summary:</strong> High-level view of project health and risks.
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Custom Metrics */}
            <Card>
                <CardHeader>
                    <CardTitle>Customizing Your View</CardTitle>
                    <CardDescription>Build what you need</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-2">
                        <li>Go to <strong>Reports</strong></li>
                        <li>Click <strong>"New Report Board"</strong></li>
                        <li>Give it a name and optional description</li>
                        <li>Use <strong>"Add Widget"</strong> to select from available metric types</li>
                    </ol>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4 mt-2">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Tip:</strong> You can set any custom dashboard as your default view so it loads
                            automatically when you visit the Reports section.
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
