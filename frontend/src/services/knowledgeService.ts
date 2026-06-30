import api from './api';
import type {
  ChunkPreview,
  CreateKnowledgeSourceRequest,
  KnowledgeSource,
} from '../types/knowledge';

export const knowledgeService = {
  listOrg: () =>
    api.get<KnowledgeSource[]>('/knowledge/sources', { params: { scope: 'org' } }),

  listTeam: (teamId: number) =>
    api.get<KnowledgeSource[]>('/knowledge/sources', {
      params: { scope: 'team', teamId },
    }),

  listProject: (projectId: number) =>
    api.get<KnowledgeSource[]>('/knowledge/sources', {
      params: { scope: 'project', projectId },
    }),

  create: (body: CreateKnowledgeSourceRequest) =>
    api.post<KnowledgeSource>('/knowledge/sources', body),

  createWithFile: (meta: CreateKnowledgeSourceRequest, file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append(
      'metadata',
      new Blob([JSON.stringify(meta)], { type: 'application/json' }),
    );
    return api.post<KnowledgeSource>('/knowledge/sources', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  refresh: (id: number) => api.post<void>(`/knowledge/sources/${id}/refresh`),
  remove: (id: number) => api.delete<void>(`/knowledge/sources/${id}`),
  chunks: (id: number) =>
    api.get<ChunkPreview[]>(`/knowledge/sources/${id}/chunks`),
};
