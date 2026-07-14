import api from './api';

// ─── Types ────────────────────────────────────────────────────────────────────

/**
 * Entities that can reference a wiki page for context. Bug reports are
 * intentionally excluded from this feature.
 */
export type WikiLinkEntityType = 'PITCH' | 'TASK';

export interface LinkedWikiPageDTO {
  linkId: number;
  wikiPageId: number;
  title: string;
  slug: string;
  spaceId: number;
  spaceName: string | null;
  linkedAt: string;
  linkedByName: string | null;
}

// ─── Service ──────────────────────────────────────────────────────────────────

export const wikiLinkService = {
  listLinkedWikiPages: (entityType: WikiLinkEntityType, entityId: number) =>
    api.get<LinkedWikiPageDTO[]>(`/wiki-links/${entityType}/${entityId}`),

  linkWikiPage: (entityType: WikiLinkEntityType, entityId: number, wikiPageId: number) =>
    api.post<LinkedWikiPageDTO>(`/wiki-links/${entityType}/${entityId}`, { wikiPageId }),

  unlinkWikiPage: (entityType: WikiLinkEntityType, entityId: number, wikiPageId: number) =>
    api.delete(`/wiki-links/${entityType}/${entityId}/${wikiPageId}`),
};
