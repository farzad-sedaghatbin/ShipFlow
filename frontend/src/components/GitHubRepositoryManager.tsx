import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  FormControlLabel,
  Switch,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Paper,
} from '@mui/material';
import { Add, GitHub, Delete, Edit, ContentCopy, CheckCircle } from '@mui/icons-material';
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
    <Box>
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h6">GitHub Integration</Typography>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => setOpen(true)}
          >
            Add Repository
          </Button>
        </Box>

        <Alert severity="info" sx={{ mb: 2 }}>
          <Typography variant="body2" gutterBottom>
            <strong>Setup Instructions:</strong>
          </Typography>
          <Typography variant="body2" component="div">
            1. Add your repository below<br />
            2. Copy the webhook URL and configure it in your GitHub repository settings<br />
            3. Set webhook events: Push, Pull Request, Create, Delete<br />
            4. Optionally add a webhook secret for security
          </Typography>
        </Alert>

        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <TextField
            size="small"
            value={`${window.location.origin}/api/github/webhook`}
            InputProps={{
              readOnly: true,
            }}
            fullWidth
            label="Webhook URL"
          />
          <IconButton onClick={copyWebhookUrl} color={webhookUrlCopied ? 'success' : 'default'}>
            {webhookUrlCopied ? <CheckCircle /> : <ContentCopy />}
          </IconButton>
        </Box>

        {repositories.length > 0 ? (
          <List>
            {repositories.map((repo) => (
              <ListItem key={repo.id} divider>
                <ListItemText
                  primary={
                    <Box display="flex" alignItems="center" gap={1}>
                      <GitHub fontSize="small" />
                      <Typography variant="body1" fontWeight={500}>
                        {repo.fullName}
                      </Typography>
                      {repo.isActive && <Chip label="Active" size="small" color="success" />}
                    </Box>
                  }
                  secondary={
                    <Box>
                      <Typography variant="caption" display="block">
                        Default branch: {repo.defaultBranch}
                      </Typography>
                      {repo.url && (
                        <Typography variant="caption" display="block" color="text.secondary">
                          {repo.url}
                        </Typography>
                      )}
                    </Box>
                  }
                />
              </ListItem>
            ))}
          </List>
        ) : (
          <Alert severity="info">
            No repositories configured yet. Click "Add Repository" to get started.
          </Alert>
        )}
      </Paper>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>Add GitHub Repository</DialogTitle>
          <DialogContent>
            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <TextField
              fullWidth
              label="Repository Owner"
              value={formData.owner}
              onChange={(e) => setFormData({ ...formData, owner: e.target.value })}
              margin="normal"
              required
              helperText="GitHub username or organization name"
            />

            <TextField
              fullWidth
              label="Repository Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              margin="normal"
              required
              helperText="Repository name (e.g., my-project)"
            />

            <TextField
              fullWidth
              label="Repository URL"
              value={formData.url}
              onChange={(e) => setFormData({ ...formData, url: e.target.value })}
              margin="normal"
              helperText="Optional: GitHub repository URL"
            />

            <TextField
              fullWidth
              label="Default Branch"
              value={formData.defaultBranch}
              onChange={(e) => setFormData({ ...formData, defaultBranch: e.target.value })}
              margin="normal"
              helperText="Usually 'main' or 'master'"
            />

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" gutterBottom>
              Webhook Configuration
            </Typography>

            <TextField
              fullWidth
              label="Webhook Secret"
              type="password"
              value={formData.webhookSecret}
              onChange={(e) => setFormData({ ...formData, webhookSecret: e.target.value })}
              margin="normal"
              helperText="Optional: Secret for validating webhook requests"
            />

            <TextField
              fullWidth
              label="Access Token"
              type="password"
              value={formData.accessToken}
              onChange={(e) => setFormData({ ...formData, accessToken: e.target.value })}
              margin="normal"
              helperText="Optional: GitHub Personal Access Token for private repos"
            />

            <Divider sx={{ my: 2 }} />

            <Typography variant="subtitle2" gutterBottom>
              Auto-linking Options
            </Typography>

            <FormControlLabel
              control={
                <Switch
                  checked={formData.autoLinkEnabled}
                  onChange={(e) => setFormData({ ...formData, autoLinkEnabled: e.target.checked })}
                />
              }
              label="Auto-link commits and PRs to tasks"
            />

            <Typography variant="caption" display="block" color="text.secondary" mb={1}>
              Automatically link commits and PRs that mention "Task #123" or "Pitch #45"
            </Typography>

            <FormControlLabel
              control={
                <Switch
                  checked={formData.autoCloseTasksOnMerge}
                  onChange={(e) => setFormData({ ...formData, autoCloseTasksOnMerge: e.target.checked })}
                />
              }
              label="Auto-close tasks when PR is merged"
            />

            <Typography variant="caption" display="block" color="text.secondary">
              Automatically close tasks when PRs with "closes #123", "fixes #123", or "resolves #123" are merged
            </Typography>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={loading}>
              {loading ? 'Adding...' : 'Add Repository'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
