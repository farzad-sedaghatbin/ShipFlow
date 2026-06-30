export type KnowledgeProviderType =
  | 'FILE_UPLOAD'
  | 'URL'
  | 'GITHUB'
  | 'CONFLUENCE'
  | 'NOTION'
  | 'GOOGLE_DRIVE';

export type KnowledgeSourceScope = 'ORG' | 'TEAM' | 'PROJECT';

export type KnowledgeSourceStatus =
  | 'PENDING'
  | 'INGESTING'
  | 'READY'
  | 'FAILED'
  | 'STALE';

export interface KnowledgeSource {
  id: number;
  name: string;
  description?: string;
  providerType: KnowledgeProviderType;
  scope: KnowledgeSourceScope;
  teamId?: number | null;
  projectId?: number | null;
  configJson: string;
  status: KnowledgeSourceStatus;
  lastIngestedAt?: string | null;
  lastError?: string | null;
  chunkCount: number;
  createdAt: string;
}

export interface CreateKnowledgeSourceRequest {
  name: string;
  description?: string;
  providerType: KnowledgeProviderType;
  scope: KnowledgeSourceScope;
  teamId?: number;
  projectId?: number;
  config: Record<string, unknown>;
}

export interface ChunkPreview {
  id: number;
  title: string;
  contentPreview: string;
  ordinal: number;
  embedded: boolean;
}
