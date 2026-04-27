import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Cpu, Github, Figma } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { MarkdownInline } from '@/components/ui/markdown';

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
                <h1 className="text-4xl font-bold tracking-tight">{t('guides.mcpServer.title', 'MCP Server Setup')}</h1>
                <p className="text-lg text-muted-foreground mt-2">
                    {t('guides.mcpServer.subtitle', 'Learn how Model Context Protocol (MCP) servers supercharge ShipFlow AI with external context.')}
                </p>
            </div>

            <Separator />

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Cpu className="h-5 w-5" />
                        {t('guides.mcpServer.whatTitle', 'What is MCP?')}
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.mcpServer.whatBody', 'The **Model Context Protocol (MCP)** allows ShipFlow\'s internal AI (like Wise Architecture and Risk Advisor) to securely access your external tools and data sources. By setting up MCP servers, ShipFlow can read your code repositories and design files directly, resulting in vastly superior AI recommendations that are grounded in your actual project reality.')} /></p>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Github className="h-5 w-5" />
                        {t('guides.mcpServer.githubTitle', 'GitHub MCP Setup')}
                    </CardTitle>
                    <CardDescription>{t('guides.mcpServer.githubDesc', 'Code repository analysis')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p><MarkdownInline content={t('guides.mcpServer.githubBody', 'With the GitHub MCP, Wise Architecture can read your `package.json`, `pom.xml`, branch structures, and actual code files to suggest libraries and architecture patterns that fit your existing tech stack.')} /></p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li><MarkdownInline content={t('guides.mcpServer.githubStep1', 'Navigate to **Organization Settings** > **Integrations** > **GitHub**.')} /></li>
                        <li>{t('guides.mcpServer.githubStep2', 'Provide your GitHub App credentials or Personal Access Token.')}</li>
                        <li><MarkdownInline content={t('guides.mcpServer.githubStep3', 'Ensure the environment variable `MCP_GITHUB_ENABLED=true` is set on your ShipFlow backend server.')} /></li>
                    </ol>
                </CardContent>
            </Card>

            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <Figma className="h-5 w-5" />
                        {t('guides.mcpServer.figmaTitle', 'Figma MCP Setup')}
                    </CardTitle>
                    <CardDescription>{t('guides.mcpServer.figmaDesc', 'Design context extraction')}</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p>{t('guides.mcpServer.figmaBody', 'The Figma MCP integration allows ShipFlow to pull node and frame data from linked Figma URLs inside pitches. The AI reads this design context to better estimate frontend complexity and required components.')}</p>
                    <ol className="list-decimal list-inside space-y-2 ml-2">
                        <li><MarkdownInline content={t('guides.mcpServer.figmaStep1', 'Navigate to **Organization Settings** > **Integrations** > **MCP Integration** (or Figma settings).')} /></li>
                        <li>{t('guides.mcpServer.figmaStep2', "Store your organization's Figma Access Token.")}</li>
                        <li><MarkdownInline content={t('guides.mcpServer.figmaStep3', 'Ensure `MCP_FIGMA_ENABLED=true` and `MCP_FIGMA_SERVER_URL` point to your running Figma MCP server instance.')} /></li>
                    </ol>
                </CardContent>
            </Card>
        </div>
    );
}
