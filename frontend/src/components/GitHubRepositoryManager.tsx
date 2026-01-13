import React, { useState, useEffect } from 'react';
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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Plus, Github, Trash2, Copy, CheckCircle2 } from 'lucide-react';
import { githubService } from '../services/githubService';
import { GitHubRepository, CreateGitHubRepositoryRequest } from '../types/github';

export default function GitHubRepositoryManager() {
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
      setError(err.response?.data?.message || 'Failed to register repository');
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
            <CardTitle>GitHub Integration</CardTitle>
            <Button onClick={() => setOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add Repository
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert>
            <AlertDescription>
              <div className="space-y-2">
                <div className="font-semibold">Setup Instructions:</div>
                <ol className="list-decimal list-inside space-y-1 text-sm">
                  <li>Add your repository below</li>
                  <li>Copy the webhook URL and configure it in your GitHub repository settings</li>
                  <li>Set webhook events: Push, Pull Request, Create, Delete</li>
                  <li>Optionally add a webhook secret for security</li>
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
                      {repo.isActive && <Badge variant="success">Active</Badge>}
                    </div>
                    <div className="text-sm text-muted-foreground space-y-0.5">
                      <div>Default branch: {repo.defaultBranch}</div>
                      {repo.url && <div className="text-xs">{repo.url}</div>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <Alert>
              <AlertDescription>
                No repositories configured yet. Click "Add Repository" to get started.
              </AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
          <form onSubmit={handleSubmit}>
            <DialogHeader>
              <DialogTitle>Add GitHub Repository</DialogTitle>
              <DialogDescription>
                Configure a GitHub repository for automatic linking
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4 py-4">
              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <div className="space-y-2">
                <Label htmlFor="owner">Repository Owner *</Label>
                <Input
                  id="owner"
                  value={formData.owner}
                  onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
                  placeholder="GitHub username or organization"
                  required
                />
                <p className="text-xs text-muted-foreground">GitHub username or organization name</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="name">Repository Name *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="my-project"
                  required
                />
                <p className="text-xs text-muted-foreground">Repository name (e.g., my-project)</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="url">Repository URL</Label>
                <Input
                  id="url"
                  value={formData.url}
                  onChange={(e) => setFormData({ ...formData, url: e.target.value })}
                  placeholder="https://github.com/owner/repo"
                />
                <p className="text-xs text-muted-foreground">Optional: GitHub repository URL</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="branch">Default Branch</Label>
                <Input
                  id="branch"
                  value={formData.defaultBranch}
                  onChange={(e) => setFormData({ ...formData, defaultBranch: e.target.value })}
                  placeholder="main"
                />
                <p className="text-xs text-muted-foreground">Usually 'main' or 'master'</p>
              </div>

              <Separator />

              <div className="space-y-4">
                <h4 className="text-sm font-medium">Webhook Configuration</h4>
                
                <div className="space-y-2">
                  <Label htmlFor="secret">Webhook Secret</Label>
                  <Input
                    id="secret"
                    type="password"
                    value={formData.webhookSecret}
                    onChange={(e) => setFormData({ ...formData, webhookSecret: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">Optional: Secret for validating webhook requests</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="token">Access Token</Label>
                  <Input
                    id="token"
                    type="password"
                    value={formData.accessToken}
                    onChange={(e) => setFormData({ ...formData, accessToken: e.target.value })}
                  />
                  <p className="text-xs text-muted-foreground">Optional: GitHub Personal Access Token for private repos</p>
                </div>
              </div>

              <Separator />

              <div className="space-y-4">
                <h4 className="text-sm font-medium">Auto-linking Options</h4>
                
                <div className="space-y-3">
                  <div className="flex items-start space-x-3">
                    <Switch
                      id="autolink"
                      checked={formData.autoLinkEnabled}
                      onCheckedChange={(checked) => setFormData({ ...formData, autoLinkEnabled: checked })}
                    />
                    <div className="space-y-1 flex-1">
                      <Label htmlFor="autolink" className="cursor-pointer">
                        Auto-link commits and PRs to tasks
                      </Label>
                      <p className="text-xs text-muted-foreground">
                        Automatically link commits and PRs that mention "Task #123" or "Pitch #45"
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
                        Auto-close tasks when PR is merged
                      </Label>
                      <p className="text-xs text-muted-foreground">
                        Automatically close tasks when PRs with "closes #123", "fixes #123", or "resolves #123" are merged
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? 'Adding...' : 'Add Repository'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
