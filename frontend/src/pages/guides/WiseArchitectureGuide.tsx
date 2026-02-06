import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Brain, Code, Users, Palette, AlertCircle, CheckCircle2, Settings, Lightbulb, Sparkles } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

export default function WiseArchitectureGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.wiseArchitecture.title')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.wiseArchitecture.subtitle')}
                </p>
            </div>

            <Separator />

            {/* Experimental Feature Notice */}
            <Alert>
                <Sparkles className="h-4 w-4" />
                <AlertTitle>{t('guides.wiseArchitecture.experimentalTitle')}</AlertTitle>
                <AlertDescription>
                    {t('guides.wiseArchitecture.experimentalDesc')}
                </AlertDescription>
            </Alert>

            {/* Introduction */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Brain className="h-5 w-5" />
                        {t('guides.wiseArchitecture.whatTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.wiseArchitecture.whatDesc')}</p>
                    <div className="rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
                        <p className="text-sm text-blue-900 dark:text-blue-100">
                            <strong>{t('guides.wiseArchitecture.howWorks')}</strong> {t('guides.wiseArchitecture.howWorksDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Context Sources */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.wiseArchitecture.contextSourcesTitle')}</CardTitle>
                    <CardDescription>{t('guides.wiseArchitecture.contextSourcesSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid md:grid-cols-3 gap-4">
                        <div className="rounded-lg border p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <Code className="h-5 w-5 text-blue-500" />
                                <h4 className="font-semibold">{t('guides.wiseArchitecture.codeAnalysis')}</h4>
                            </div>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.codeAnalysisDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <Users className="h-5 w-5 text-green-500" />
                                <h4 className="font-semibold">{t('guides.wiseArchitecture.teamSkills')}</h4>
                            </div>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.teamSkillsDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <Palette className="h-5 w-5 text-purple-500" />
                                <h4 className="font-semibold">{t('guides.wiseArchitecture.figmaDesigns')}</h4>
                            </div>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.figmaDesignsDesc')}
                            </p>
                        </div>
                    </div>

                    <Alert variant="default">
                        <AlertCircle className="h-4 w-4" />
                        <AlertTitle>{t('guides.wiseArchitecture.contextWarningsTitle')}</AlertTitle>
                        <AlertDescription>
                            {t('guides.wiseArchitecture.contextWarningsDesc')}
                        </AlertDescription>
                    </Alert>
                </CardContent>
            </Card>

            {/* What You Get */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.wiseArchitecture.outputTitle')}</CardTitle>
                    <CardDescription>{t('guides.wiseArchitecture.outputSubtitle')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-3">
                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <CheckCircle2 className="h-4 w-4 text-green-500" />
                                {t('guides.wiseArchitecture.architectureOverview')}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.architectureOverviewDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <CheckCircle2 className="h-4 w-4 text-green-500" />
                                {t('guides.wiseArchitecture.reusableServices')}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.reusableServicesDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <CheckCircle2 className="h-4 w-4 text-green-500" />
                                {t('guides.wiseArchitecture.recommendedLibraries')}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.recommendedLibrariesDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <CheckCircle2 className="h-4 w-4 text-green-500" />
                                {t('guides.wiseArchitecture.implementationSteps')}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.implementationStepsDesc')}
                            </p>
                        </div>

                        <div className="rounded-lg border p-4">
                            <h4 className="font-semibold mb-2 flex items-center gap-2">
                                <AlertCircle className="h-4 w-4 text-amber-500" />
                                {t('guides.wiseArchitecture.riskFactors')}
                            </h4>
                            <p className="text-sm text-muted-foreground">
                                {t('guides.wiseArchitecture.riskFactorsDesc')}
                            </p>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* How to Use */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.wiseArchitecture.howToUseTitle')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <ol className="list-decimal list-inside space-y-3">
                        <li className="text-muted-foreground">
                            <span className="text-foreground font-medium">{t('guides.wiseArchitecture.step1Title')}</span>
                            <p className="mt-1 ms-6">{t('guides.wiseArchitecture.step1Desc')}</p>
                        </li>
                        <li className="text-muted-foreground">
                            <span className="text-foreground font-medium">{t('guides.wiseArchitecture.step2Title')}</span>
                            <p className="mt-1 ms-6">{t('guides.wiseArchitecture.step2Desc')}</p>
                        </li>
                        <li className="text-muted-foreground">
                            <span className="text-foreground font-medium">{t('guides.wiseArchitecture.step3Title')}</span>
                            <p className="mt-1 ms-6">{t('guides.wiseArchitecture.step3Desc')}</p>
                        </li>
                        <li className="text-muted-foreground">
                            <span className="text-foreground font-medium">{t('guides.wiseArchitecture.step4Title')}</span>
                            <p className="mt-1 ms-6">{t('guides.wiseArchitecture.step4Desc')}</p>
                        </li>
                    </ol>
                </CardContent>
            </Card>

            {/* Configuration */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Settings className="h-5 w-5" />
                        {t('guides.wiseArchitecture.configTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.wiseArchitecture.configDesc')}</p>
                    
                    <div className="rounded-lg border p-4">
                        <h4 className="font-semibold mb-2">{t('guides.wiseArchitecture.enableFeature')}</h4>
                        <p className="text-sm text-muted-foreground">
                            {t('guides.wiseArchitecture.enableFeatureDesc')}
                        </p>
                    </div>

                    <div className="rounded-lg border p-4">
                        <h4 className="font-semibold mb-2">{t('guides.wiseArchitecture.figmaToken')}</h4>
                        <p className="text-sm text-muted-foreground">
                            {t('guides.wiseArchitecture.figmaTokenDesc')}
                        </p>
                    </div>

                    <div className="rounded-lg border p-4">
                        <h4 className="font-semibold mb-2">{t('guides.wiseArchitecture.mcpConfig')}</h4>
                        <p className="text-sm text-muted-foreground">
                            {t('guides.wiseArchitecture.mcpConfigDesc')}
                        </p>
                    </div>
                </CardContent>
            </Card>

            {/* Tips */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Lightbulb className="h-5 w-5" />
                        {t('guides.wiseArchitecture.tipsTitle')}
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <ul className="space-y-2">
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-4 w-4 text-green-500 mt-1 shrink-0" />
                            <span className="text-sm">{t('guides.wiseArchitecture.tip1')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-4 w-4 text-green-500 mt-1 shrink-0" />
                            <span className="text-sm">{t('guides.wiseArchitecture.tip2')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-4 w-4 text-green-500 mt-1 shrink-0" />
                            <span className="text-sm">{t('guides.wiseArchitecture.tip3')}</span>
                        </li>
                        <li className="flex items-start gap-2">
                            <CheckCircle2 className="h-4 w-4 text-green-500 mt-1 shrink-0" />
                            <span className="text-sm">{t('guides.wiseArchitecture.tip4')}</span>
                        </li>
                    </ul>
                </CardContent>
            </Card>

            {/* Related Links */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('guides.wiseArchitecture.relatedTitle')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex flex-wrap gap-2">
                        <Button asChild variant="outline" size="sm">
                            <Link to="/help/ai-risk-advisor">{t('guides.wiseArchitecture.relatedAIRisk')}</Link>
                        </Button>
                        <Button asChild variant="outline" size="sm">
                            <Link to="/organization-settings">{t('guides.wiseArchitecture.relatedOrgSettings')}</Link>
                        </Button>
                        <Button asChild variant="outline" size="sm">
                            <Link to="/help/getting-started">{t('guides.wiseArchitecture.relatedGettingStarted')}</Link>
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
