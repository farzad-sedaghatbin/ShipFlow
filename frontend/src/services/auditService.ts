import api from './api';

// ── Types ────────────────────────────────────────────────────────────────────

export type AuditEntityType = 'task' | 'bug' | 'pitch' | 'testcase' | 'all';
export type AuditExportFormat = 'csv' | 'json';

export interface AuditExportParams {
  entityType: AuditEntityType;
  format: AuditExportFormat;
  /** Optional inclusive start date, ISO `YYYY-MM-DD`. */
  from?: string;
  /** Optional inclusive end date, ISO `YYYY-MM-DD`. */
  to?: string;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Pull the `filename` out of a `Content-Disposition` header, supporting both the
 * plain `filename="..."` and RFC 5987 `filename*=UTF-8''...` forms. Returns
 * `null` when the header is absent or unparseable.
 */
function parseFilename(disposition: string | undefined | null): string | null {
  if (!disposition) return null;

  // RFC 5987 extended form takes precedence (e.g. filename*=UTF-8''audit.csv)
  const extendedMatch = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(disposition);
  if (extendedMatch?.[1]) {
    try {
      return decodeURIComponent(extendedMatch[1].trim().replace(/^"|"$/g, ''));
    } catch {
      // fall through to the plain form
    }
  }

  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition);
  if (plainMatch?.[1]) return plainMatch[1].trim();

  return null;
}

/** Today as `YYYY-MM-DD`, used for the fallback download filename. */
function today(): string {
  return new Date().toISOString().slice(0, 10);
}

// ── Service ──────────────────────────────────────────────────────────────────

export const auditService = {
  /**
   * Download the Envers audit trail export through the authenticated axios
   * client and trigger a browser save. The endpoint is admin-only and requires
   * the JWT bearer token, so a raw `<a href>` would 401 — we must fetch the blob
   * via `api` (the request interceptor attaches the token) and synthesize the
   * download from an object URL.
   */
  exportAuditTrail: async ({ entityType, format, from, to }: AuditExportParams): Promise<void> => {
    const response = await api.get<Blob>('/audit/export', {
      params: {
        entityType,
        format,
        ...(from && { from }),
        ...(to && { to }),
      },
      responseType: 'blob',
    });

    const fallbackName = `audit-${entityType}-${today()}.${format === 'json' ? 'json' : 'csv'}`;
    const fileName =
      parseFilename(response.headers?.['content-disposition'] as string | undefined) ??
      fallbackName;

    const url = window.URL.createObjectURL(response.data);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
