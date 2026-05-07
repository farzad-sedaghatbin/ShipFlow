import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, FileText, Database } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function ExportDataGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.exportData.title', 'Export Data')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.exportData.subtitle', 'Learn how to export cycle summaries and extract your data from ShipFlow.')}
                </p>
            </div>

            <Separator />

            {/* Cycle Summaries */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FileText className="h-5 w-5" />
                        {t('guides.exportData.cycleSummaryTitle', 'Cycle Summary Export')}
                    </CardTitle>
                    <CardDescription>{t('guides.exportData.cycleSummaryDesc', 'Export AI-generated narratives for stakeholders')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.exportData.cycleSummaryBody', 'ShipFlow automatically generates cycle narratives that summarize everything your team accomplished during a 6-week cycle. You can export these as clean Markdown files to share in company wikis, emails, or update posts.')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li><MarkdownInline content={t('guides.exportData.cycleSummaryStep1', 'Navigate to the **Dashboard** or a specific **Cycle** view.')} /></li>
                        <li><MarkdownInline content={t('guides.exportData.cycleSummaryStep2', 'Find the **Cycle Summary** panel.')} /></li>
                        <li><MarkdownInline content={t('guides.exportData.cycleSummaryStep3', 'Click the **Download** icon (or Export to Markdown) at the top right of the narrative.')} /></li>
                        <li><MarkdownInline content={t('guides.exportData.cycleSummaryStep4', 'A `.md` file will be generated immediately containing the structured summary.')} /></li>
                    </ol>
                </CardContent>
            </Card>

            {/* General Data Portability */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Database className="h-5 w-5" />
                        {t('guides.exportData.portabilityTitle', 'Data Portability')}
                    </CardTitle>
                    <CardDescription>{t('guides.exportData.portabilityDesc', 'Accessing your raw data')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.exportData.portabilityBody', 'All your data in ShipFlow belongs to you. To extract raw data for backup or migration purposes:')}</p>
                    <ul className="list-disc list-inside space-y-2 ml-2">
                        <li><MarkdownInline content={t('guides.exportData.portabilityItem1', '**Public REST API:** Use the REST API to bulk export pitches, cycles, and tasks in JSON format.')} /></li>
                        <li><MarkdownInline content={t('guides.exportData.portabilityItem2', '**Database Access:** If you are self-hosting, you have direct access to the PostgreSQL database for complete pg_dump backups.')} /></li>
                    </ul>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4 mt-2">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.exportData.portabilityNoteLabel', '💡 Note:')}</strong>{' '}
                            {t('guides.exportData.portabilityNote', 'Built-in CSV export for tables is planned for a future release to make raw data extraction even easier.')}
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
