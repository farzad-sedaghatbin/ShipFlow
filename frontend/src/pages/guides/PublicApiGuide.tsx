import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Key, Terminal } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

export default function PublicApiGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.publicApi.title', 'Public API')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.publicApi.subtitle', 'Access ShipFlow programmatically via our REST API and OpenAPI specification.')}
                </p>
            </div>

            <Separator />

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Key className="h-5 w-5" />
                        {t('guides.publicApi.authTitle', 'Authentication')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.publicApi.authBody', 'All API requests require authentication. You can generate a Personal Access Token or use your active session token.')}</p>
                    <div className="rounded-lg bg-muted p-4 font-mono text-sm border">
                        Authorization: Bearer &lt;YOUR_TOKEN&gt;
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Terminal className="h-5 w-5" />
                        {t('guides.publicApi.openApiTitle', 'OpenAPI & Swagger UI')}
                    </CardTitle>
                    <CardDescription>{t('guides.publicApi.openApiDesc', 'Interactive documentation and client generation')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.publicApi.openApiBody', 'ShipFlow exposes a full OpenAPI 3.0 specification.')}</p>
                    <ul className="list-disc list-inside space-y-2 ml-2">
                        <li><MarkdownInline content={t('guides.publicApi.openApiItem1', '**Swagger UI:** Access the interactive documentation at `/swagger-ui.html` (if enabled on your instance).')} /></li>
                        <li><MarkdownInline content={t('guides.publicApi.openApiItem2', '**Raw Spec:** Download the JSON schema from `/v3/api-docs`.')} /></li>
                    </ul>

                    <h3 className="font-semibold text-lg mt-4">{t('guides.publicApi.clientTitle', 'Generating a TypeScript Client')}</h3>
                    <p><MarkdownInline content={t('guides.publicApi.clientBody', 'If you are building an extension or scripts, you can auto-generate a type-safe client using `openapi-typescript-codegen`:')} /></p>
                    <div className="rounded-lg bg-muted p-4 font-mono text-sm border whitespace-pre-wrap">
                        {`# Example generation command
npx openapi-typescript-codegen \\
  --input http://localhost:8080/v3/api-docs \\
  --output ./src/api/generated \\
  --client axios`}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
