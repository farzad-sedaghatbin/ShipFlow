import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, FileText, Database } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

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
                <h1 className="text-4xl font-bold tracking-tight">{t('helpGuides.exportData', 'Export Data')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('helpGuides.exportDataDesc', 'Learn how to export cycle summaries and extract your data from ShipFlow.')}
                </p>
            </div>

            <Separator />

            {/* Cycle Summaries */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <FileText className="h-5 w-5" />
                        Cycle Summary Export
                    </CardTitle>
                    <CardDescription>Export AI-generated narratives for stakeholders</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow automatically generates cycle narratives that summarize everything your team accomplished during a 6-week cycle. You can export these as clean Markdown files to share in company wikis, emails, or update posts.
                    </p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>Navigate to the <strong>Dashboard</strong> or a specific <strong>Cycle</strong> view.</li>
                        <li>Find the <strong>Cycle Summary</strong> panel.</li>
                        <li>Click the <strong>Download</strong> icon (or Export to Markdown) at the top right of the narrative.</li>
                        <li>A `.md` file will be generated immediately containing the structured summary.</li>
                    </ol>
                </CardContent>
            </Card>

            {/* General Data Portability */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Database className="h-5 w-5" />
                        Data Portability
                    </CardTitle>
                    <CardDescription>Accessing your raw data</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        All your data in ShipFlow belongs to you. To extract raw data for backup or migration purposes:
                    </p>
                    <ul className="list-disc list-inside space-y-2 ml-2">
                        <li><strong>Public REST API:</strong> Use the REST API to bulk export pitches, cycles, and tasks in JSON format.</li>
                        <li><strong>Database Access:</strong> If you are self-hosting, you have direct access to the PostgreSQL database for complete pg_dump backups.</li>
                    </ul>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4 mt-2">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>💡 Note:</strong> Built-in CSV export for tables is planned for a future release to make raw data extraction even easier.
                        </p>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
