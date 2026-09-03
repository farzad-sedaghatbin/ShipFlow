import api from './api';

// ── Types ────────────────────────────────────────────────────────────────────

export type AuthAuditEventType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'PASSKEY_LOGIN_SUCCESS'
  | 'PASSKEY_LOGIN_FAILURE'
  | 'PASSKEY_REGISTERED'
  | 'PASSKEY_REMOVED'
  | 'PASSWORD_CHANGED'
  | 'PASSWORD_RESET_REQUESTED'
  | 'PASSWORD_RESET_COMPLETED'
  | 'ACCOUNT_REGISTERED'
  | 'LOGOUT';

export type AuthAuditOutcome = 'SUCCESS' | 'FAILURE';

export interface AuthAuditLogEntry {
  id: number;
  eventType: AuthAuditEventType;
  outcome: AuthAuditOutcome;
  /** Username exactly as supplied — may not match a real account on failures. */
  username: string | null;
  userId: number | null;
  ipAddress: string | null;
  /** Two-letter country from Cloudflare, when the request was proxied by it. */
  country: string | null;
  deviceSummary: string | null;
  userAgent: string | null;
  failureReason: string | null;
  createdAt: string;
}

export interface AuthAuditQuery {
  username?: string;
  ipAddress?: string;
  outcome?: AuthAuditOutcome;
  /** ISO date-time, inclusive. */
  from?: string;
  /** ISO date-time, inclusive. */
  to?: string;
  page?: number;
  size?: number;
}

export interface AuthAuditPage {
  content: AuthAuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── API ──────────────────────────────────────────────────────────────────────

/**
 * Fetches authentication events, newest first. Admin-only server side.
 * Blank filters are omitted rather than sent empty, so the backend's
 * "null matches everything" semantics apply.
 */
async function list(query: AuthAuditQuery = {}): Promise<AuthAuditPage> {
  const params: Record<string, string | number> = {
    page: query.page ?? 0,
    size: query.size ?? 50,
  };

  if (query.username?.trim()) params.username = query.username.trim();
  if (query.ipAddress?.trim()) params.ipAddress = query.ipAddress.trim();
  if (query.outcome) params.outcome = query.outcome;
  if (query.from) params.from = query.from;
  if (query.to) params.to = query.to;

  const response = await api.get<AuthAuditPage>('/audit/auth', { params });
  return response.data;
}

export const authAuditService = { list };
export default authAuditService;
