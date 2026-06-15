import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Save,
  Eye,
  EyeOff,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Info,
  HelpCircle,
  Github,
  Figma,
  Server,
  KeyRound,
  Copy,
  Trash2,
} from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card';
import { Alert, AlertDescription } from '../../components/ui/alert';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/tabs';
import { Switch } from '../../components/ui/switch';
import { Checkbox } from '../../components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '../../components/ui/dialog';
import {
  getMcpStatus,
  getMcpSettings,
  updateMcpSettings,
  updateMcpServerToggle,
  McpStatus,
  McpOrganizationSettings,
  McpServerSettings,
  UpdateMcpSettingsRequest,
} from '../../services/mcpService';
import {
  listApiKeys,
  listAllApiKeys,
  createApiKey,
  revokeApiKey,
  adminRevokeApiKey,
  ApiKey,
  CreatedApiKey,
} from '../../services/apiKeyService';
import { useAuth } from '../../contexts';

const ALL_SCOPES = ['READ', 'WRITE', 'ADMIN'] as const;

export default function McpIntegration() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
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
  const [notionToken, setNotionToken] = useState('');
  const [confluenceToken, setConfluenceToken] = useState('');
  const [confluenceDomain, setConfluenceDomain] = useState('');
  const [confluenceSpaceKey, setConfluenceSpaceKey] = useState('');
  const [showGithubToken, setShowGithubToken] = useState(false);
  const [showFigmaToken, setShowFigmaToken] = useState(false);
  const [showNotionToken, setShowNotionToken] = useState(false);
  const [showConfluenceToken, setShowConfluenceToken] = useState(false);

  // Built-in MCP server runtime toggle
  const [serverSettings, setServerSettings] = useState<McpServerSettings | null>(null);
  const [savingToggle, setSavingToggle] = useState(false);

  // API key management
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [showCreateKeyDialog, setShowCreateKeyDialog] = useState(false);
  const [keyName, setKeyName] = useState('');
  const [keyScopes, setKeyScopes] = useState<string[]>(['READ']);
  const [keyExpiresAt, setKeyExpiresAt] = useState('');
  const [creatingKey, setCreatingKey] = useState(false);
  const [createdKey, setCreatedKey] = useState<CreatedApiKey | null>(null);
  const [copied, setCopied] = useState(false);

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
      const [status, orgSettings] = await Promise.all([getMcpStatus(), getMcpSettings()]);
      setMcpStatus(status);
      setSettings(orgSettings);
      // The MCP server runtime toggle is part of the same /settings payload — derive it
      // here instead of making a second identical request.
      setServerSettings({
        mcpServerEnabled: orgSettings.mcpServerEnabled,
        mcpServerWriteEnabled: orgSettings.mcpServerWriteEnabled,
      });
      setError(null);
    } catch (err: any) {
      // Keep serverSettings null so the UI shows an "unavailable" state rather than
      // misrepresenting a network error as "server disabled".
      setServerSettings(null);
      setError(err.response?.data?.message || t('mcpIntegration.fetchFailed'));
    } finally {
      setLoading(false);
    }

    // API keys are independent and non-fatal — a failure here must not blank the page.
    try {
      setApiKeys(isAdmin ? await listAllApiKeys() : await listApiKeys());
    } catch {
      setApiKeys([]);
    }
  };

  const handleSaveGithub = async () => {
    try {
      setSaving(true);
      const request: UpdateMcpSettingsRequest = {};
      if (githubToken) {
        request.githubAccessToken = githubToken;
      }
      const updated = await updateMcpSettings(request);
      setSettings(updated);
      setGithubToken('');
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
      if (figmaToken) {
        request.figmaAccessToken = figmaToken;
      }
      const updated = await updateMcpSettings(request);
      setSettings(updated);
      setFigmaToken('');
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

  const handleSaveNotion = async () => {
    if (!notionToken.trim()) return;
    try {
      setSaving(true);
      const updated = await updateMcpSettings({ notionAccessToken: notionToken });
      setSettings(updated);
      setNotionToken('');
      setSuccess(t('mcpIntegration.tokenSaved'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleClearNotionToken = async () => {
    try {
      setSaving(true);
      const updated = await updateMcpSettings({ notionAccessToken: '' });
      setSettings(updated);
      setNotionToken('');
      setSuccess(t('mcpIntegration.tokenCleared'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleSaveConfluence = async () => {
    if (!confluenceToken.trim() && !confluenceDomain.trim() && !confluenceSpaceKey.trim()) return;
    try {
      setSaving(true);
      const req: { confluenceAccessToken?: string; defaultConfluenceDomain?: string; defaultConfluenceSpaceKey?: string } = {};
      if (confluenceToken.trim()) req.confluenceAccessToken = confluenceToken;
      if (confluenceDomain.trim()) req.defaultConfluenceDomain = confluenceDomain;
      if (confluenceSpaceKey.trim()) req.defaultConfluenceSpaceKey = confluenceSpaceKey;
      const updated = await updateMcpSettings(req);
      setSettings(updated);
      setConfluenceToken('');
      setSuccess(t('mcpIntegration.tokenSaved'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  const handleClearConfluenceToken = async () => {
    try {
      setSaving(true);
      const updated = await updateMcpSettings({ confluenceAccessToken: '' });
      setSettings(updated);
      setConfluenceToken('');
      setSuccess(t('mcpIntegration.tokenCleared'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  // --- Built-in MCP server toggle handlers ---
  const handleToggleServer = async (enabled: boolean) => {
    setError(null);
    setSuccess(null);
    setSavingToggle(true);
    try {
      const updated = await updateMcpServerToggle({ mcpServerEnabled: enabled });
      setServerSettings(updated);
      setSuccess(t('mcpIntegration.serverToggleSuccess'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.serverToggleError'));
    } finally {
      setSavingToggle(false);
    }
  };

  const handleToggleWrite = async (enabled: boolean) => {
    setError(null);
    setSuccess(null);
    setSavingToggle(true);
    try {
      const updated = await updateMcpServerToggle({ mcpServerWriteEnabled: enabled });
      setServerSettings(updated);
      setSuccess(t('mcpIntegration.serverToggleSuccess'));
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.serverToggleError'));
    } finally {
      setSavingToggle(false);
    }
  };

  // --- API key handlers ---
  const handleToggleScope = (scope: string) => {
    setKeyScopes((prev) =>
      prev.includes(scope) ? prev.filter((s) => s !== scope) : [...prev, scope]
    );
  };

  const resetKeyForm = () => {
    setKeyName('');
    setKeyScopes(['READ']);
    setKeyExpiresAt('');
  };

  const handleCreateKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!keyName.trim() || keyScopes.length === 0) {
      return;
    }
    setError(null);
    setSuccess(null);
    setCreatingKey(true);
    try {
      const created = await createApiKey({
        name: keyName.trim(),
        scopes: keyScopes,
        // Backend expects a LocalDateTime; pad the date input to start-of-day.
        expiresAt: keyExpiresAt ? `${keyExpiresAt}T00:00:00` : undefined,
      });
      setShowCreateKeyDialog(false);
      resetKeyForm();
      setCreatedKey(created);
      setCopied(false);
      setApiKeys(isAdmin ? await listAllApiKeys() : await listApiKeys());
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.keyCreateError'));
    } finally {
      setCreatingKey(false);
    }
  };

  const handleRevokeKey = async (id: number) => {
    if (!window.confirm(t('mcpIntegration.keyRevokeConfirm'))) {
      return;
    }
    setError(null);
    setSuccess(null);
    try {
      isAdmin ? await adminRevokeApiKey(id) : await revokeApiKey(id);
      setSuccess(t('mcpIntegration.keyRevokeSuccess'));
      setApiKeys(isAdmin ? await listAllApiKeys() : await listApiKeys());
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.keyRevokeError'));
    }
  };

  const handleCopyKey = async () => {
    if (!createdKey) return;
    try {
      await navigator.clipboard.writeText(createdKey.rawKey);
      setCopied(true);
    } catch {
      // Clipboard may be unavailable (insecure context); leave the key visible to copy manually.
      setCopied(false);
    }
  };

  const formatDate = (value?: string | null) => {
    if (!value) return t('mcpIntegration.never');
    return new Date(value).toLocaleDateString();
  };

  // The MCP endpoints are served by the backend. Prefer the configured API base
  // (VITE_API_BASE_URL) so the URL is correct when the frontend talks to a different
  // backend host; fall back to the current origin for same-origin deployments.
  const mcpBaseUrl = (import.meta.env.VITE_API_BASE_URL || window.location.origin).replace(/\/$/, '');
  const sseUrl = `${mcpBaseUrl}/mcp/sse`;

  const renderServerStatus = (status: {
    enabled: boolean;
    configured: boolean;
    serverUrlMasked: string | null;
    timeoutSeconds: number;
  }) => {
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
          <Badge variant="outline" className="ml-2">
            {status.serverUrlMasked}
          </Badge>
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

  const serverEnabled = serverSettings?.mcpServerEnabled ?? false;
  const writeEnabled = serverSettings?.mcpServerWriteEnabled ?? false;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('mcpIntegration.title')}</h1>
          <p className="text-muted-foreground mt-2">{t('mcpIntegration.description')}</p>
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
        <TabsList className="grid w-full max-w-3xl grid-cols-6">
          <TabsTrigger value="github" className="flex items-center gap-2">
            <Github className="h-4 w-4" />
            GitHub
          </TabsTrigger>
          <TabsTrigger value="figma" className="flex items-center gap-2">
            <Figma className="h-4 w-4" />
            Figma
          </TabsTrigger>
          <TabsTrigger value="notion" className="flex items-center gap-2">
            <span className="font-bold text-xs">N</span>
            Notion
          </TabsTrigger>
          <TabsTrigger value="confluence" className="flex items-center gap-2">
            <span className="font-bold text-xs">C</span>
            {t('mcpIntegration.confluenceTab')}
          </TabsTrigger>
          <TabsTrigger value="server" className="flex items-center gap-2">
            <Server className="h-4 w-4" />
            {t('mcpIntegration.serverTab')}
          </TabsTrigger>
          <TabsTrigger value="apikeys" className="flex items-center gap-2">
            <KeyRound className="h-4 w-4" />
            {t('mcpIntegration.apiKeysTab')}
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
              <CardDescription>{t('mcpIntegration.serverStatusDescription')}</CardDescription>
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
              <CardDescription>{t('mcpIntegration.githubTokenDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasGithubAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasGithubAccessToken
                    ? t('mcpIntegration.tokenConfigured')
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasGithubAccessToken && (
                  <Button variant="ghost" size="sm" onClick={handleClearGithubToken} disabled={saving}>
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
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.githubTokenHint')}</p>
              </div>
            </CardContent>
          </Card>

          {/* Save Button */}
          <div className="flex justify-end">
            <Button onClick={handleSaveGithub} disabled={saving}>
              {saving ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Save className="h-4 w-4 mr-2" />
              )}
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
              <CardDescription>{t('mcpIntegration.serverStatusDescription')}</CardDescription>
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
              <CardDescription>{t('mcpIntegration.figmaTokenDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasFigmaAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasFigmaAccessToken
                    ? t('mcpIntegration.tokenConfigured')
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasFigmaAccessToken && (
                  <Button variant="ghost" size="sm" onClick={handleClearFigmaToken} disabled={saving}>
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
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.figmaTokenHint')}</p>
              </div>
            </CardContent>
          </Card>

          {/* Save Button */}
          <div className="flex justify-end">
            <Button onClick={handleSaveFigma} disabled={saving}>
              {saving ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Save className="h-4 w-4 mr-2" />
              )}
              {t('common.save')}
            </Button>
          </div>
        </TabsContent>

        {/* Notion Tab */}
        <TabsContent value="notion" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('mcpIntegration.serverStatus')}
              </CardTitle>
              <CardDescription>{t('mcpIntegration.notionServerStatusDesc')}</CardDescription>
            </CardHeader>
            <CardContent>
              {mcpStatus?.notion && renderServerStatus(mcpStatus.notion)}
              {mcpStatus?.notion?.enabled && mcpStatus.notion?.configured && (
                <p className="text-sm text-muted-foreground mt-2">
                  {t('mcpIntegration.timeout')}: {mcpStatus.notion.timeoutSeconds}s
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('mcpIntegration.accessToken')}</CardTitle>
              <CardDescription>{t('mcpIntegration.notionTokenDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasNotionAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasNotionAccessToken
                    ? t('mcpIntegration.tokenConfigured')
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasNotionAccessToken && (
                  <Button variant="ghost" size="sm" onClick={handleClearNotionToken} disabled={saving}>
                    {t('mcpIntegration.clearToken')}
                  </Button>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="notion-token">
                  {settings?.hasNotionAccessToken
                    ? t('mcpIntegration.updateToken')
                    : t('mcpIntegration.enterToken')}
                </Label>
                <div className="relative flex-1">
                  <Input
                    id="notion-token"
                    type={showNotionToken ? 'text' : 'password'}
                    value={notionToken}
                    onChange={(e) => setNotionToken(e.target.value)}
                    placeholder="secret_xxxxxxxxxxxxxxxxxxxx"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-2 top-1/2 -translate-y-1/2"
                    onClick={() => setShowNotionToken(!showNotionToken)}
                  >
                    {showNotionToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </Button>
                </div>
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.notionTokenHint')}</p>
              </div>
            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button onClick={handleSaveNotion} disabled={saving || !notionToken.trim()}>
              {saving ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Save className="h-4 w-4 mr-2" />
              )}
              {t('common.save')}
            </Button>
          </div>
        </TabsContent>

        {/* Confluence Tab */}
        <TabsContent value="confluence" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('mcpIntegration.serverStatus')}
              </CardTitle>
              <CardDescription>{t('mcpIntegration.confluenceServerStatusDesc')}</CardDescription>
            </CardHeader>
            <CardContent>
              {mcpStatus?.confluence && renderServerStatus(mcpStatus.confluence)}
              {mcpStatus?.confluence?.enabled && mcpStatus.confluence?.configured && (
                <p className="text-sm text-muted-foreground mt-2">
                  {t('mcpIntegration.timeout')}: {mcpStatus.confluence.timeoutSeconds}s
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('mcpIntegration.accessToken')}</CardTitle>
              <CardDescription>{t('mcpIntegration.confluenceTokenDescription')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2">
                <Badge variant={settings?.hasConfluenceAccessToken ? 'default' : 'secondary'}>
                  {settings?.hasConfluenceAccessToken
                    ? t('mcpIntegration.tokenConfigured')
                    : t('mcpIntegration.tokenNotConfigured')}
                </Badge>
                {settings?.hasConfluenceAccessToken && (
                  <Button variant="ghost" size="sm" onClick={handleClearConfluenceToken} disabled={saving}>
                    {t('mcpIntegration.clearToken')}
                  </Button>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="confluence-token">
                  {settings?.hasConfluenceAccessToken
                    ? t('mcpIntegration.updateToken')
                    : t('mcpIntegration.enterToken')}
                </Label>
                <div className="relative flex-1">
                  <Input
                    id="confluence-token"
                    type={showConfluenceToken ? 'text' : 'password'}
                    value={confluenceToken}
                    onChange={(e) => setConfluenceToken(e.target.value)}
                    placeholder="xxxxxxxxxxxxxxxxxxxx"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-2 top-1/2 -translate-y-1/2"
                    onClick={() => setShowConfluenceToken(!showConfluenceToken)}
                  >
                    {showConfluenceToken ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </Button>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confluence-domain">{t('mcpIntegration.confluenceDomain')}</Label>
                <Input
                  id="confluence-domain"
                  value={confluenceDomain}
                  onChange={(e) => setConfluenceDomain(e.target.value)}
                  placeholder="yourcompany.atlassian.net"
                />
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.confluenceDomainHint')}</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="confluence-space">{t('mcpIntegration.confluenceSpaceKey')}</Label>
                <Input
                  id="confluence-space"
                  value={confluenceSpaceKey}
                  onChange={(e) => setConfluenceSpaceKey(e.target.value)}
                  placeholder="ENG"
                />
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.confluenceSpaceKeyHint')}</p>
              </div>
            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button
              onClick={handleSaveConfluence}
              disabled={saving || (!confluenceToken.trim() && !confluenceDomain.trim() && !confluenceSpaceKey.trim())}
            >
              {saving ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Save className="h-4 w-4 mr-2" />
              )}
              {t('common.save')}
            </Button>
          </div>
        </TabsContent>

        {/* Built-in MCP Server Tab */}
        <TabsContent value="server" className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Server className="h-5 w-5" />
                    {t('mcpIntegration.serverTitle')}
                  </CardTitle>
                  <CardDescription>{t('mcpIntegration.serverDescription')}</CardDescription>
                </div>
                <Badge variant={serverEnabled ? 'default' : 'secondary'}>
                  {serverEnabled
                    ? t('mcpIntegration.serverStatusEnabled')
                    : t('mcpIntegration.serverStatusDisabled')}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="pr-4">
                  <Label htmlFor="mcp-server-toggle" className="font-medium">
                    {t('mcpIntegration.serverEnableLabel')}
                  </Label>
                  <p className="text-sm text-muted-foreground">
                    {t('mcpIntegration.serverEnableHint')}
                  </p>
                </div>
                <Switch
                  id="mcp-server-toggle"
                  checked={serverEnabled}
                  onCheckedChange={handleToggleServer}
                  disabled={savingToggle}
                />
              </div>

              <div className="flex items-center justify-between">
                <div className="pr-4">
                  <Label htmlFor="mcp-write-toggle" className="font-medium">
                    {t('mcpIntegration.serverWriteLabel')}
                  </Label>
                  <p className="text-sm text-muted-foreground">
                    {t('mcpIntegration.serverWriteHint')}
                  </p>
                </div>
                <Switch
                  id="mcp-write-toggle"
                  checked={writeEnabled}
                  onCheckedChange={handleToggleWrite}
                  disabled={savingToggle || !serverEnabled}
                />
              </div>

              <div className="rounded-md border p-4 bg-muted/50 space-y-2">
                <h4 className="font-semibold text-sm">{t('mcpIntegration.connectTitle')}</h4>
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.connectIntro')}</p>
                <div className="text-sm">
                  <span className="text-muted-foreground">{t('mcpIntegration.connectSseUrl')}: </span>
                  <code className="px-1 py-0.5 rounded bg-muted font-mono text-xs">{sseUrl}</code>
                </div>
                <div className="text-sm">
                  <span className="text-muted-foreground">{t('mcpIntegration.connectAuth')}: </span>
                  <code className="px-1 py-0.5 rounded bg-muted font-mono text-xs">
                    Authorization: Bearer &lt;api-key&gt;
                  </code>
                </div>
                <p className="text-sm text-muted-foreground">{t('mcpIntegration.connectKeyHint')}</p>
                <p className="text-sm text-muted-foreground">
                  {t('mcpIntegration.connectNoRestart')}
                </p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* API Keys Tab */}
        <TabsContent value="apikeys" className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <KeyRound className="h-5 w-5" />
                    {t('mcpIntegration.apiKeysTitle')}
                  </CardTitle>
                  <CardDescription>
                    {isAdmin
                      ? t('mcpIntegration.adminApiKeysDescription')
                      : t('mcpIntegration.apiKeysDescription')}
                  </CardDescription>
                </div>
                <Button
                  onClick={() => {
                    resetKeyForm();
                    setShowCreateKeyDialog(true);
                  }}
                >
                  {t('mcpIntegration.createKey')}
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {apiKeys.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
                  {t('mcpIntegration.noApiKeys')}
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b text-left text-muted-foreground">
                        <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableName')}</th>
                        <th className="py-2 pr-4 font-medium">
                          {t('mcpIntegration.keyTablePrefix')}
                        </th>
                        <th className="py-2 pr-4 font-medium">
                          {t('mcpIntegration.keyTableScopes')}
                        </th>
                        <th className="py-2 pr-4 font-medium">
                          {t('mcpIntegration.keyTableLastUsed')}
                        </th>
                        <th className="py-2 pr-4 font-medium">
                          {t('mcpIntegration.keyTableCreated')}
                        </th>
                        {isAdmin && (
                          <th className="py-2 pr-4 font-medium">
                            {t('mcpIntegration.keyTableCreatedBy')}
                          </th>
                        )}
                        <th className="py-2 pr-4 font-medium">
                          {t('mcpIntegration.keyTableStatus')}
                        </th>
                        <th className="py-2 pr-4 font-medium text-right">
                          {t('mcpIntegration.keyTableActions')}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {apiKeys.map((key) => (
                        <tr key={key.id} className="border-b last:border-0">
                          <td className="py-2 pr-4">{key.name}</td>
                          <td className="py-2 pr-4">
                            <code className="font-mono text-xs">{key.keyPrefix}…</code>
                          </td>
                          <td className="py-2 pr-4">
                            <div className="flex flex-wrap gap-1">
                              {key.scopes.map((scope) => (
                                <Badge key={scope} variant="secondary">
                                  {scope}
                                </Badge>
                              ))}
                            </div>
                          </td>
                          <td className="py-2 pr-4">{formatDate(key.lastUsedAt)}</td>
                          <td className="py-2 pr-4">{formatDate(key.createdAt)}</td>
                          {isAdmin && <td className="py-2 pr-4">{key.createdByUsername ?? '—'}</td>}
                          <td className="py-2 pr-4">
                            <Badge variant={key.isActive ? 'default' : 'secondary'}>
                              {key.isActive
                                ? t('mcpIntegration.keyStatusActive')
                                : t('mcpIntegration.keyStatusRevoked')}
                            </Badge>
                          </td>
                          <td className="py-2 pr-4 text-right">
                            {key.isActive && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleRevokeKey(key.id)}
                              >
                                <Trash2 className="h-4 w-4 mr-1" />
                                {t('mcpIntegration.revokeKey')}
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Help Dialog */}
      <Dialog open={helpDialogOpen} onOpenChange={setHelpDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.helpTitle')}</DialogTitle>
            <DialogDescription>{t('mcpIntegration.helpDescription')}</DialogDescription>
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
              <p className="text-sm text-muted-foreground">{t('mcpIntegration.tokensDescription')}</p>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Create API Key Dialog */}
      <Dialog
        open={showCreateKeyDialog}
        onOpenChange={(open) => !open && setShowCreateKeyDialog(false)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.createKeyTitle')}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreateKey} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="keyName">{t('mcpIntegration.keyName')}</Label>
              <Input
                id="keyName"
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>{t('mcpIntegration.keyScopes')}</Label>
              <div className="space-y-2">
                {ALL_SCOPES.map((scope) => (
                  <div key={scope} className="flex items-center gap-2">
                    <Checkbox
                      id={`scope-${scope}`}
                      checked={keyScopes.includes(scope)}
                      onCheckedChange={() => handleToggleScope(scope)}
                    />
                    <Label htmlFor={`scope-${scope}`} className="font-normal">
                      {t(`mcpIntegration.scope${scope}`)}
                    </Label>
                  </div>
                ))}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="keyExpiresAt">{t('mcpIntegration.keyExpiresAt')}</Label>
              <Input
                id="keyExpiresAt"
                type="date"
                value={keyExpiresAt}
                onChange={(e) => setKeyExpiresAt(e.target.value)}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreateKeyDialog(false)}>
                {t('common.cancel')}
              </Button>
              <Button
                type="submit"
                disabled={creatingKey || !keyName.trim() || keyScopes.length === 0}
              >
                {creatingKey ? t('mcpIntegration.creating') : t('mcpIntegration.createKey')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Show-once Raw Key Dialog */}
      <Dialog open={!!createdKey} onOpenChange={(open) => !open && setCreatedKey(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.keyCreatedTitle')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <Alert variant="destructive">
              <AlertDescription>{t('mcpIntegration.keyShowOnceWarning')}</AlertDescription>
            </Alert>
            <div className="flex items-center gap-2">
              <code className="flex-1 break-all rounded bg-muted px-3 py-2 font-mono text-sm">
                {createdKey?.rawKey}
              </code>
              <Button type="button" variant="outline" size="sm" onClick={handleCopyKey}>
                <Copy className="h-4 w-4 mr-1" />
                {copied ? t('mcpIntegration.copied') : t('mcpIntegration.copy')}
              </Button>
            </div>
          </div>
          <DialogFooter>
            <Button type="button" onClick={() => setCreatedKey(null)}>
              {t('common.close')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
