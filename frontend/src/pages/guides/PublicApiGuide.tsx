import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Key, Terminal } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

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
                <h1 className="text-4xl font-bold tracking-tight">{t('helpGuides.publicApi', 'Public API')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('helpGuides.publicApiDesc', 'Access ShipFlow programmatically via our REST API and OpenAPI specification.')}
                </p>
            </div>

            <Separator />

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Key className="h-5 w-5" />
                        Authentication
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        All API requests require authentication. You can generate a Personal Access Token or use your active session token.
                    </p>
                    <div className="rounded-lg bg-muted p-4 font-mono text-sm border">
                        Authorization: Bearer &lt;YOUR_TOKEN&gt;
                    </div>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Terminal className="h-5 w-5" />
                        OpenAPI & Swagger UI
                    </CardTitle>
                    <CardDescription>Interactive documentation and client generation</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        ShipFlow exposes a full OpenAPI 3.0 specification.
                    </p>
                    <ul className="list-disc list-inside space-y-2 ml-2">
                        <li><strong>Swagger UI:</strong> Access the interactive documentation at <code>/swagger-ui.html</code> (if enabled on your instance).</li>
                        <li><strong>Raw Spec:</strong> Download the JSON schema from <code>/v3/api-docs</code>.</li>
                    </ul>

                    <h3 className="font-semibold text-lg mt-4">Generating a TypeScript Client</h3>
                    <p>
                        If you are building an extension or scripts, you can auto-generate a type-safe client using <code>openapi-typescript-codegen</code>:
                    </p>
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
