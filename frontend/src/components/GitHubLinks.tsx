import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader } from './ui/card';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { 
  Github, 
  GitCommit, 
  GitMerge, 
  GitBranch, 
  XCircle, 
  Clock,
  Loader2
} from 'lucide-react';
import { GitHubLink, GitHubPullRequest } from '../types/github';
import { githubService } from '../services/githubService';
import { formatDistanceToNow } from 'date-fns';

interface GitHubLinksProps {
  taskId?: number;
  pitchId?: number;
  title?: string;
}

export default function GitHubLinks({ taskId, pitchId, title = 'GitHub Activity' }: GitHubLinksProps) {
  const [links, setLinks] = useState<GitHubLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadGitHubLinks();
  }, [taskId, pitchId]);

  const loadGitHubLinks = async () => {
    try {
      setLoading(true);
      setError(null);
      
      if (taskId) {
        const data = await githubService.getTaskGitHubLinks(taskId);
        setLinks(data);
      } else if (pitchId) {
        const data = await githubService.getPitchGitHubLinks(pitchId);
        setLinks(data);
      }
    } catch (err) {
      console.error('Failed to load GitHub links:', err);
      setError('Failed to load GitHub activity');
    } finally {
      setLoading(false);
    }
  };

  const getPRStateIcon = (pr: GitHubPullRequest) => {
    if (pr.state === 'MERGED') {
      return <GitMerge className="h-4 w-4 text-green-600" />;
    } else if (pr.state === 'CLOSED') {
      return <XCircle className="h-4 w-4 text-red-600" />;
    } else {
      return <Clock className="h-4 w-4 text-blue-600" />;
    }
  };

  const getPRStateChip = (state: string) => {
    const stateConfig: Record<string, { label: string; className: string }> = {
      MERGED: { label: 'Merged', className: 'bg-green-100 text-green-800' },
      CLOSED: { label: 'Closed', className: 'bg-red-100 text-red-800' },
      OPEN: { label: 'Open', className: 'bg-blue-100 text-blue-800' },
    };

    const config = stateConfig[state] || { label: state, className: 'bg-blue-100 text-blue-800' };
    return <Badge variant="secondary" className={config.className}>{config.label}</Badge>;
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent>
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (links.length === 0) {
    return null; // Don't show card if no GitHub links
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center gap-2">
          <Github className="h-5 w-5" />
          <h3 className="text-lg font-semibold">{title}</h3>
          <Badge variant="default">{links.length}</Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {links.map((link, index) => (
            <div key={link.id}>
              {index > 0 && <Separator className="my-4" />}
              {link.linkType === 'COMMIT' && link.commit && (
                <div>
                  <div className="flex items-start gap-2">
                    <GitCommit className="h-4 w-4 mt-1 text-muted-foreground" />
                    <div className="flex-1 space-y-1">
                      <a
                        href={link.commit.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium text-sm hover:underline"
                      >
                        {link.commit.message.split('\n')[0]}
                      </a>
                      <div className="flex items-center gap-2 flex-wrap text-xs text-muted-foreground">
                        <code className="px-1 py-0.5 bg-muted rounded">
                          {link.commit.sha.substring(0, 7)}
                        </code>
                        <span>by {link.commit.authorName}</span>
                        {link.commit.branch && (
                          <Badge variant="outline" className="h-5 text-xs">
                            {link.commit.branch}
                          </Badge>
                        )}
                        <span>{formatDistanceToNow(new Date(link.commit.commitDate), { addSuffix: true })}</span>
                        {link.autoLinked && (
                          <Badge variant="secondary" className="h-5 text-xs bg-green-100 text-green-800" title="Automatically linked from commit message">
                            Auto
                          </Badge>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {link.linkType === 'PULL_REQUEST' && link.pullRequest && (
                <div>
                  <div className="flex items-start gap-2">
                    {getPRStateIcon(link.pullRequest)}
                    <div className="flex-1 space-y-1">
                      <a
                        href={link.pullRequest.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium text-sm hover:underline"
                      >
                        #{link.pullRequest.prNumber} {link.pullRequest.title}
                      </a>
                      <div className="flex items-center gap-2 flex-wrap text-xs text-muted-foreground">
                        {getPRStateChip(link.pullRequest.state)}
                        <span>by {link.pullRequest.authorUsername}</span>
                        <span>{link.pullRequest.headBranch} → {link.pullRequest.baseBranch}</span>
                        {link.pullRequest.mergedAt && (
                          <span>merged {formatDistanceToNow(new Date(link.pullRequest.mergedAt), { addSuffix: true })}</span>
                        )}
                        {link.pullRequest.state === 'OPEN' && (
                          <span>opened {formatDistanceToNow(new Date(link.pullRequest.openedAt), { addSuffix: true })}</span>
                        )}
                        {link.autoLinked && (
                          <Badge variant="secondary" className="h-5 text-xs bg-green-100 text-green-800" title="Automatically linked from PR description">
                            Auto
                          </Badge>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {link.linkType === 'BRANCH' && link.branch && (
                <div>
                  <div className="flex items-start gap-2">
                    <GitBranch className="h-4 w-4 mt-1 text-muted-foreground" />
                    <div className="flex-1 space-y-1">
                      <div className="font-medium text-sm">
                        {link.branch.name}
                      </div>
                      <div className="flex items-center gap-2 flex-wrap text-xs text-muted-foreground">
                        {link.branch.isDefault && <Badge variant="default" className="h-5 text-xs">Default</Badge>}
                        {link.branch.headSha && (
                          <code className="px-1 py-0.5 bg-muted rounded">
                            {link.branch.headSha.substring(0, 7)}
                          </code>
                        )}
                        <span>{link.branch.repositoryFullName}</span>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
