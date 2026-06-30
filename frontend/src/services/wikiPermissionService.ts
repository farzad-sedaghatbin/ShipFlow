import api from './api';

/**
 * Wiki space access-control (sharing) service.
 *
 * Mirrors the backend `WikiController` permission endpoints:
 *  - GET    /api/wiki/spaces/{id}/permissions       → list grants (requires READ)
 *  - POST   /api/wiki/spaces/{id}/permissions       → grant       (requires WRITE)
 *  - DELETE /api/wiki/permissions/{permId}          → revoke      (requires WRITE)
 *
 * A grant targets either a single USER or an entire ROLE. For USER grants the
 * `granteeRef` carries the user id as a string; for ROLE grants it carries the
 * role name (e.g. "MEMBER"). The `level` is READ (view) or WRITE (edit).
 */

export type WikiGranteeType = 'USER' | 'ROLE';
export type WikiPermissionLevel = 'READ' | 'WRITE';

export interface WikiSpacePermissionDTO {
  id: number;
  spaceId: number;
  granteeType: WikiGranteeType;
  /** User id (as string) for USER grants, or role name for ROLE grants. */
  granteeRef: string;
  level: WikiPermissionLevel;
}

export interface GrantWikiPermissionRequest {
  granteeType: WikiGranteeType;
  granteeRef: string;
  level: WikiPermissionLevel;
}

export const wikiPermissionService = {
  /** List all access grants on a wiki space. */
  list: (spaceId: number) =>
    api.get<WikiSpacePermissionDTO[]>(`/wiki/spaces/${spaceId}/permissions`),

  /** Grant a USER or ROLE access (READ/WRITE) to a wiki space. */
  grant: (spaceId: number, req: GrantWikiPermissionRequest) =>
    api.post<WikiSpacePermissionDTO>(`/wiki/spaces/${spaceId}/permissions`, req),

  /** Revoke a previously granted permission by its id. */
  revoke: (permId: number) => api.delete(`/wiki/permissions/${permId}`),
};

export default wikiPermissionService;
