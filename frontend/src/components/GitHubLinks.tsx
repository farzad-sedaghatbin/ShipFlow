import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Link,
  Stack,
  Divider,
  Tooltip,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  GitHub as GitHubIcon,
  CommitOutlined,
  MergeType,
  BranchesOutlined,
  CheckCircle,
  Cancel,
  Schedule,
} from '@mui/icons-material';
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
      return <MergeType sx={{ fontSize: 18, color: 'success.main' }} />;
    } else if (pr.state === 'CLOSED') {
      return <Cancel sx={{ fontSize: 18, color: 'error.main' }} />;
    } else {
      return <Schedule sx={{ fontSize: 18, color: 'info.main' }} />;
    }
  };

  const getPRStateChip = (state: string) => {
    const stateConfig: Record<string, { label: string; color: 'success' | 'error' | 'info' }> = {
      MERGED: { label: 'Merged', color: 'success' },
      CLOSED: { label: 'Closed', color: 'error' },
      OPEN: { label: 'Open', color: 'info' },
    };

    const config = stateConfig[state] || { label: state, color: 'info' };
    return <Chip label={config.label} color={config.color} size="small" />;
  };

  if (loading) {
    return (
      <Card>
        <CardContent>
          <Box display="flex" alignItems="center" justifyContent="center" py={3}>
            <CircularProgress size={24} />
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent>
          <Alert severity="error">{error}</Alert>
        </CardContent>
      </Card>
    );
  }

  if (links.length === 0) {
    return null; // Don't show card if no GitHub links
  }

  return (
    <Card>
      <CardContent>
        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <GitHubIcon />
          <Typography variant="h6">{title}</Typography>
          <Chip label={links.length} size="small" color="primary" />
        </Box>

        <Stack spacing={2} divider={<Divider />}>
          {links.map((link) => (
            <Box key={link.id}>
              {link.linkType === 'COMMIT' && link.commit && (
                <Box>
                  <Box display="flex" alignItems="flex-start" gap={1} mb={0.5}>
                    <CommitOutlined sx={{ fontSize: 18, mt: 0.3, color: 'text.secondary' }} />
                    <Box flex={1}>
                      <Link
                        href={link.commit.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        sx={{ fontWeight: 500, fontSize: '0.95rem' }}
                      >
                        {link.commit.message.split('\n')[0]}
                      </Link>
                      <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                        <Typography variant="caption" color="text.secondary">
                          {link.commit.sha.substring(0, 7)}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          by {link.commit.authorName}
                        </Typography>
                        {link.commit.branch && (
                          <Chip
                            label={link.commit.branch}
                            size="small"
                            variant="outlined"
                            sx={{ height: 18, fontSize: '0.7rem' }}
                          />
                        )}
                        <Typography variant="caption" color="text.secondary">
                          {formatDistanceToNow(new Date(link.commit.commitDate), { addSuffix: true })}
                        </Typography>
                        {link.autoLinked && (
                          <Tooltip title="Automatically linked from commit message">
                            <Chip label="Auto" size="small" color="success" sx={{ height: 18, fontSize: '0.7rem' }} />
                          </Tooltip>
                        )}
                      </Box>
                    </Box>
                  </Box>
                </Box>
              )}

              {link.linkType === 'PULL_REQUEST' && link.pullRequest && (
                <Box>
                  <Box display="flex" alignItems="flex-start" gap={1} mb={0.5}>
                    {getPRStateIcon(link.pullRequest)}
                    <Box flex={1}>
                      <Link
                        href={link.pullRequest.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        sx={{ fontWeight: 500, fontSize: '0.95rem' }}
                      >
                        #{link.pullRequest.prNumber} {link.pullRequest.title}
                      </Link>
                      <Box display="flex" alignItems="center" gap={1} mt={0.5} flexWrap="wrap">
                        {getPRStateChip(link.pullRequest.state)}
                        <Typography variant="caption" color="text.secondary">
                          by {link.pullRequest.authorUsername}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {link.pullRequest.headBranch} → {link.pullRequest.baseBranch}
                        </Typography>
                        {link.pullRequest.mergedAt && (
                          <Typography variant="caption" color="text.secondary">
                            merged {formatDistanceToNow(new Date(link.pullRequest.mergedAt), { addSuffix: true })}
                          </Typography>
                        )}
                        {link.pullRequest.state === 'OPEN' && (
                          <Typography variant="caption" color="text.secondary">
                            opened {formatDistanceToNow(new Date(link.pullRequest.openedAt), { addSuffix: true })}
                          </Typography>
                        )}
                        {link.autoLinked && (
                          <Tooltip title="Automatically linked from PR description">
                            <Chip label="Auto" size="small" color="success" sx={{ height: 18, fontSize: '0.7rem' }} />
                          </Tooltip>
                        )}
                      </Box>
                    </Box>
                  </Box>
                </Box>
              )}

              {link.linkType === 'BRANCH' && link.branch && (
                <Box>
                  <Box display="flex" alignItems="flex-start" gap={1}>
                    <BranchesOutlined sx={{ fontSize: 18, mt: 0.3, color: 'text.secondary' }} />
                    <Box flex={1}>
                      <Typography variant="body2" fontWeight={500}>
                        {link.branch.name}
                      </Typography>
                      <Box display="flex" alignItems="center" gap={1} mt={0.5}>
                        {link.branch.isDefault && <Chip label="Default" size="small" color="primary" />}
                        {link.branch.headSha && (
                          <Typography variant="caption" color="text.secondary">
                            {link.branch.headSha.substring(0, 7)}
                          </Typography>
                        )}
                        <Typography variant="caption" color="text.secondary">
                          {link.branch.repositoryFullName}
                        </Typography>
                      </Box>
                    </Box>
                  </Box>
                </Box>
              )}
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}
