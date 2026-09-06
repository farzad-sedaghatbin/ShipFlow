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

/**
 * A resolved internal `[[pageId]]` link found in a page's body.
 * `exists=false` means the target page was deleted/missing; `url` then points at
 * the canonical id route (`/wiki/pages/{id}`) and the UI renders a broken link.
 */
export interface WikiPageLinkDTO {
  pageId: number;
  title: string | null;
  spaceId: number | null;
  exists: boolean;
  url: string;
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
  /**
   * Internal `[[pageId]]` links resolved by the backend from the page body.
   * Used by the view to render the "Linked pages" affordance.
   */
  pageLinks: WikiPageLinkDTO[];
  /** Optimistic-lock version. Present on every fetch/save response as of v1.13.0 (S64); echo it back as `expectedVersion` on the next update. */
  version?: number;
}

/**
 * Convenience alias used across the wiki UI. Identical to {@link WikiPageDTO}.
 */
export type WikiPage = WikiPageDTO;

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
  /** Optimistic-lock check (v1.13.0 S64) — pass the last-known WikiPageDTO.version; a mismatch returns HTTP 409. */
  expectedVersion?: number;
}

export interface MovePageRequest {
  newParentId?: number | null;
  newIndex: number;
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

  getPageRevision: (id: number, revision: number) =>
    api.get<WikiPageDTO>(`/wiki/pages/${id}/revisions/${revision}`),

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

  /**
   * Download an attachment through the authenticated axios client and trigger a
   * browser save. A plain `<a href>` cannot be used: the JWT lives in
   * localStorage and is only attached by the axios request interceptor, so a raw
   * browser GET to the secured endpoint returns 401.
   */
  downloadAttachment: async (attId: number, fileName: string) => {
    const response = await api.get<Blob>(`/wiki/attachments/${attId}/download`, {
      responseType: 'blob',
    });
    const url = window.URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  /**
   * Fetch an attachment's bytes (authenticated) and return an object URL for
   * inline preview (e.g. an `<img src>`). Caller MUST revoke the URL when done.
   */
  fetchAttachmentObjectUrl: async (attId: number): Promise<string> => {
    const response = await api.get<Blob>(`/wiki/attachments/${attId}/download`, {
      responseType: 'blob',
    });
    return window.URL.createObjectURL(response.data);
  },
};
