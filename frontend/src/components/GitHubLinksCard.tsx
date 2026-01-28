import { useEffect, useState } from 'react';
import { ExternalLink, GitBranch, GitCommit, GitPullRequest, Clock, XCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';
import { GitHubLink } from '../types/github';
import { githubService } from '../services/githubService';
import { formatDistanceToNow } from 'date-fns';

interface GitHubLinksCardProps {
  taskId?: number;
  pitchId?: number;
  title?: string;
}

export default function GitHubLinksCard({ taskId, pitchId, title = 'GitHub Activity' }: GitHubLinksCardProps) {
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

  const getPRStateIcon = (state: string) => {
    if (state === 'MERGED') {
      return <GitPullRequest className="h-4 w-4 text-green-500" />;
    } else if (state === 'CLOSED') {
      return <XCircle className="h-4 w-4 text-red-500" />;
    } else {
      return <Clock className="h-4 w-4 text-blue-500" />;
    }
  };

  const getPRStateVariant = (state: string): 'default' | 'success' | 'destructive' | 'info' => {
    const mapping: Record<string, 'default' | 'success' | 'destructive' | 'info'> = {
      MERGED: 'success',
      CLOSED: 'destructive',
      OPEN: 'info',
    };
    return mapping[state] || 'default';
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <GitBranch className="h-5 w-5" />
            {title}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <GitBranch className="h-5 w-5" />
            {title}
          </CardTitle>
        </CardHeader>
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
      <CardHeader>
        <CardTitle className="text-lg flex items-center gap-2">
          <GitBranch className="h-5 w-5" />
          {title}
          <Badge variant="secondary">{links.length}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {links.map((link) => (
            <div key={link.id} className="border-b pb-4 last:border-b-0 last:pb-0">
              {/* Commit */}
              {link.linkType === 'COMMIT' && link.commit && (
                <div className="space-y-2">
                  <div className="flex items-start gap-2">
                    <GitCommit className="h-4 w-4 mt-0.5 text-muted-foreground" />
                    <div className="flex-1 min-w-0">
                      <a
                        href={link.commit.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium hover:underline text-sm line-clamp-2"
                      >
                        {link.commit.message.split('\n')[0]}
                      </a>
                      <div className="flex items-center gap-2 mt-1 flex-wrap">
                        <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                          {link.commit.sha.substring(0, 7)}
                        </code>
                        <span className="text-xs text-muted-foreground">
                          by {link.commit.authorName}
                        </span>
                        {link.commit.branch && (
                          <Badge variant="outline" className="text-xs">
                            {link.commit.branch}
                          </Badge>
                        )}
                        <span className="text-xs text-muted-foreground">
                          {formatDistanceToNow(new Date(link.commit.commitDate), { addSuffix: true })}
                        </span>
                        {link.autoLinked && (
                          <Badge variant="success" className="text-xs">
                            Auto
                          </Badge>
                        )}
                      </div>
                    </div>
                    <a
                      href={link.commit.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-muted-foreground hover:text-foreground"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </div>
                </div>
              )}

              {/* Pull Request */}
              {link.linkType === 'PULL_REQUEST' && link.pullRequest && (
                <div className="space-y-2">
                  <div className="flex items-start gap-2">
                    {getPRStateIcon(link.pullRequest.state)}
                    <div className="flex-1 min-w-0">
                      <a
                        href={link.pullRequest.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium hover:underline text-sm line-clamp-2"
                      >
                        #{link.pullRequest.prNumber} {link.pullRequest.title}
                      </a>
                      <div className="flex items-center gap-2 mt-1 flex-wrap">
                        <Badge variant={getPRStateVariant(link.pullRequest.state)} className="text-xs">
                          {link.pullRequest.state}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          by {link.pullRequest.authorUsername}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {link.pullRequest.headBranch} → {link.pullRequest.baseBranch}
                        </span>
                        {link.pullRequest.mergedAt && (
                          <span className="text-xs text-muted-foreground">
                            merged {formatDistanceToNow(new Date(link.pullRequest.mergedAt), { addSuffix: true })}
                          </span>
                        )}
                        {link.pullRequest.state === 'OPEN' && (
                          <span className="text-xs text-muted-foreground">
                            opened {formatDistanceToNow(new Date(link.pullRequest.openedAt), { addSuffix: true })}
                          </span>
                        )}
                        {link.autoLinked && (
                          <Badge variant="success" className="text-xs">
                            Auto
                          </Badge>
                        )}
                      </div>
                    </div>
                    <a
                      href={link.pullRequest.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-muted-foreground hover:text-foreground"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </a>
                  </div>
                </div>
              )}

              {/* Branch */}
              {link.linkType === 'BRANCH' && link.branch && (
                <div className="space-y-2">
                  <div className="flex items-start gap-2">
                    <GitBranch className="h-4 w-4 mt-0.5 text-muted-foreground" />
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm">{link.branch.name}</div>
                      <div className="flex items-center gap-2 mt-1 flex-wrap">
                        {link.branch.isDefault && (
                          <Badge variant="default" className="text-xs">
                            Default
                          </Badge>
                        )}
                        {link.branch.headSha && (
                          <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                            {link.branch.headSha.substring(0, 7)}
                          </code>
                        )}
                        <span className="text-xs text-muted-foreground">
                          {link.branch.repositoryFullName}
                        </span>
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
