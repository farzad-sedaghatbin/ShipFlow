import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, CheckSquare, Bug, Sparkles, Beaker } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function QATestingGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.qaTesting.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.qaTesting.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Beaker className="h-5 w-5" />
                        {t('guides.qaTesting.integratedTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.qaTesting.integratedDesc')}</p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.qaTesting.bestPractice')}</strong> {t('guides.qaTesting.bestPracticeDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Test Cases */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <CheckSquare className="h-5 w-5" />
                        {t('guides.qaTesting.testCasesTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.qaTesting.testCasesSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li><MarkdownInline content={t('guides.qaTesting.testCasesStep1')} /></li>
                        <li><MarkdownInline content={t('guides.qaTesting.testCasesStep2')} /></li>
                        <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3')} />
                            <ul className="list-disc list-inside ml-6 mt-2 space-y-1 text-sm text-muted-foreground">
                                <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3a')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3b')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3c')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3d')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.testCasesStep3e')} /></li>
                            </ul>
                        </li>
                        <li><MarkdownInline content={t('guides.qaTesting.testCasesStep4')} /></li>
                    </ol>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/qa-test-cases.png"
                            alt="Test Cases list view showing status and priorities"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Test Cases List
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* AI Generation */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Sparkles className="h-5 w-5 text-purple-500" />
                        {t('guides.qaTesting.aiGenerationTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.qaTesting.aiGenerationSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.qaTesting.aiGenerationDesc')}
                    </p>
                    <div className="grid gap-4 md:grid-cols-2">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2">{t('guides.qaTesting.aiHowToUseTitle')}</h4>
                            <ol className="list-decimal list-inside space-y-2 text-sm">
                                <li><MarkdownInline content={t('guides.qaTesting.aiHowToStep1')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.aiHowToStep2')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.aiHowToStep3')} /></li>
                                <li><MarkdownInline content={t('guides.qaTesting.aiHowToStep4')} /></li>
                            </ol>
                        </div>
                        <div className="rounded-lg bg-purple-50 dark:bg-purple-950/20 border border-purple-200 dark:border-purple-900 p-4">
                            <h4 className="font-semibold text-purple-900 dark:text-purple-100 mb-2">{t('guides.qaTesting.aiWhyUseTitle')}</h4>
                            <ul className="list-disc list-inside space-y-1 text-sm text-purple-800 dark:text-purple-200">
                                <li>{t('guides.qaTesting.aiWhyItem1')}</li>
                                <li>{t('guides.qaTesting.aiWhyItem2')}</li>
                                <li>{t('guides.qaTesting.aiWhyItem3')}</li>
                            </ul>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Bug Tracking */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Bug className="h-5 w-5" />
                        {t('guides.qaTesting.bugReportsTitle')}
                    </CardTitle>
                    <CardDescription>{t('guides.qaTesting.bugReportsSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        {t('guides.qaTesting.bugReportsDesc')}
                    </p>
                    <ul className="list-disc list-inside space-y-2 text-sm">
                        <li><MarkdownInline content={t('guides.qaTesting.bugSeverity')} /></li>
                        <li><MarkdownInline content={t('guides.qaTesting.bugStatus')} /></li>
                    </ul>

                    <div className="rounded-lg border bg-muted/50 p-4 mt-4">
                        <img
                            src="/guides/qa-bug-reports.png"
                            alt="Bug Reports dashboard"
                            className="w-full rounded-lg shadow-md"
                            onError={(e) => {
                                e.currentTarget.style.display = 'none';
                                e.currentTarget.nextElementSibling?.classList.remove('hidden');
                            }}
                        />
                        <div className="hidden text-center text-sm text-muted-foreground py-8">
                            Screenshot: Bug Reports List
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
