import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

// Dedicated axios instance for API key management (JWT-authenticated).
// Mirrors the token-interceptor pattern used in mcpService.ts.
const apiKeyApi = axios.create({
  baseURL: API_BASE_URL,
});

apiKeyApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * An API key as returned by the backend. The raw secret is never included here —
 * see {@link CreatedApiKey} for the one-time creation response.
 */
export interface ApiKey {
  id: number;
  name: string;
  keyPrefix: string;
  scopes: string[];
  isActive: boolean;
  expiresAt?: string | null;
  lastUsedAt?: string | null;
  createdAt?: string | null;
  revokedAt?: string | null;
}

/** The create response includes the raw key value exactly once. */
export interface CreatedApiKey extends ApiKey {
  rawKey: string;
}

export interface CreateApiKeyRequest {
  name: string;
  scopes: string[];
  /** ISO-8601 date-time string. Omit for a key that never expires. */
  expiresAt?: string;
}

// The backend uses Lombok @Data on a `Boolean isActive` field, so Jackson may
// serialize the property as either `isActive` or `active`. Normalize defensively.
function normalize(raw: Record<string, unknown>): ApiKey {
  return {
    id: raw.id as number,
    name: raw.name as string,
    keyPrefix: raw.keyPrefix as string,
    scopes: (raw.scopes as string[]) ?? [],
    isActive: Boolean(raw.isActive ?? raw.active),
    expiresAt: (raw.expiresAt as string) ?? null,
    lastUsedAt: (raw.lastUsedAt as string) ?? null,
    createdAt: (raw.createdAt as string) ?? null,
    revokedAt: (raw.revokedAt as string) ?? null,
  };
}

export async function listApiKeys(): Promise<ApiKey[]> {
  const response = await apiKeyApi.get<Record<string, unknown>[]>('/api/api-keys');
  return response.data.map(normalize);
}

export async function createApiKey(req: CreateApiKeyRequest): Promise<CreatedApiKey> {
  const response = await apiKeyApi.post<Record<string, unknown>>('/api/api-keys', req);
  const rawKey = response.data.rawKey as string | undefined;
  // The raw secret is returned exactly once. If it is missing (e.g. a proxy
  // stripped it), fail loudly rather than handing the user a blank credential
  // they can never recover.
  if (!rawKey) {
    throw new Error('API key was created but its secret was not returned. Please revoke it and create a new key.');
  }
  return {
    ...normalize(response.data),
    rawKey,
  };
}

export async function revokeApiKey(id: number): Promise<void> {
  await apiKeyApi.delete(`/api/api-keys/${id}`);
}

const apiKeyService = {
  listApiKeys,
  createApiKey,
  revokeApiKey,
};

export default apiKeyService;
