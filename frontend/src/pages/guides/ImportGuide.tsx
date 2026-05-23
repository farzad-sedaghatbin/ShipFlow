import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft,
    Upload,
    FolderInput,
    Plug,
    Layers,
    Workflow,
    AlertCircle,
    CheckCircle2,
    ArrowRight,
    RefreshCw,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function ImportGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.import.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.import.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Overview */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FolderInput className="h-5 w-5" />
                        {t('guides.import.overviewTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.overviewDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.import.overviewBody')}</p>
                    <div className="grid gap-4 md:grid-cols-2">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <Upload className="h-4 w-4 text-blue-500" />
                                {t('guides.import.csvOptionTitle')}
                            </h4>
                            <p className="text-sm text-muted-foreground">{t('guides.import.csvOptionDesc')}</p>
                        </div>
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <Plug className="h-4 w-4 text-green-500" />
                                {t('guides.import.apiOptionTitle')}
                            </h4>
                            <p className="text-sm text-muted-foreground">{t('guides.import.apiOptionDesc')}</p>
                        </div>
                    </div>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.import.overviewNote')}</strong> {t('guides.import.overviewNoteDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* CSV Import Walkthrough */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Upload className="h-5 w-5" />
                        {t('guides.import.csvTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.csvDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-6">
                    {/* Export from Jira */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.import.jiraExportTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                            <li><MarkdownInline content={t('guides.import.jiraExportStep1')} /></li>
                            <li><MarkdownInline content={t('guides.import.jiraExportStep2')} /></li>
                            <li><MarkdownInline content={t('guides.import.jiraExportStep3')} /></li>
                            <li><MarkdownInline content={t('guides.import.jiraExportStep4')} /></li>
                        </ol>
                    </div>

                    <Separator />

                    {/* Export from Linear */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.import.linearExportTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                            <li><MarkdownInline content={t('guides.import.linearExportStep1')} /></li>
                            <li><MarkdownInline content={t('guides.import.linearExportStep2')} /></li>
                            <li><MarkdownInline content={t('guides.import.linearExportStep3')} /></li>
                        </ol>
                    </div>

                    <Separator />

                    {/* Export from Asana */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.import.asanaExportTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                            <li><MarkdownInline content={t('guides.import.asanaExportStep1')} /></li>
                            <li><MarkdownInline content={t('guides.import.asanaExportStep2')} /></li>
                            <li><MarkdownInline content={t('guides.import.asanaExportStep3')} /></li>
                        </ol>
                    </div>

                    <Separator />

                    {/* Upload Steps in ShipFlow */}
                    <div>
                        <h4 className="font-semibold mb-2">{t('guides.import.uploadStepsTitle')}</h4>
                        <ol className="list-decimal list-inside space-y-3 ml-2">
                            <li>
                                <strong>{t('guides.import.uploadStep1Label')}</strong>{' '}
                                {t('guides.import.uploadStep1Desc')}
                            </li>
                            <li>
                                <strong>{t('guides.import.uploadStep2Label')}</strong>{' '}
                                {t('guides.import.uploadStep2Desc')}
                            </li>
                            <li>
                                <strong>{t('guides.import.uploadStep3Label')}</strong>{' '}
                                {t('guides.import.uploadStep3Desc')}
                            </li>
                        </ol>
                    </div>

                    <Separator />

                    {/* Field Mapping */}
                    <div>
                        <h4 className="font-semibold mb-3">{t('guides.import.fieldMappingTitle')}</h4>
                        <div className="rounded-lg border overflow-hidden">
                            <table className="w-full text-sm">
                                <thead>
                                    <tr className="bg-muted/50 border-b">
                                        <th className="text-left p-3 font-medium">{t('guides.import.fieldMappingSource')}</th>
                                        <th className="text-left p-3 font-medium">{t('guides.import.fieldMappingShipFlow')}</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y">
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Epic / Initiative</td>
                                        <td className="p-3 font-medium">Pitch / Epic</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Sprint / Cycle</td>
                                        <td className="p-3 font-medium">Cycle</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Issue / Task / Card</td>
                                        <td className="p-3 font-medium">Task</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Priority</td>
                                        <td className="p-3 font-medium">Priority</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Status</td>
                                        <td className="p-3 font-medium">Status</td>
                                    </tr>
                                    <tr>
                                        <td className="p-3 text-muted-foreground">Assignee</td>
                                        <td className="p-3 font-medium">Assignee</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <p className="text-xs text-muted-foreground mt-2">{t('guides.import.fieldMappingNote')}</p>
                    </div>
                </CardContent>
            </Card>

            {/* Linear API Import */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Plug className="h-5 w-5" />
                        {t('guides.import.linearApiTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.linearApiDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.import.linearApiBody')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li><MarkdownInline content={t('guides.import.linearApiStep1')} /></li>
                        <li><MarkdownInline content={t('guides.import.linearApiStep2')} /></li>
                        <li><MarkdownInline content={t('guides.import.linearApiStep3')} /></li>
                        <li><MarkdownInline content={t('guides.import.linearApiStep4')} /></li>
                    </ol>
                    <div className="rounded-lg bg-muted/50 border p-4">
                        <h5 className="text-sm font-semibold mb-2">{t('guides.import.linearApiWhatTitle')}</h5>
                        <ul className="space-y-1 text-sm text-muted-foreground ml-2">
                            <li>• {t('guides.import.linearApiWhat1')}</li>
                            <li>• {t('guides.import.linearApiWhat2')}</li>
                            <li>• {t('guides.import.linearApiWhat3')}</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* Jira API Import */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Layers className="h-5 w-5" />
                        {t('guides.import.jiraApiTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.jiraApiDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.import.jiraApiBody')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2 text-sm">
                        <li><MarkdownInline content={t('guides.import.jiraApiStep1')} /></li>
                        <li><MarkdownInline content={t('guides.import.jiraApiStep2')} /></li>
                        <li><MarkdownInline content={t('guides.import.jiraApiStep3')} /></li>
                        <li><MarkdownInline content={t('guides.import.jiraApiStep4')} /></li>
                    </ol>
                    <div className="rounded-lg bg-muted/50 border p-4">
                        <h5 className="text-sm font-semibold mb-2">{t('guides.import.jiraApiWhatTitle')}</h5>
                        <ul className="space-y-1 text-sm text-muted-foreground ml-2">
                            <li>• {t('guides.import.jiraApiWhat1')}</li>
                            <li>• {t('guides.import.jiraApiWhat2')}</li>
                            <li>• {t('guides.import.jiraApiWhat3')}</li>
                        </ul>
                    </div>
                </CardContent>
            </Card>

            {/* Post-Import */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Workflow className="h-5 w-5" />
                        {t('guides.import.postImportTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.postImportDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.import.postImportBody')}</p>
                    <ul className="space-y-3">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.import.postImportItem1Title')}</strong>{' '}
                                {t('guides.import.postImportItem1Desc')}
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.import.postImportItem2Title')}</strong>{' '}
                                {t('guides.import.postImportItem2Desc')}
                            </div>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-5 w-5 text-green-500 mt-0.5 flex-shrink-0" />
                            <div>
                                <strong>{t('guides.import.postImportItem3Title')}</strong>{' '}
                                {t('guides.import.postImportItem3Desc')}
                            </div>
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Error Recovery */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <RefreshCw className="h-5 w-5" />
                        {t('guides.import.errorRecoveryTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.import.errorRecoveryDesc')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="rounded-lg border p-4">
                        <h4 className="font-semibold mb-2 flex items-center gap-2">
                            <AlertCircle className="h-4 w-4 text-amber-500" />
                            {t('guides.import.errorRowsTitle')}
                        </h4>
                        <p className="text-sm text-muted-foreground mb-2">{t('guides.import.errorRowsDesc')}</p>
                        <ol className="list-decimal list-inside space-y-1 text-sm ml-2">
                            <li>{t('guides.import.errorRowsStep1')}</li>
                            <li>{t('guides.import.errorRowsStep2')}</li>
                            <li>{t('guides.import.errorRowsStep3')}</li>
                        </ol>
                    </div>
                    <div className="rounded-lg border p-4">
                        <h4 className="font-semibold mb-2 flex items-center gap-2">
                            <AlertCircle className="h-4 w-4 text-red-500" />
                            {t('guides.import.errorApiTitle')}
                        </h4>
                        <p className="text-sm text-muted-foreground mb-2">{t('guides.import.errorApiDesc')}</p>
                        <ul className="space-y-1 text-sm ml-2">
                            <li>• {t('guides.import.errorApiItem1')}</li>
                            <li>• {t('guides.import.errorApiItem2')}</li>
                            <li>• {t('guides.import.errorApiItem3')}</li>
                        </ul>
                    </div>
                    <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 p-4">
                        <p className="text-sm text-amber-900 dark:text-amber-100">
                            <strong>{t('guides.import.errorTipLabel')}</strong> {t('guides.import.errorTipDesc')}
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
                        <Link to="/help/migration">
                            {t('helpGuides.migration')}
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
