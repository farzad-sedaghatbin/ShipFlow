import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft,
    Layers,
    Plug,
    Upload,
    Settings,
    CheckCircle2,
    AlertCircle,
    ArrowRight,
    Users,
    FolderInput,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function MigrationGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.migration.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.migration.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Why Migrate */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FolderInput className="h-5 w-5" />
                        {t('guides.migration.whyTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.migration.whyDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.migration.whyBody')}</p>
                    <ul className="space-y-2">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span className="text-sm">{t('guides.migration.whyItem1')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span className="text-sm">{t('guides.migration.whyItem2')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span className="text-sm">{t('guides.migration.whyItem3')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <span className="text-sm">{t('guides.migration.whyItem4')}</span>
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* From Jira */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Layers className="h-5 w-5" />
                        {t('guides.migration.jiraTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.migration.jiraDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    {/* Option 1: CSV */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.migration.jiraCsvTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                            <li><MarkdownInline content={t('guides.migration.jiraCsvStep1')} /></li>
                            <li><MarkdownInline content={t('guides.migration.jiraCsvStep2')} /></li>
                            <li><MarkdownInline content={t('guides.migration.jiraCsvStep3')} /></li>
                            <li><MarkdownInline content={t('guides.migration.jiraCsvStep4')} /></li>
                        </ol>
                    </div>

                    {/* Option 2: API */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.migration.jiraApiTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                            <li><MarkdownInline content={t('guides.migration.jiraApiStep1')} /></li>
                            <li><MarkdownInline content={t('guides.migration.jiraApiStep2')} /></li>
                            <li><MarkdownInline content={t('guides.migration.jiraApiStep3')} /></li>
                        </ol>
                    </div>

                    {/* Field mapping */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.migration.jiraMappingTitle')}</h4>
                        <div className="rounded-lg border overflow-hidden">
                            <table className="w-full text-sm">
                                <thead>
                                    <tr className="bg-muted/50 border-b">
                                        <th className="text-left p-3 font-medium">{t('guides.migration.jiraConcept')}</th>
                                        <th className="text-left p-3 font-medium">{t('guides.migration.shipFlowConcept')}</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y">
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Epic</td>
                                        <td className="p-3 font-medium">Pitch / Epic</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Sprint</td>
                                        <td className="p-3 font-medium">Cycle</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Issue (Story / Task / Bug)</td>
                                        <td className="p-3 font-medium">Task</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Project</td>
                                        <td className="p-3 font-medium">Project (Kanban)</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    {/* Gotchas */}
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-100 mb-2 flex items-center gap-2">
                            <AlertCircle className="h-4 w-4" />
                            {t('guides.migration.jiraGotchasTitle')}
                        </h4>
                        <ul className="space-y-1 text-sm text-amber-800 dark:text-amber-200">
                            <li>• {t('guides.migration.jiraGotcha1')}</li>
                            <li>• {t('guides.migration.jiraGotcha2')}</li>
                            <li>• {t('guides.migration.jiraGotcha3')}</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* From Linear */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Plug className="h-5 w-5" />
                        {t('guides.migration.linearTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.migration.linearDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.migration.linearBody')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li><MarkdownInline content={t('guides.migration.linearStep1')} /></li>
                        <li><MarkdownInline content={t('guides.migration.linearStep2')} /></li>
                        <li><MarkdownInline content={t('guides.migration.linearStep3')} /></li>
                        <li><MarkdownInline content={t('guides.migration.linearStep4')} /></li>
                    </ol>
                    <div className="rounded-lg border overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-muted/50 border-b">
                                    <th className="text-left p-3 font-medium">{t('guides.migration.linearConcept')}</th>
                                    <th className="text-left p-3 font-medium">{t('guides.migration.shipFlowConcept')}</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                <tr>
                                    <td className="p-3 text-muted-foreground">Issue</td>
                                    <td className="p-3 font-medium">Task</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Cycle</td>
                                    <td className="p-3 font-medium">Cycle</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Project</td>
                                    <td className="p-3 font-medium">Epic</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Team</td>
                                    <td className="p-3 font-medium">Project (Kanban)</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.migration.linearNote')}</strong> {t('guides.migration.linearNoteDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* From Asana */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Upload className="h-5 w-5" />
                        {t('guides.migration.asanaTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.migration.asanaDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.migration.asanaBody')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li><MarkdownInline content={t('guides.migration.asanaStep1')} /></li>
                        <li><MarkdownInline content={t('guides.migration.asanaStep2')} /></li>
                        <li><MarkdownInline content={t('guides.migration.asanaStep3')} /></li>
                        <li><MarkdownInline content={t('guides.migration.asanaStep4')} /></li>
                    </ol>
                    <div className="rounded-lg border overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="bg-muted/50 border-b">
                                    <th className="text-left p-3 font-medium">{t('guides.migration.asanaConcept')}</th>
                                    <th className="text-left p-3 font-medium">{t('guides.migration.shipFlowConcept')}</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y">
                                <tr>
                                    <td className="p-3 text-muted-foreground">Task</td>
                                    <td className="p-3 font-medium">Task</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Project</td>
                                    <td className="p-3 font-medium">Project (Kanban)</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Section</td>
                                    <td className="p-3 font-medium">Kanban Column / Status</td>
                                </tr>
                                <tr>
                                    <td className="p-3 text-muted-foreground">Milestone</td>
                                    <td className="p-3 font-medium">Epic</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.migration.asanaNote')}</strong> {t('guides.migration.asanaNoteDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* After Import */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Settings className="h-5 w-5" />
                        {t('guides.migration.afterTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.migration.afterDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.migration.afterBody')}</p>
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <Users className="h-4 w-4 text-blue-500" />
                                {t('guides.migration.afterTeamTitle')}
                            </h4>
                            <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                                <li>{t('guides.migration.afterTeamStep1')}</li>
                                <li>{t('guides.migration.afterTeamStep2')}</li>
                                <li>{t('guides.migration.afterTeamStep3')}</li>
                            </ol>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.migration.afterMethodologyTitle')}</h4>
                            <p className="text-sm text-muted-foreground">{t('guides.migration.afterMethodologyDesc')}</p>
                            <ul className="space-y-1 text-sm mt-2 ml-2">
                                <li>• <strong>Kanban</strong> — {t('guides.migration.afterMethodologyKanban')}</li>
                                <li>• <strong>Scrum</strong> — {t('guides.migration.afterMethodologyScrum')}</li>
                                <li>• <strong>Shape Up</strong> — {t('guides.migration.afterMethodologyShapeUp')}</li>
                            </ul>
                        </div>
                    </div>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.migration.afterNote')}</strong> {t('guides.migration.afterNoteDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Related Guides */}
            <Card className="bg-gradient-to-br from-primary/5 to-primary/10 border-primary/20">
                <CardHeader>
                    <CardTitle>{t('guides.relatedGuides')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/import">
                            {t('helpGuides.importData')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/project-types">
                            {t('helpGuides.projectTypes')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                    <Button asChild variant="outline" className="w-full justify-between">
                        <Link to="/help/scrum-mode">
                            {t('helpGuides.scrumMode')}
                            <ArrowRight className="h-4 w-4" />
                        </Link>
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
