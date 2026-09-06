import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldCheck, ShieldAlert, RefreshCw, Search, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import authAuditService, {
  type AuthAuditLogEntry,
  type AuthAuditOutcome,
} from '@/services/authAuditService';

const PAGE_SIZE = 50;

/**
 * Read-only view of the authentication audit trail: who signed in, from which
 * address and country, on what device, and whether it worked.
 *
 * Exists because a password change on a public instance could not be attributed
 * to anyone — logins were recorded nowhere at all.
 */
export function AuthAuditSettingsTab() {
  const { t } = useTranslation();

  const [entries, setEntries] = useState<AuthAuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [username, setUsername] = useState('');
  const [ipAddress, setIpAddress] = useState('');
  const [outcome, setOutcome] = useState<AuthAuditOutcome | 'ALL'>('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await authAuditService.list({
        username,
        ipAddress,
        outcome: outcome === 'ALL' ? undefined : outcome,
        page,
        size: PAGE_SIZE,
      });
      setEntries(result.content ?? []);
      setTotalPages(result.totalPages ?? 0);
      setTotalElements(result.totalElements ?? 0);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, [username, ipAddress, outcome, page]);

  useEffect(() => {
    load();
  }, [load]);

  const clearFilters = () => {
    setUsername('');
    setIpAddress('');
    setOutcome('ALL');
    setPage(0);
  };

  const hasFilters = Boolean(username || ipAddress || outcome !== 'ALL');

  const formatWhen = (iso: string) => {
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <h3 className="text-lg font-semibold">{t('authAudit.title', 'Sign-in activity')}</h3>
        <p className="text-sm text-muted-foreground mt-1">
          {t(
            'authAudit.description',
            'Every sign-in attempt, successful or not — with the originating IP address, country and device.'
          )}
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-end gap-2">
        <div className="flex-1 min-w-[160px]">
          <label className="text-xs text-muted-foreground" htmlFor="auth-audit-username">
            {t('authAudit.filterUsername', 'Username')}
          </label>
          <Input
            id="auth-audit-username"
            value={username}
            onChange={(e) => {
              setPage(0);
              setUsername(e.target.value);
            }}
            placeholder={t('authAudit.filterUsernamePlaceholder', 'e.g. admin')}
          />
        </div>

        <div className="flex-1 min-w-[160px]">
          <label className="text-xs text-muted-foreground" htmlFor="auth-audit-ip">
            {t('authAudit.filterIp', 'IP address')}
          </label>
          <Input
            id="auth-audit-ip"
            value={ipAddress}
            onChange={(e) => {
              setPage(0);
              setIpAddress(e.target.value);
            }}
            placeholder="203.0.113.7"
          />
        </div>

        <div className="min-w-[150px]">
          <label className="text-xs text-muted-foreground">{t('authAudit.filterOutcome', 'Outcome')}</label>
          <Select
            value={outcome}
            onValueChange={(v) => {
              setPage(0);
              setOutcome(v as AuthAuditOutcome | 'ALL');
            }}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">{t('authAudit.outcomeAll', 'All')}</SelectItem>
              <SelectItem value="SUCCESS">{t('authAudit.outcomeSuccess', 'Successful')}</SelectItem>
              <SelectItem value="FAILURE">{t('authAudit.outcomeFailure', 'Failed')}</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <Button variant="outline" size="icon" onClick={load} title={t('authAudit.refresh', 'Refresh')}>
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </Button>

        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters} className="gap-1">
            <X className="h-3 w-3" />
            {t('authAudit.clearFilters', 'Clear')}
          </Button>
        )}
      </div>

      {error && (
        <div className="rounded-md border border-destructive/40 bg-destructive/5 p-3 text-sm text-destructive">
          {t('authAudit.loadError', 'Could not load sign-in activity')}: {error}
        </div>
      )}

      {!loading && !error && entries.length === 0 && (
        <div className="rounded-md border border-dashed p-8 text-center">
          <Search className="mx-auto h-6 w-6 text-muted-foreground" />
          <p className="mt-2 text-sm font-medium">{t('authAudit.emptyTitle', 'No sign-in activity')}</p>
          <p className="text-sm text-muted-foreground">
            {hasFilters
              ? t('authAudit.emptyFiltered', 'No events match these filters.')
              : t('authAudit.emptyAll', 'Events will appear here as people sign in.')}
          </p>
        </div>
      )}

      {entries.length > 0 && (
        <div className="overflow-x-auto rounded-md border">
          <table className="w-full text-sm">
            <thead className="bg-muted/50">
              <tr className="text-left">
                <th className="p-2 font-medium">{t('authAudit.colWhen', 'When')}</th>
                <th className="p-2 font-medium">{t('authAudit.colUser', 'User')}</th>
                <th className="p-2 font-medium">{t('authAudit.colEvent', 'Event')}</th>
                <th className="p-2 font-medium">{t('authAudit.colIp', 'IP')}</th>
                <th className="p-2 font-medium">{t('authAudit.colDevice', 'Device')}</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr key={entry.id} className="border-t align-top">
                  <td className="p-2 whitespace-nowrap text-muted-foreground">{formatWhen(entry.createdAt)}</td>
                  <td className="p-2 font-medium">{entry.username ?? '—'}</td>
                  <td className="p-2">
                    <Badge variant={entry.outcome === 'SUCCESS' ? 'secondary' : 'destructive'} className="gap-1">
                      {entry.outcome === 'SUCCESS' ? (
                        <ShieldCheck className="h-3 w-3" />
                      ) : (
                        <ShieldAlert className="h-3 w-3" />
                      )}
                      {t(`authAudit.event.${entry.eventType}`, entry.eventType)}
                    </Badge>
                    {entry.failureReason && (
                      <div className="mt-1 text-xs text-muted-foreground">{entry.failureReason}</div>
                    )}
                  </td>
                  <td className="p-2 whitespace-nowrap font-mono text-xs">
                    {entry.ipAddress ?? '—'}
                    {entry.country && <span className="ml-1 text-muted-foreground">({entry.country})</span>}
                  </td>
                  {/* Full UA on hover — the summary is a best-effort guess. */}
                  <td className="p-2 text-muted-foreground" title={entry.userAgent ?? undefined}>
                    {entry.deviceSummary ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            {t('authAudit.pageOf', 'Page {{page}} of {{total}} · {{count}} events', {
              page: page + 1,
              total: totalPages,
              count: totalElements,
            })}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              {t('common.previous', 'Previous')}
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              {t('common.next', 'Next')}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export default AuthAuditSettingsTab;
