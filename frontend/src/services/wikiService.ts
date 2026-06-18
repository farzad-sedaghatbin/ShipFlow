import api from './api';

// ─── DTOs ─────────────────────────────────────────────────────────────────────

export interface WikiSpaceDTO {
  id: number;
  name: string;
  spaceKey: string;
  description?: string | null;
  projectId?: number | null;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface WikiTreeNodeDTO {
  id: number;
  title: string;
  slug: string;
  position: number;
  children: WikiTreeNodeDTO[];
}

export interface WikiPageDTO {
  id: number;
  spaceId: number;
  parentId: number | null;
  title: string;
  slug: string;
  content: string | null;
  contentText: string | null;
  position: number;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface WikiRevisionDTO {
  revision: number;
  timestamp: string;
  editorId: number;
  title: string;
}

export interface WikiAttachmentDTO {
  id: number;
  pageId: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  uploadedBy: number;
  createdAt: string;
}

export interface WikiSpacePermissionDTO {
  id: number;
  spaceId: number;
  userId: number | null;
  role: string;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

export interface CreateWikiSpaceRequest {
  name: string;
  spaceKey: string;
  description?: string;
  projectId?: number | null;
}

export interface UpdateWikiSpaceRequest {
  name?: string;
  description?: string;
}

export interface CreateWikiPageRequest {
  spaceId: number;
  parentId?: number | null;
  title: string;
  content?: string | null;
}

export interface UpdateWikiPageRequest {
  title?: string;
  content?: string | null;
}

export interface MovePageRequest {
  newParentId?: number | null;
  newIndex: number;
}

export interface GrantPermissionRequest {
  userId: number;
  role: string;
}

export interface WikiPageSearchDTO {
  id: number;
  title: string;
  spaceId: number;
  spaceKey: string;
  slug: string;
  excerpt: string | null;
}

// ─── Service ──────────────────────────────────────────────────────────────────

export const wikiService = {
  // Spaces
  createSpace: (req: CreateWikiSpaceRequest) =>
    api.post<WikiSpaceDTO>('/wiki/spaces', req),

  getSpaces: () =>
    api.get<WikiSpaceDTO[]>('/wiki/spaces'),

  getSpace: (id: number) =>
    api.get<WikiSpaceDTO>(`/wiki/spaces/${id}`),

  updateSpace: (id: number, req: UpdateWikiSpaceRequest) =>
    api.put<WikiSpaceDTO>(`/wiki/spaces/${id}`, req),

  deleteSpace: (id: number) =>
    api.delete(`/wiki/spaces/${id}`),

  getSpaceTree: (id: number) =>
    api.get<WikiTreeNodeDTO[]>(`/wiki/spaces/${id}/tree`),

  getSpacePermissions: (id: number) =>
    api.get<WikiSpacePermissionDTO[]>(`/wiki/spaces/${id}/permissions`),

  grantPermission: (id: number, req: GrantPermissionRequest) =>
    api.post<WikiSpacePermissionDTO>(`/wiki/spaces/${id}/permissions`, req),

  revokePermission: (permId: number) =>
    api.delete(`/wiki/permissions/${permId}`),

  // Pages
  createPage: (req: CreateWikiPageRequest) =>
    api.post<WikiPageDTO>('/wiki/pages', req),

  getPage: (id: number) =>
    api.get<WikiPageDTO>(`/wiki/pages/${id}`),

  updatePage: (id: number, req: UpdateWikiPageRequest) =>
    api.put<WikiPageDTO>(`/wiki/pages/${id}`, req),

  deletePage: (id: number) =>
    api.delete(`/wiki/pages/${id}`),

  movePage: (id: number, req: MovePageRequest) =>
    api.post<WikiPageDTO>(`/wiki/pages/${id}/move`, req),

  getPageHistory: (id: number) =>
    api.get<WikiRevisionDTO[]>(`/wiki/pages/${id}/history`),

  restorePage: (id: number, revision: number) =>
    api.post<WikiPageDTO>(`/wiki/pages/${id}/restore/${revision}`),

  searchPages: (q: string, spaceId?: number) =>
    api.get<WikiPageSearchDTO[]>('/wiki/pages/search', { params: { q, spaceId } }),

  // Attachments
  uploadAttachment: (pageId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<WikiAttachmentDTO>(`/wiki/pages/${pageId}/attachments`, form);
  },

  getAttachments: (pageId: number) =>
    api.get<WikiAttachmentDTO[]>(`/wiki/pages/${pageId}/attachments`),

  deleteAttachment: (attId: number) =>
    api.delete(`/wiki/attachments/${attId}`),

  /** Returns a browser-navigable URL — use as href or window.open target. */
  getAttachmentDownloadUrl: (attId: number) =>
    `/api/wiki/attachments/${attId}/download`,
};
