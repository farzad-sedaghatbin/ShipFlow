export interface GitHubRepository {
  id: number;
  owner: string;
  name: string;
  fullName: string;
  url: string;
  defaultBranch: string;
  isActive: boolean;
}

export interface GitHubCommit {
  id: number;
  sha: string;
  message: string;
  authorName: string;
  authorEmail?: string;
  authorUsername?: string;
  commitDate: string;
  branch?: string;
  url: string;
  repositoryFullName: string;
}

export interface GitHubPullRequest {
  id: number;
  prNumber: number;
  title: string;
  description?: string;
  state: 'OPEN' | 'CLOSED' | 'MERGED';
  headBranch: string;
  baseBranch: string;
  authorUsername: string;
  url: string;
  openedAt: string;
  closedAt?: string;
  mergedAt?: string;
  mergedByUsername?: string;
  repositoryFullName: string;
}

export interface GitHubBranch {
  id: number;
  name: string;
  headSha?: string;
  isDefault: boolean;
  repositoryFullName: string;
}

export type GitHubLinkType = 'COMMIT' | 'PULL_REQUEST' | 'BRANCH';

export interface GitHubLink {
  id: number;
  linkType: GitHubLinkType;
  commit?: GitHubCommit;
  pullRequest?: GitHubPullRequest;
  branch?: GitHubBranch;
  linkedAt: string;
  linkedByUsername?: string;
  autoLinked?: boolean;
}

export interface CreateGitHubRepositoryRequest {
  owner: string;
  name: string;
  url?: string;
  defaultBranch?: string;
  webhookSecret?: string;
  accessToken?: string;
  autoLinkEnabled?: boolean;
  autoCloseTasksOnMerge?: boolean;
}
