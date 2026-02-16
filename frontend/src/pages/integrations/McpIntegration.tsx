import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Save, Eye, EyeOff, RefreshCw, CheckCircle2, XCircle, Info, HelpCircle, Github, Figma } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card';
import { Alert, AlertDescription } from '../../components/ui/alert';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../components/ui/dialog';
import {
  getMcpStatus,
  getMcpSettings,
  updateMcpSettings,
  McpStatus,
  McpOrganizationSettings,
  UpdateMcpSettingsRequest,
} from '../../services/mcpService';

export default function McpIntegration() {
  const { t } = useTranslation();
  const [tabValue, setTabValue] = useState('github');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [helpDialogOpen, setHelpDialogOpen] = useState(false);
  
  // MCP status from environment
  const [mcpStatus, setMcpStatus] = useState<McpStatus | null>(null);
  
  // Organization settings
  const [settings, setSettings] = useState<McpOrganizationSettings | null>(null);
  
  // Form states for tokens (we need separate state since they're not returned)
  const [githubToken, setGithubToken] = useState('');
  const [figmaToken, setFigmaToken] = useState('');
  const [showGithubToken, setShowGithubToken] = useState(false);
  const [showFigmaToken, setShowFigmaToken] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  // Auto-clear messages after 5 seconds
  useEffect(() => {
    if (success || error) {
      const timer = setTimeout(() => {
        setSuccess(null);
        setError(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [success, error]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [status, orgSettings] = await Promise.all([
        getMcpStatus(),
        getMcpSettings(),
      ]);
      setMcpStatus(status);
      setSettings(orgSettings);
      
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.fetchFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleSaveGithub = async () => {
    try {
      setSaving(true);
      const request: UpdateMcpSettingsRequest = {};
      
      // Only include token if user entered something
      if (githubToken) {
        request.githubAccessToken = githubToken;
      }
      
      const updated = await updateMcpSettings(request);
      setSettings(updated);
      setGithubToken(''); // Clear token field after save
      setSuccess(t('mcpIntegration.githubSaved'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleSaveFigma = async () => {
    try {
      setSaving(true);
      const request: UpdateMcpSettingsRequest = {};
      
      // Only include token if user entered something
      if (figmaToken) {
        request.figmaAccessToken = figmaToken;
      }
      
      const updated = await updateMcpSettings(request);
      setSettings(updated);
      setFigmaToken(''); // Clear token field after save
      setSuccess(t('mcpIntegration.figmaSaved'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleClearGithubToken = async () => {
    try {
      setSaving(true);
      const updated = await updateMcpSettings({ githubAccessToken: '' });
      setSettings(updated);
      setGithubToken('');
      setSuccess(t('mcpIntegration.tokenCleared'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleClearFigmaToken = async () => {
    try {
      setSaving(true);
      const updated = await updateMcpSettings({ figmaAccessToken: '' });
      setSettings(updated);
      setFigmaToken('');
      setSuccess(t('mcpIntegration.tokenCleared'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const renderServerStatus = (status: { enabled: boolean; configured: boolean; serverUrlMasked: string | null; timeoutSeconds: number }) => {
    if (!status.enabled) {
      return (
        <div className="flex items-center gap-2">
          <XCircle className="h-4 w-4 text-muted-foreground" />
          <span className="text-muted-foreground">{t('mcpIntegration.disabled')}</span>
        </div>
      );
    }
    
    if (!status.configured) {
      return (
        <div className="flex items-center gap-2">
          <XCircle className="h-4 w-4 text-yellow-500" />
          <span className="text-yellow-500">{t('mcpIntegration.notConfigured')}</span>
        </div>
      );
    }
    
    return (
      <div className="flex items-center gap-2">
        <CheckCircle2 className="h-4 w-4 text-green-500" />
        <span className="text-green-500">{t('mcpIntegration.connected')}</span>
        {status.serverUrlMasked && (
          <Badge variant="outline" className="ml-2">{status.serverUrlMasked}</Badge>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('mcpIntegration.title')}</h1>
          <p className="text-muted-foreground mt-2">
            {t('mcpIntegration.description')}
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => setHelpDialogOpen(true)}>
          <HelpCircle className="h-4 w-4 mr-2" />
          {t('common.help')}
        </Button>
      </div>

      {/* Alerts */}
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {success && (
        <Alert>
          <CheckCircle2 className="h-4 w-4" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Tabs */}
      <Tabs value={tabValue} onValueChange={setTabValue}>
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="github" className="flex items-center gap-2">
            <Github className="h-4 w-4" />
            GitHub
          </TabsTrigger>
          <TabsTrigger value="figma" className="flex items-center gap-2">
            <Figma className="h-4 w-4" />
            Figma
          </TabsTrigger>
        </TabsList>

        {/* GitHub Tab */}
        <TabsContent value="github" className="space-y-6">
          {/* Server Status Card */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('mcpIntegration.serverStatus')}
              </CardTitle>
              <CardDescription>
                {t('mcpIntegration.serverStatusDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {mcpStatus && renderServerStatus(mcpStatus.github)}
              {mcpStatus?.github.enabled && mcpStatus.github.configured && (
                <p className="text-sm text-muted-foreground mt-2">
                  {t('mcpIntegration.timeout')}: {mcpStatus.github.timeoutSeconds}s
                </p>
              )}
            </CardContent>
          </Card>

          {/* Access Token Card */}
          <Card>
            <CardHeader>
              <CardTitle>{t('mcpIntegration.accessToken')}</CardTitle>
              <CardDescription>
                {t('mcpIntegration.githubTokenDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasGithubAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasGithubAccessToken 
                    ? t('mcpIntegration.tokenConfigured') 
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasGithubAccessToken && (
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    onClick={handleClearGithubToken}
                    disabled={saving}
                  >
                    {t('mcpIntegration.clearToken')}
                  </Button>
                )}
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="github-token">
                  {settings?.hasGithubAccessToken 
                    ? t('mcpIntegration.updateToken') 
                    : t('mcpIntegration.enterToken')}
                </Label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Input
                      id="github-token"
                      type={showGithubToken ? 'text' : 'password'}
                      value={githubToken}
                      onChange={(e) => setGithubToken(e.target.value)}
                      placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-2 top-1/2 -translate-y-1/2"
                      onClick={() => setShowGithubToken(!showGithubToken)}
                    >
                      {showGithubToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </Button>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground">
                  {t('mcpIntegration.githubTokenHint')}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Save Button */}
          <div className="flex justify-end">
            <Button onClick={handleSaveGithub} disabled={saving}>
              {saving ? <RefreshCw className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
              {t('common.save')}
            </Button>
          </div>
        </TabsContent>

        {/* Figma Tab */}
        <TabsContent value="figma" className="space-y-6">
          {/* Server Status Card */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('mcpIntegration.serverStatus')}
              </CardTitle>
              <CardDescription>
                {t('mcpIntegration.serverStatusDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {mcpStatus && renderServerStatus(mcpStatus.figma)}
              {mcpStatus?.figma.enabled && mcpStatus.figma.configured && (
                <p className="text-sm text-muted-foreground mt-2">
                  {t('mcpIntegration.timeout')}: {mcpStatus.figma.timeoutSeconds}s
                </p>
              )}
            </CardContent>
          </Card>

          {/* Access Token Card */}
          <Card>
            <CardHeader>
              <CardTitle>{t('mcpIntegration.accessToken')}</CardTitle>
              <CardDescription>
                {t('mcpIntegration.figmaTokenDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasFigmaAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasFigmaAccessToken 
                    ? t('mcpIntegration.tokenConfigured') 
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasFigmaAccessToken && (
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    onClick={handleClearFigmaToken}
                    disabled={saving}
                  >
                    {t('mcpIntegration.clearToken')}
                  </Button>
                )}
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="figma-token">
                  {settings?.hasFigmaAccessToken 
                    ? t('mcpIntegration.updateToken') 
                    : t('mcpIntegration.enterToken')}
                </Label>
                <div className="flex gap-2">
                  <div className="relative flex-1">
                    <Input
                      id="figma-token"
                      type={showFigmaToken ? 'text' : 'password'}
                      value={figmaToken}
                      onChange={(e) => setFigmaToken(e.target.value)}
                      placeholder="figd_xxxxxxxxxxxxxxxxxxxx"
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="absolute right-2 top-1/2 -translate-y-1/2"
                      onClick={() => setShowFigmaToken(!showFigmaToken)}
                    >
                      {showFigmaToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </Button>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground">
                  {t('mcpIntegration.figmaTokenHint')}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Save Button */}
          <div className="flex justify-end">
            <Button onClick={handleSaveFigma} disabled={saving}>
              {saving ? <RefreshCw className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
              {t('common.save')}
            </Button>
          </div>
        </TabsContent>
      </Tabs>

      {/* Help Dialog */}
      <Dialog open={helpDialogOpen} onOpenChange={setHelpDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.helpTitle')}</DialogTitle>
            <DialogDescription>
              {t('mcpIntegration.helpDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <h4 className="font-semibold mb-2">{t('mcpIntegration.whatIsMcp')}</h4>
              <p className="text-sm text-muted-foreground">
                {t('mcpIntegration.whatIsMcpDescription')}
              </p>
            </div>
            <div>
              <h4 className="font-semibold mb-2">{t('mcpIntegration.serverConfiguration')}</h4>
              <p className="text-sm text-muted-foreground">
                {t('mcpIntegration.serverConfigurationDescription')}
              </p>
              <div className="mt-2 p-3 bg-muted rounded-md font-mono text-sm">
                <p>MCP_GITHUB_ENABLED=true</p>
                <p>MCP_GITHUB_SERVER_URL=http://localhost:3000</p>
                <p>MCP_FIGMA_ENABLED=true</p>
                <p>MCP_FIGMA_SERVER_URL=http://localhost:3001</p>
              </div>
            </div>
            <div>
              <h4 className="font-semibold mb-2">{t('mcpIntegration.tokens')}</h4>
              <p className="text-sm text-muted-foreground">
                {t('mcpIntegration.tokensDescription')}
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
