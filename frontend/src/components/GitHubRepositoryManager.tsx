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
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Plus, Github, Copy, CheckCircle2 } from 'lucide-react';
import { githubService } from '../services/githubService';
import { GitHubRepository, CreateGitHubRepositoryRequest } from '../types/github';

export default function GitHubRepositoryManager() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [webhookUrlCopied, setWebhookUrlCopied] = useState(false);
  
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
  }, []);

  const loadRepositories = async () => {
    try {
      const data = await githubService.getAllRepositories();
      setRepositories(data);
    } catch (err) {
      console.error('Failed to load repositories:', err);
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
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>{t('githubRepositoryManager.title')}</CardTitle>
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
