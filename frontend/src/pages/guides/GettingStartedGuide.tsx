import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, LogIn, LayoutDashboard, Folder, CheckCircle2 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function GettingStartedGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.gettingStarted.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.gettingStarted.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LogIn className="h-5 w-5" />
                        {t('guides.gettingStarted.welcome')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.gettingStarted.intro')}
                    </p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.gettingStarted.whatIsShapeUp')}</strong> {t('guides.gettingStarted.shapeUpDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Step 1: Logging In */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.gettingStarted.step1Title')}</CardTitle>
                    <CardDescription>{t('guides.gettingStarted.step1Subtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Navigate to the ShipFlow login page</li>
                        <li>Enter your username and password</li>
                        <li>Click "Sign In" to access your workspace</li>
                    </ol>
                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/login.png"
                            alt="ShipFlow login page showing username and password fields"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Login page
                        </div>
                    </div>
                    <p className="text-sm text-muted-foreground">
                        <strong>{t('guides.gettingStarted.firstTime')}</strong> {t('guides.gettingStarted.firstTimeDesc')}
                    </p>
                </CardContent>
            </Card>

            {/* Step 2: Understanding the Dashboard */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <LayoutDashboard className="h-5 w-5" />
                        Step 2: Understanding the Dashboard
                    </CardTitle>
                    <CardDescription>Your central hub for project insights</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        After logging in, you'll land on the <strong>Dashboard</strong>. This is your command center
                        where you can see:
                    </p>
                    <ul className="space-y-2 ml-6">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Active Cycles:</strong> Current work cycles and their progress</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Recent Activity:</strong> Latest updates from your team</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Quick Actions:</strong> Shortcuts to common tasks</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span><strong>Health Metrics:</strong> Overview of project health and risks</span>
                        </li>
                    </ul>
                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/dashboard.png"
                            alt="ShipFlow dashboard showing active cycles, recent activity, and metrics"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Dashboard view
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Step 3: Navigating the Interface */}
            <Card>
                <CardHeader>
                    <CardTitle>Step 3: Navigating the Interface</CardTitle>
                    <CardDescription>Explore the main sections of ShipFlow</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The sidebar on the left contains all main navigation items organized into sections:
                    </p>

                    <div className="space-y-4">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">📊 Overview</h4>
                            <ul className="space-y-1 text-sm ml-4">
                                <li><strong>Dashboard:</strong> Your main overview</li>
                                <li><strong>Projects:</strong> All your projects in one place</li>
                                <li><strong>Cycles:</strong> Manage work cycles</li>
                            </ul>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">🎯 Cycle Workspace</h4>
                            <ul className="space-y-1 text-sm ml-4">
                                <li><strong>Pitch Board:</strong> View and manage pitches</li>
                                <li><strong>Betting Table:</strong> Make cycle commitments</li>
                                <li><strong>Health Overview:</strong> Monitor project health</li>
                                <li><strong>Retrospectives:</strong> Reflect on completed cycles</li>
                                <li><strong>Reports:</strong> Analytics and insights</li>
                            </ul>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">📋 Work Management</h4>
                            <ul className="space-y-1 text-sm ml-4">
                                <li><strong>Backlog:</strong> Manage tasks and technical debt</li>
                                <li><strong>Work Logs:</strong> Track time and effort</li>
                                <li><strong>Meetings:</strong> Schedule and document meetings</li>
                            </ul>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">✅ Quality</h4>
                            <ul className="space-y-1 text-sm ml-4">
                                <li><strong>Test Cases:</strong> Manage QA test cases</li>
                                <li><strong>Bug Reports:</strong> Track and resolve bugs</li>
                            </ul>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Step 4: Creating Your First Project */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Folder className="h-5 w-5" />
                        Step 4: Creating Your First Project
                    </CardTitle>
                    <CardDescription>Set up a project to organize your work</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        Projects help you organize related cycles and pitches. Here's how to create one:
                    </p>
                    <ol className="list-decimal list-inside space-y-3">
                        <li>Click on <strong>Projects</strong> in the sidebar</li>
                        <li>Click the <strong>"New Project"</strong> button</li>
                        <li>Enter a project name and description</li>
                        <li>Set a project key (e.g., "SHOP" for an e-commerce project)</li>
                        <li>Click <strong>"Create Project"</strong></li>
                    </ol>
                    <div className="rounded-lg border bg-muted/50 p-4">
                        <img
                            src="/guides/projects-list.png"
                            alt="Projects page showing list of projects with create button"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Projects list
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Next Steps */}
            <Card className="bg-gradient-to-br from-primary/5 to-primary/10 border-primary/20">
                <CardHeader>
                    <CardTitle>Next Steps</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <p>Now that you're familiar with the basics, explore these guides:</p>
                    <div className="grid gap-2">
                        <Button asChild variant="outline" className="justify-start">
                            <Link to="/help/cycle-setup">
                                📅 Setting Up Your First Cycle
                            </Link>
                        </Button>
                        <Button asChild variant="outline" className="justify-start">
                            <Link to="/help/hill-charts">
                                📈 Understanding Hill Charts
                            </Link>
                        </Button>
                        <Button asChild variant="outline" className="justify-start">
                            <Link to="/help/betting-meeting">
                                🎲 Running a Betting Meeting
                            </Link>
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
