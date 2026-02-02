import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Alert, AlertDescription } from './ui/alert';
import { Switch } from './ui/switch';
import { Separator } from './ui/separator';
import { Badge } from './ui/badge';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import { Plus, Github, Copy, CheckCircle2, Building2, RefreshCw, Trash2 } from 'lucide-react';
import { githubService, GitHubAppStatus, GitHubAppInstallation } from '../services/githubService';
import { GitHubRepository, CreateGitHubRepositoryRequest } from '../types/github';

export default function GitHubRepositoryManager() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [webhookUrlCopied, setWebhookUrlCopied] = useState(false);
  
  // GitHub App OAuth state
  const [appStatus, setAppStatus] = useState<GitHubAppStatus | null>(null);
  const [installations, setInstallations] = useState<GitHubAppInstallation[]>([]);
  const [oauthLoading, setOauthLoading] = useState(false);
  const [syncLoading, setSyncLoading] = useState<number | 'all' | 'discover' | null>(null);
  
  const [formData, setFormData] = useState<CreateGitHubRepositoryRequest>({
    owner: '',
    name: '',
    url: '',
    defaultBranch: 'main',
    webhookSecret: '',
    accessToken: '',
    autoLinkEnabled: true,
    autoCloseTasksOnMerge: true,
  });

  useEffect(() => {
    loadRepositories();
    loadAppStatus();
  }, []);

  const loadRepositories = async () => {
    try {
      const data = await githubService.getAllRepositories();
      setRepositories(data);
    } catch (err) {
      console.error('Failed to load repositories:', err);
    }
  };

  const loadAppStatus = async () => {
    try {
      const status = await githubService.getAppStatus();
      setAppStatus(status);
      if (status.configured) {
        loadInstallations();
      }
    } catch (err) {
      console.error('Failed to load app status:', err);
      setAppStatus({ configured: false });
    }
  };

  const loadInstallations = async () => {
    try {
      const data = await githubService.getInstallations();
      setInstallations(data);
    } catch (err) {
      console.error('Failed to load installations:', err);
    }
  };

  const handleConnectOrganization = async () => {
    setOauthLoading(true);
    try {
      const response = await githubService.initiateOAuth();
      // Redirect to GitHub authorization
      window.location.href = response.authorizationUrl;
    } catch (err: any) {
      setError(err.response?.data?.message || t('githubApp.connectError'));
      setOauthLoading(false);
    }
  };

  const handleSyncInstallation = async (installationId: number) => {
    setSyncLoading(installationId);
    try {
      await githubService.syncInstallation(installationId);
      await loadRepositories();
      await loadInstallations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('githubApp.syncError'));
    } finally {
      setSyncLoading(null);
    }
  };

  const handleSyncAll = async () => {
    setSyncLoading('all');
    try {
      await githubService.syncAllRepositories();
      await loadRepositories();
      await loadInstallations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('githubApp.syncError'));
    } finally {
      setSyncLoading(null);
    }
  };

  const handleDiscoverInstallations = async () => {
    setSyncLoading('discover');
    setError(null);
    try {
      const result = await githubService.discoverInstallations();
      if (result.success) {
        await loadRepositories();
        await loadInstallations();
      } else {
        setError(result.message || t('githubApp.discoverError'));
      }
    } catch (err: any) {
      setError(err.response?.data?.message || t('githubApp.discoverError'));
    } finally {
      setSyncLoading(null);
    }
  };

  const handleRemoveInstallation = async (installationId: number) => {
    if (!confirm(t('githubApp.confirmRemove'))) return;
    try {
      await githubService.removeInstallation(installationId);
      await loadInstallations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('githubApp.removeError'));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await githubService.registerRepository(formData);
      setOpen(false);
      resetForm();
      loadRepositories();
    } catch (err: any) {
      setError(err.response?.data?.message || t('errors.registerRepositoryFailed'));
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setFormData({
      owner: '',
      name: '',
      url: '',
      defaultBranch: 'main',
      webhookSecret: '',
      accessToken: '',
      autoLinkEnabled: true,
      autoCloseTasksOnMerge: true,
    });
    setError(null);
  };

  const copyWebhookUrl = () => {
    const baseUrl = window.location.origin;
    const webhookUrl = `${baseUrl}/api/github/webhook`;
    navigator.clipboard.writeText(webhookUrl);
    setWebhookUrlCopied(true);
    setTimeout(() => setWebhookUrlCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* GitHub App OAuth Section - Only show if configured */}
      {appStatus?.configured && (
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <Building2 className="h-5 w-5" />
                  {t('githubApp.title')}
                </CardTitle>
                <CardDescription className="mt-1">
                  {t('githubApp.description')}
                </CardDescription>
              </div>
              <div className="flex gap-2">
                {installations.length > 0 ? (
                  <Button
                    variant="outline"
                    onClick={handleSyncAll}
                    disabled={syncLoading === 'all'}
                  >
                    <RefreshCw className={`mr-2 h-4 w-4 ${syncLoading === 'all' ? 'animate-spin' : ''}`} />
                    {t('githubApp.syncAll')}
                  </Button>
                ) : (
                  <Button
                    variant="outline"
                    onClick={handleDiscoverInstallations}
                    disabled={syncLoading === 'discover'}
                  >
                    <RefreshCw className={`mr-2 h-4 w-4 ${syncLoading === 'discover' ? 'animate-spin' : ''}`} />
                    {t('githubApp.discoverInstallations')}
                  </Button>
                )}
                <Button onClick={handleConnectOrganization} disabled={oauthLoading}>
                  <Github className="mr-2 h-4 w-4" />
                  {oauthLoading ? t('githubApp.connecting') : t('githubApp.connectOrganization')}
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {installations.length > 0 ? (
              <div className="space-y-3">
                {installations.map((installation) => (
                  <div key={installation.id} className="flex items-center justify-between p-4 border rounded-lg">
                    <div className="flex items-center gap-3">
                      <Building2 className="h-8 w-8 text-muted-foreground" />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold">{installation.accountLogin}</span>
                          <Badge variant={installation.accountType === 'Organization' ? 'default' : 'secondary'}>
                            {installation.accountType}
                          </Badge>
                          {installation.tokenValid && (
                            <Badge variant="success">{t('githubApp.connected')}</Badge>
                          )}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {installation.repositorySelection === 'all' 
                            ? t('githubApp.allRepositories')
                            : t('githubApp.selectedRepositories', { count: installation.repositoriesCount || 0 })}
                        </div>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleSyncInstallation(installation.installationId)}
                        disabled={syncLoading === installation.installationId}
                      >
                        <RefreshCw className={`mr-1 h-3 w-3 ${syncLoading === installation.installationId ? 'animate-spin' : ''}`} />
                        {t('githubApp.sync')}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleRemoveInstallation(installation.installationId)}
                      >
                        <Trash2 className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Alert>
                <Github className="h-4 w-4" />
                <AlertDescription>
                  {t('githubApp.noInstallations')}
                </AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      {/* Manual Repository Setup Section */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <div>
              <CardTitle>{t('githubRepositoryManager.title')}</CardTitle>
              {appStatus?.configured && (
                <CardDescription className="mt-1">
                  {t('githubRepositoryManager.manualDescription')}
                </CardDescription>
              )}
            </div>
            <Button onClick={() => setOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              {t('githubRepositoryManager.addRepository')}
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <AlertDescription>
              <div className="space-y-2">
                <div className="font-semibold">{t('githubRepositoryManager.setupInstructions')}</div>
                <ol className="list-decimal list-inside space-y-1 text-sm">
                  <li>{t('githubRepositoryManager.setupStep1')}</li>
                  <li>{t('githubRepositoryManager.setupStep2')}</li>
                  <li>{t('githubRepositoryManager.setupStep3')}</li>
                  <li>{t('githubRepositoryManager.setupStep4')}</li>
                </ol>
              </div>
            </AlertDescription>
          </Alert>

          <div className="flex items-center gap-2">
            <Input
              value={`${window.location.origin}/api/github/webhook`}
              readOnly
              className="flex-1"
            />
            <Button
              variant="outline"
              size="icon"
              onClick={copyWebhookUrl}
              className={webhookUrlCopied ? 'text-green-600' : ''}
            >
              {webhookUrlCopied ? <CheckCircle2 className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
            </Button>
          </div>

          {repositories.length > 0 ? (
            <div className="space-y-3">
              {repositories.map((repo) => (
                <div key={repo.id} className="flex items-start justify-between p-3 border rounded-lg">
                  <div className="space-y-1 flex-1">
                    <div className="flex items-center gap-2">
                      <Github className="h-4 w-4" />
                      <span className="font-medium">{repo.fullName}</span>
                      {repo.isActive && <Badge variant="success">{t('githubRepositoryManager.active')}</Badge>}
                    </div>
                    <div className="text-sm text-muted-foreground space-y-0.5">
                      <div>{t('githubRepositoryManager.defaultBranch')}: {repo.defaultBranch}</div>
                      {repo.url && <div className="text-xs">{repo.url}</div>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Alert>
              <AlertDescription>
                {t('githubRepositoryManager.noRepositories')}
              </AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle>{t('githubRepositoryManager.addRepository')}</DialogTitle>
              <DialogDescription>
                {t('githubRepositoryManager.dialogDescription')}
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4 py-4">
              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <div className="space-y-2">
                <Label htmlFor="owner">{t('githubRepositoryManager.repositoryOwner')} *</Label>
                <Input
                  id="owner"
                  value={formData.owner}
                  onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
                  placeholder={t('githubRepositoryManager.ownerPlaceholder')}
                  required
                />
                <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.ownerHelp')}</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="name">{t('githubRepositoryManager.repositoryName')} *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder={t('githubRepositoryManager.namePlaceholder')}
                  required
                />
                <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.nameHelp')}</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="url">{t('githubRepositoryManager.repositoryUrl')}</Label>
                <Input
                  id="url"
                  value={formData.url}
                  onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                  placeholder={t('githubRepositoryManager.urlPlaceholder')}
                />
                <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.urlHelp')}</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="branch">{t('githubRepositoryManager.defaultBranch')}</Label>
                <Input
                  id="branch"
                  value={formData.defaultBranch}
                  onChange={(e) => setFormData({ ...formData, defaultBranch: e.target.value })}
                  placeholder={t('githubRepositoryManager.branchPlaceholder')}
                />
                <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.branchHelp')}</p>
              </div>

              <Separator />

              <div className="space-y-4">
                <h4 className="text-sm font-medium">{t('githubRepositoryManager.webhookConfig')}</h4>
                
                <div className="space-y-2">
                  <Label htmlFor="secret">{t('githubRepositoryManager.webhookSecret')}</Label>
                  <Input
                    id="secret"
                    type="password"
                    value={formData.webhookSecret}
                    onChange={(e) => setFormData({ ...formData, webhookSecret: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.secretHelp')}</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="token">{t('githubRepositoryManager.accessToken')}</Label>
                  <Input
                    id="token"
                    type="password"
                    value={formData.accessToken}
                    onChange={(e) => setFormData({ ...formData, accessToken: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">{t('githubRepositoryManager.tokenHelp')}</p>
                </div>
              </div>

              <Separator />

              <div className="space-y-4">
                <h4 className="text-sm font-medium">{t('githubRepositoryManager.autoLinkOptions')}</h4>
                
                <div className="space-y-3">
                  <div className="flex items-start space-x-3">
                    <Switch
                      id="autolink"
                      checked={formData.autoLinkEnabled}
                      onCheckedChange={(checked) => setFormData({ ...formData, autoLinkEnabled: checked })}
                    />
                    <div className="space-y-1 flex-1">
                      <Label htmlFor="autolink" className="cursor-pointer">
                        {t('githubRepositoryManager.autoLinkCommits')}
                      </Label>
                      <p className="text-xs text-muted-foreground">
                        {t('githubRepositoryManager.autoLinkCommitsHelp')}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start space-x-3">
                    <Switch
                      id="autoclose"
                      checked={formData.autoCloseTasksOnMerge}
                      onCheckedChange={(checked) => setFormData({ ...formData, autoCloseTasksOnMerge: checked })}
                    />
                    <div className="space-y-1 flex-1">
                      <Label htmlFor="autoclose" className="cursor-pointer">
                        {t('githubRepositoryManager.autoCloseTasks')}
                      </Label>
                      <p className="text-xs text-muted-foreground">
                        {t('githubRepositoryManager.autoCloseTasksHelp')}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? t('githubRepositoryManager.adding') : t('githubRepositoryManager.addRepository')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
