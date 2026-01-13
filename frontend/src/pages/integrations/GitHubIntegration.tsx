import GitHubRepositoryManager from '../../components/GitHubRepositoryManager';

export default function GitHubIntegration() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">GitHub Integration</h1>
        <p className="text-muted-foreground mt-2">
          Manage GitHub repository integrations and webhook configurations
        </p>
      </div>
      <GitHubRepositoryManager />
    </div>
  );
}
