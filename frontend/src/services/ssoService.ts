import api from './api';

// ── Types ────────────────────────────────────────────────────────────────────

export interface IdentityProviderDTO {
  id: number;
  name: string;
  providerType: 'SAML2' | 'OIDC';
  clientId?: string;
  metadataUrl?: string;
  entityId?: string;
  ssoUrl?: string;
  isEnabled: boolean;
  enforceSso: boolean;
}

export interface CreateIdentityProviderRequest {
  name: string;
  providerType: 'SAML2' | 'OIDC';
  clientId?: string;
  clientSecret?: string;
  metadataUrl?: string;
  entityId?: string;
  ssoUrl?: string;
  certificate?: string;
  isEnabled: boolean;
  enforceSso: boolean;
}

export interface SsoInitiateResponse {
  redirectUrl: string;
  state: string;
}

// ── Public endpoints (no auth required) ──────────────────────────────────────

/**
 * Returns the list of identity providers that are enabled and visible on the
 * login page.
 */
export const getEnabledProviders = (): Promise<IdentityProviderDTO[]> =>
  api.get<IdentityProviderDTO[]>('/sso/providers').then((r) => r.data);

/**
 * Initiates an SSO flow for the given provider. Returns a redirect URL that
 * the browser must be sent to.
 */
export const initiateSSO = (idpId: number): Promise<SsoInitiateResponse> =>
  api.get<SsoInitiateResponse>(`/sso/initiate/${idpId}`).then((r) => r.data);

// ── Admin endpoints ───────────────────────────────────────────────────────────

/**
 * Lists all identity providers (enabled and disabled) for admin management.
 */
export const listAllProviders = (): Promise<IdentityProviderDTO[]> =>
  api.get<IdentityProviderDTO[]>('/admin/sso/providers').then((r) => r.data);

/**
 * Creates a new identity provider.
 */
export const createProvider = (
  req: CreateIdentityProviderRequest
): Promise<IdentityProviderDTO> =>
  api.post<IdentityProviderDTO>('/admin/sso/providers', req).then((r) => r.data);

/**
 * Updates an existing identity provider.
 */
export const updateProvider = (
  id: number,
  req: CreateIdentityProviderRequest
): Promise<IdentityProviderDTO> =>
  api.put<IdentityProviderDTO>(`/admin/sso/providers/${id}`, req).then((r) => r.data);

/**
 * Deletes an identity provider.
 */
export const deleteProvider = (id: number): Promise<void> =>
  api.delete(`/admin/sso/providers/${id}`).then(() => undefined);
