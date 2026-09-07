import api from './api';

export type LicenseStatus = 'MISSING' | 'VALID' | 'GRACE' | 'EXPIRED';

/**
 * Note: the backend's Jackson config omits null fields entirely from the
 * response JSON (the key is absent, not present with a `null` value), so
 * every nullable field below should be treated as possibly-undefined at
 * runtime as well as possibly-null.
 */
export interface LicenseStatusResponse {
  status: LicenseStatus;
  edition: string | null; // e.g. "COMMERCIAL", null when no licence
  licensee: string | null;
  seats: number | null; // licensed seat count from the payload, null when no licence
  seatsUsed: number; // current active user count — always present regardless of status
  features: string[]; // e.g. ["sso","audit-export"] — [] when no licence
  issuedAt: string | null; // ISO date "yyyy-MM-dd"
  expiresAt: string | null;
  supportUntil: string | null;
  graceEndsAt: string | null; // expiresAt + 30 days; only meaningful when status === 'GRACE'
  communityCap: number; // always 10 (the Community Edition user cap), regardless of status
  automationCap: number; // always 5 (the Community Edition automation cap), regardless of status
  automationsUsed: number; // current enabled-automation count — always present
}

/**
 * Service for managing the ShipFlow licence — checking the current status
 * and uploading/removing a signed licence file. A valid licence unlocks
 * commercial features and lifts the Community Edition's user/automation
 * caps (see LicenseStatusResponse.communityCap / automationCap).
 */
export const licenseService = {
  /**
   * Load the current licence status. Any authenticated user may call this —
   * the backend endpoint has no role restriction, unlike upload/remove.
   */
  getStatus: () => api.get<LicenseStatusResponse>('/license/status'),

  /**
   * Upload a licence file's raw text content (admin only, enforced
   * server-side). Returns the refreshed status on success. On failure the
   * backend returns 400 with `messageKey: "license.invalid"` for malformed
   * or unverifiable content, or a generic validation 400 for blank content.
   */
  uploadLicense: (content: string) => api.post<LicenseStatusResponse>('/license', { content }),

  /**
   * Remove the currently installed licence (admin only, enforced
   * server-side). Returns the refreshed status — typically `MISSING` unless
   * a fallback licence is configured server-side.
   */
  removeLicense: () => api.delete<LicenseStatusResponse>('/license'),
};
