import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { KeyRound, Trash2, Copy, Check, AlertCircle, CheckCircle2 } from 'lucide-react';
import { Button } from '../../components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../../components/ui/card';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Badge } from '../../components/ui/badge';
import { Checkbox } from '../../components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../../components/ui/dialog';
import { Alert, AlertDescription } from '../../components/ui/alert';
import {
  listApiKeys,
  createApiKey,
  revokeApiKey,
  ApiKey,
  CreatedApiKey,
} from '../../services/apiKeyService';

const ALL_SCOPES = ['READ', 'WRITE', 'ADMIN'] as const;

/**
 * Standalone API Keys management page.
 *
 * Unlike {@link McpIntegration}, this page is accessible to every authenticated
 * user (not just admins) so members can mint personal keys to connect external
 * tools (MCP, CI, scripts) without needing admin elevation. The backend
 * {@code ApiKeyController} already scopes keys to the authenticated user.
 */
export default function ApiKeysPage() {
  const { t } = useTranslation();
  const [apiKeys, setApiKeys] = useState<ApiKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [keyName, setKeyName] = useState('');
  const [keyScopes, setKeyScopes] = useState<string[]>(['READ']);
  const [keyExpiresAt, setKeyExpiresAt] = useState('');
  const [creating, setCreating] = useState(false);
  const [createdKey, setCreatedKey] = useState<CreatedApiKey | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    refresh();
  }, []);

  useEffect(() => {
    if (success || error) {
      const timer = setTimeout(() => {
        setSuccess(null);
        setError(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [success, error]);

  const refresh = async () => {
    try {
      setLoading(true);
      setApiKeys(await listApiKeys());
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.fetchFailed'));
      setApiKeys([]);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setKeyName('');
    setKeyScopes(['READ']);
    setKeyExpiresAt('');
  };

  const toggleScope = (scope: string) => {
    setKeyScopes((prev) =>
      prev.includes(scope) ? prev.filter((s) => s !== scope) : [...prev, scope]
    );
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!keyName.trim() || keyScopes.length === 0) return;
    setError(null);
    setSuccess(null);
    setCreating(true);
    try {
      const created = await createApiKey({
        name: keyName.trim(),
        scopes: keyScopes,
        expiresAt: keyExpiresAt ? `${keyExpiresAt}T00:00:00` : undefined,
      });
      setShowCreateDialog(false);
      resetForm();
      setCreatedKey(created);
      setCopied(false);
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.keyCreateError'));
    } finally {
      setCreating(false);
    }
  };

  const handleRevoke = async (id: number) => {
    if (!window.confirm(t('mcpIntegration.keyRevokeConfirm'))) return;
    setError(null);
    setSuccess(null);
    try {
      await revokeApiKey(id);
      setSuccess(t('mcpIntegration.keyRevokeSuccess'));
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.message || t('mcpIntegration.keyRevokeError'));
    }
  };

  const handleCopy = async () => {
    if (!createdKey) return;
    try {
      await navigator.clipboard.writeText(createdKey.rawKey);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  const formatDate = (value?: string | null) => {
    if (!value) return t('mcpIntegration.never');
    return new Date(value).toLocaleDateString();
  };

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <KeyRound className="h-6 w-6" />
          {t('apiKeysPage.title')}
        </h1>
        <p className="text-muted-foreground mt-1">{t('apiKeysPage.description')}</p>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {success && (
        <Alert>
          <CheckCircle2 className="h-4 w-4" />
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>{t('mcpIntegration.apiKeysTitle')}</CardTitle>
              <CardDescription>{t('mcpIntegration.apiKeysDescription')}</CardDescription>
            </div>
            <Button
              onClick={() => {
                resetForm();
                setShowCreateDialog(true);
              }}
            >
              {t('mcpIntegration.createKey')}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-muted-foreground text-center py-8">{t('common.loading')}</p>
          ) : apiKeys.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              {t('mcpIntegration.noApiKeys')}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-muted-foreground">
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableName')}</th>
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTablePrefix')}</th>
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableScopes')}</th>
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableLastUsed')}</th>
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableCreated')}</th>
                    <th className="py-2 pr-4 font-medium">{t('mcpIntegration.keyTableStatus')}</th>
                    <th className="py-2 pr-4 font-medium text-right">
                      {t('mcpIntegration.keyTableActions')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {apiKeys.map((key) => (
                    <tr key={key.id} className="border-b last:border-0">
                      <td className="py-2 pr-4">{key.name}</td>
                      <td className="py-2 pr-4">
                        <code className="font-mono text-xs">{key.keyPrefix}…</code>
                      </td>
                      <td className="py-2 pr-4">
                        <div className="flex flex-wrap gap-1">
                          {key.scopes.map((scope) => (
                            <Badge key={scope} variant="secondary">
                              {scope}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="py-2 pr-4">{formatDate(key.lastUsedAt)}</td>
                      <td className="py-2 pr-4">{formatDate(key.createdAt)}</td>
                      <td className="py-2 pr-4">
                        <Badge variant={key.isActive ? 'default' : 'secondary'}>
                          {key.isActive
                            ? t('mcpIntegration.keyStatusActive')
                            : t('mcpIntegration.keyStatusRevoked')}
                        </Badge>
                      </td>
                      <td className="py-2 pr-4 text-right">
                        {key.isActive && (
                          <Button variant="ghost" size="sm" onClick={() => handleRevoke(key.id)}>
                            <Trash2 className="h-4 w-4 mr-1" />
                            {t('mcpIntegration.revokeKey')}
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={showCreateDialog} onOpenChange={(open) => !open && setShowCreateDialog(false)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.createKeyTitle')}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleCreate} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="keyName">{t('mcpIntegration.keyName')}</Label>
              <Input
                id="keyName"
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>{t('mcpIntegration.keyScopes')}</Label>
              <div className="space-y-2">
                {ALL_SCOPES.map((scope) => (
                  <div key={scope} className="flex items-center gap-2">
                    <Checkbox
                      id={`scope-${scope}`}
                      checked={keyScopes.includes(scope)}
                      onCheckedChange={() => toggleScope(scope)}
                    />
                    <Label htmlFor={`scope-${scope}`} className="font-normal">
                      {t(`mcpIntegration.scope${scope}`)}
                    </Label>
                  </div>
                ))}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="keyExpiresAt">{t('mcpIntegration.keyExpiresAt')}</Label>
              <Input
                id="keyExpiresAt"
                type="date"
                value={keyExpiresAt}
                onChange={(e) => setKeyExpiresAt(e.target.value)}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setShowCreateDialog(false)}>
                {t('common.cancel')}
              </Button>
              <Button
                type="submit"
                disabled={creating || !keyName.trim() || keyScopes.length === 0}
              >
                {creating ? t('mcpIntegration.creating') : t('mcpIntegration.createKey')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!createdKey} onOpenChange={(open) => !open && setCreatedKey(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('mcpIntegration.keyCreatedTitle')}</DialogTitle>
            <DialogDescription>{t('mcpIntegration.keyCreatedDescription')}</DialogDescription>
          </DialogHeader>
          {createdKey && (
            <div className="space-y-3">
              <div className="rounded-md border bg-muted/30 p-3 font-mono text-sm break-all">
                {createdKey.rawKey}
              </div>
              <Button variant="outline" size="sm" onClick={handleCopy} className="w-full">
                {copied ? (
                  <>
                    <Check className="h-4 w-4 mr-2" />
                    {t('mcpIntegration.copied')}
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4 mr-2" />
                    {t('mcpIntegration.copyKey')}
                  </>
                )}
              </Button>
            </div>
          )}
          <DialogFooter>
            <Button onClick={() => setCreatedKey(null)}>{t('common.close')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
