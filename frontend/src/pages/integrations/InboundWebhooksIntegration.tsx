import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Copy, RefreshCw, ArrowDownToLine } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import {
  inboundWebhookService,
  InboundWebhookConfig,
  CreateInboundWebhookConfigRequest,
} from '@/services/inboundWebhookService';

const HMAC_ALGORITHMS = ['HmacSHA256', 'HmacSHA1', 'HmacSHA512'];

const emptyForm: CreateInboundWebhookConfigRequest = {
  providerName: '',
  displayName: '',
  webhookSecret: '',
  signatureHeader: '',
  hmacAlgorithm: 'HmacSHA256',
  description: '',
  isEnabled: true,
};

export default function InboundWebhooksIntegration() {
  const { t } = useTranslation();

  // ── State ────────────────────────────────────────────────────────
  const [configurations, setConfigurations] = useState<InboundWebhookConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [configToDelete, setConfigToDelete] = useState<number | null>(null);
  const [editingConfig, setEditingConfig] = useState<InboundWebhookConfig | null>(null);

  // Form
  const [form, setForm] = useState<CreateInboundWebhookConfigRequest>({ ...emptyForm });

  // ── Data fetching ────────────────────────────────────────────────
  const fetchConfigurations = useCallback(async () => {
    try {
      setLoading(true);
      const { data } = await inboundWebhookService.getAllConfigurations();
      setConfigurations(data);
      setError(null);
    } catch {
      setError(t('inboundWebhooks.fetchError', 'Failed to load inbound webhook configurations.'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => { fetchConfigurations(); }, [fetchConfigurations]);

  // Auto-clear alerts
  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => setSuccess(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [success]);

  // ── Handlers ─────────────────────────────────────────────────────

  const openCreateDialog = () => {
    setEditingConfig(null);
    setForm({ ...emptyForm });
    setDialogOpen(true);
  };

  const openEditDialog = (config: InboundWebhookConfig) => {
    setEditingConfig(config);
    setForm({
      providerName: config.providerName,
      displayName: config.displayName || '',
      webhookSecret: '', // don't pre-fill masked secret
      signatureHeader: config.signatureHeader || '',
      hmacAlgorithm: config.hmacAlgorithm || 'HmacSHA256',
      description: config.description || '',
      isEnabled: config.isEnabled,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      await inboundWebhookService.createConfiguration(form);
      setDialogOpen(false);
      setSuccess(editingConfig
        ? t('inboundWebhooks.updated', 'Provider configuration updated.')
        : t('inboundWebhooks.created', 'Provider configuration created.'));
      fetchConfigurations();
    } catch {
      setError(t('inboundWebhooks.saveError', 'Failed to save configuration.'));
    }
  };

  const handleToggle = async (id: number) => {
    try {
      await inboundWebhookService.toggleConfiguration(id);
      fetchConfigurations();
    } catch {
      setError(t('inboundWebhooks.toggleError', 'Failed to toggle provider.'));
    }
  };

  const handleDelete = async () => {
    if (configToDelete == null) return;
    try {
      await inboundWebhookService.deleteConfiguration(configToDelete);
      setDeleteConfirmOpen(false);
      setConfigToDelete(null);
      setSuccess(t('inboundWebhooks.deleted', 'Provider configuration deleted.'));
      fetchConfigurations();
    } catch {
      setError(t('inboundWebhooks.deleteError', 'Failed to delete configuration.'));
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setSuccess(t('inboundWebhooks.copied', 'Webhook URL copied to clipboard.'));
  };

  // ── Render ───────────────────────────────────────────────────────
  return (
    <div className="w-full max-w-none space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <ArrowDownToLine className="h-7 w-7" />
            {t('inboundWebhooks.title', 'Inbound Webhooks')}
          </h1>
          <p className="text-muted-foreground mt-1">
            {t('inboundWebhooks.subtitle', 'Configure external services to push events into ShipFlow.')}
          </p>
        </div>
        <Button onClick={openCreateDialog} className="gap-2">
          <Plus className="h-4 w-4" />
          {t('inboundWebhooks.addProvider', 'Add Provider')}
        </Button>
      </div>

      {/* Alerts */}
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {success && (
        <Alert>
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      {/* Instructions Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">{t('inboundWebhooks.howItWorks', 'How It Works')}</CardTitle>
          <CardDescription>
            {t('inboundWebhooks.howItWorksDesc', 'Any external service that supports outgoing webhooks can push events into ShipFlow.')}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground">
            <li>{t('inboundWebhooks.step1', 'Click "Add Provider" and enter a name (e.g., intercom, zendesk, pagerduty).')}</li>
            <li>{t('inboundWebhooks.step2', 'Copy the generated webhook URL and paste it in your external service\'s webhook settings.')}</li>
            <li>{t('inboundWebhooks.step3', 'Enter the shared secret from the external service for signature validation.')}</li>
            <li>{t('inboundWebhooks.step4', 'Events sent by the external service will now appear in ShipFlow automatically.')}</li>
          </ol>
        </CardContent>
      </Card>

      {/* Configurations Table */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>{t('inboundWebhooks.configurations', 'Configured Providers')}</CardTitle>
            <CardDescription>
              {t('inboundWebhooks.configurationsDesc', 'Manage inbound webhook providers and their security settings.')}
            </CardDescription>
          </div>
          <Button variant="outline" size="sm" onClick={fetchConfigurations} className="gap-2">
            <RefreshCw className="h-4 w-4" />
            {t('common.refresh', 'Refresh')}
          </Button>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-muted-foreground text-center py-8">{t('common.loading', 'Loading...')}</p>
          ) : configurations.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">
              {t('inboundWebhooks.empty', 'No inbound webhook providers configured yet. Click "Add Provider" to get started.')}
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('inboundWebhooks.provider', 'Provider')}</TableHead>
                  <TableHead>{t('inboundWebhooks.webhookUrl', 'Webhook URL')}</TableHead>
                  <TableHead>{t('inboundWebhooks.algorithm', 'Algorithm')}</TableHead>
                  <TableHead>{t('inboundWebhooks.status', 'Status')}</TableHead>
                  <TableHead className="text-right">{t('common.actions', 'Actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {configurations.map((config) => (
                  <TableRow key={config.id}>
                    <TableCell>
                      <div>
                        <p className="font-medium">{config.displayName || config.providerName}</p>
                        {config.displayName && (
                          <p className="text-xs text-muted-foreground font-mono">{config.providerName}</p>
                        )}
                        {config.description && (
                          <p className="text-xs text-muted-foreground mt-0.5">{config.description}</p>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <code className="text-xs bg-muted px-2 py-1 rounded max-w-[300px] truncate">
                          {config.webhookUrl}
                        </code>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => copyToClipboard(config.webhookUrl)}
                          title={t('inboundWebhooks.copyUrl', 'Copy URL')}
                        >
                          <Copy className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary" className="text-xs">
                        {config.hmacAlgorithm || 'HmacSHA256'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Switch
                        checked={config.isEnabled}
                        onCheckedChange={() => handleToggle(config.id)}
                      />
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openEditDialog(config)}
                        >
                          {t('common.edit', 'Edit')}
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          onClick={() => {
                            setConfigToDelete(config.id);
                            setDeleteConfirmOpen(true);
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* ── Create / Edit Dialog ───────────────────────────────────── */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              {editingConfig
                ? t('inboundWebhooks.editProvider', 'Edit Provider')
                : t('inboundWebhooks.addProvider', 'Add Provider')}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>{t('inboundWebhooks.providerNameLabel', 'Provider Name')}</Label>
              <Input
                placeholder="e.g., intercom, zendesk, pagerduty"
                value={form.providerName}
                onChange={(e) => setForm({ ...form, providerName: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') })}
                disabled={!!editingConfig}
              />
              <p className="text-xs text-muted-foreground">
                {t('inboundWebhooks.providerNameHint', 'Lowercase letters, numbers, and hyphens only. This becomes part of your webhook URL.')}
              </p>
            </div>

            <div className="space-y-2">
              <Label>{t('inboundWebhooks.displayNameLabel', 'Display Name (optional)')}</Label>
              <Input
                placeholder="e.g., Intercom Production"
                value={form.displayName || ''}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label>{t('inboundWebhooks.webhookSecretLabel', 'Webhook Secret')}</Label>
              <Input
                type="password"
                placeholder={editingConfig ? t('inboundWebhooks.leaveBlank', 'Leave blank to keep current') : t('inboundWebhooks.secretPlaceholder', 'Shared secret from the external service')}
                value={form.webhookSecret || ''}
                onChange={(e) => setForm({ ...form, webhookSecret: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                {t('inboundWebhooks.secretHint', 'The shared secret used for HMAC signature validation. Find this in your external service webhook settings.')}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>{t('inboundWebhooks.signatureHeaderLabel', 'Signature Header')}</Label>
                <Input
                  placeholder="e.g., X-Hub-Signature-256"
                  value={form.signatureHeader || ''}
                  onChange={(e) => setForm({ ...form, signatureHeader: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label>{t('inboundWebhooks.algorithmLabel', 'HMAC Algorithm')}</Label>
                <select
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm"
                  value={form.hmacAlgorithm || 'HmacSHA256'}
                  onChange={(e) => setForm({ ...form, hmacAlgorithm: e.target.value })}
                >
                  {HMAC_ALGORITHMS.map((alg) => (
                    <option key={alg} value={alg}>{alg}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="space-y-2">
              <Label>{t('inboundWebhooks.descriptionLabel', 'Description (optional)')}</Label>
              <Input
                placeholder={t('inboundWebhooks.descriptionPlaceholder', 'Notes about this provider integration')}
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>

            <div className="flex items-center gap-2">
              <Switch
                checked={form.isEnabled ?? true}
                onCheckedChange={(checked) => setForm({ ...form, isEnabled: checked })}
              />
              <Label>{t('inboundWebhooks.enabledLabel', 'Enabled')}</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              {t('common.cancel', 'Cancel')}
            </Button>
            <Button onClick={handleSave} disabled={!form.providerName.trim()}>
              {editingConfig ? t('common.save', 'Save') : t('inboundWebhooks.create', 'Create Provider')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Delete Confirm ─────────────────────────────────────────── */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title={t('inboundWebhooks.deleteTitle', 'Delete Provider')}
        description={t('inboundWebhooks.deleteDescription', 'Are you sure you want to delete this inbound webhook provider? Any external services still sending events will receive errors.')}
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
}
