import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Cpu, Github, Figma } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';

export default function McpServerGuide() {
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
                <h1 className="text-4xl font-bold tracking-tight">{t('helpGuides.mcpServer', 'MCP Server Setup')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('helpGuides.mcpServerDesc', 'Learn how Model Context Protocol (MCP) servers supercharge ShipFlow AI with external context.')}
                </p>
            </div>

            <Separator />

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Cpu className="h-5 w-5" />
                        What is MCP?
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The <strong>Model Context Protocol (MCP)</strong> allows ShipFlow's internal AI (like Wise Architecture and Risk Advisor) to securely access your external tools and data sources.
                        By setting up MCP servers, ShipFlow can read your code repositories and design files directly, resulting in vastly superior AI recommendations that are grounded in your actual project reality.
                    </p>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Github className="h-5 w-5" />
                        GitHub MCP Setup
                    </CardTitle>
                    <CardDescription>Code repository analysis</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        With the GitHub MCP, Wise Architecture can read your <code>package.json</code>, <code>pom.xml</code>, branch structures, and actual code files to suggest libraries and architecture patterns that fit your existing tech stack.
                    </p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>Navigate to <strong>Organization Settings</strong> &gt; <strong>Integrations</strong> &gt; <strong>GitHub</strong>.</li>
                        <li>Provide your GitHub App credentials or Personal Access Token.</li>
                        <li>Ensure the environment variable <code>MCP_GITHUB_ENABLED=true</code> is set on your ShipFlow backend server.</li>
                    </ol>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Figma className="h-5 w-5" />
                        Figma MCP Setup
                    </CardTitle>
                    <CardDescription>Design context extraction</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>
                        The Figma MCP integration allows ShipFlow to pull node and frame data from linked Figma URLs inside pitches. The AI reads this design context to better estimate frontend complexity and required components.
                    </p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li>Navigate to <strong>Organization Settings</strong> &gt; <strong>Integrations</strong> &gt; <strong>MCP Integration</strong> (or Figma settings).</li>
                        <li>Store your organization's Figma Access Token.</li>
                        <li>Ensure <code>MCP_FIGMA_ENABLED=true</code> and <code>MCP_FIGMA_SERVER_URL</code> point to your running Figma MCP server instance.</li>
                    </ol>
                </CardContent>
            </Card>
        </div>
    );
}
