import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus, MoreVertical, Shield, AlertTriangle, Pencil, Trash2 } from 'lucide-react';
import {
  listAllProviders,
  createProvider,
  updateProvider,
  deleteProvider,
  type IdentityProviderDTO,
  type CreateIdentityProviderRequest,
} from '../../services/ssoService';
import { useToast } from '../../contexts';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Badge } from '../ui/badge';
import { Alert, AlertDescription } from '../ui/alert';
import { Card, CardContent } from '../ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '../ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { Switch } from '../ui/switch';
import { Textarea } from '../ui/textarea';
import { ConfirmDialog } from '../ui/confirm-dialog';

// ── Zod schema ────────────────────────────────────────────────────────────────

const providerSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  providerType: z.enum(['OIDC', 'SAML2']),
  clientId: z.string().optional(),
  clientSecret: z.string().optional(),
  metadataUrl: z.string().optional(),
  entityId: z.string().optional(),
  ssoUrl: z.string().optional(),
  certificate: z.string().optional(),
  isEnabled: z.boolean(),
  enforceSso: z.boolean(),
});

type ProviderFormData = z.infer<typeof providerSchema>;

// ── Provider dialog ───────────────────────────────────────────────────────────

interface ProviderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  existing?: IdentityProviderDTO;
  onSaved: () => void;
}

function ProviderDialog({ open, onOpenChange, existing, onSaved }: ProviderDialogProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ProviderFormData>({
    resolver: zodResolver(providerSchema),
    defaultValues: existing
      ? {
          name: existing.name,
          providerType: existing.providerType,
          clientId: existing.clientId ?? '',
          clientSecret: '',
          metadataUrl: existing.metadataUrl ?? '',
          entityId: existing.entityId ?? '',
          ssoUrl: existing.ssoUrl ?? '',
          certificate: '',
          isEnabled: existing.isEnabled,
          enforceSso: existing.enforceSso,
        }
      : {
          name: '',
          providerType: 'OIDC',
          clientId: '',
          clientSecret: '',
          metadataUrl: '',
          entityId: '',
          ssoUrl: '',
          certificate: '',
          isEnabled: true,
          enforceSso: false,
        },
  });

  const providerType = watch('providerType');
  const enforceSso = watch('enforceSso');

  const onSubmit = async (data: ProviderFormData) => {
    const req: CreateIdentityProviderRequest = {
      name: data.name,
      providerType: data.providerType,
      isEnabled: data.isEnabled,
      enforceSso: data.enforceSso,
      ...(data.clientId && { clientId: data.clientId }),
      ...(data.clientSecret && { clientSecret: data.clientSecret }),
      ...(data.metadataUrl && { metadataUrl: data.metadataUrl }),
      ...(data.entityId && { entityId: data.entityId }),
      ...(data.ssoUrl && { ssoUrl: data.ssoUrl }),
      ...(data.certificate && { certificate: data.certificate }),
    };

    try {
      if (existing) {
        await updateProvider(existing.id, req);
      } else {
        await createProvider(req);
      }
      showToast(t('sso.saveSuccess'), 'success');
      reset();
      onSaved();
      onOpenChange(false);
    } catch {
      showToast(t('sso.saveFailed'), 'error');
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {existing ? t('sso.editProvider') : t('sso.addProvider')}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Name */}
          <div className="space-y-2">
            <Label htmlFor="sso-name">{t('sso.providerName')}</Label>
            <Input
              id="sso-name"
              {...register('name')}
              placeholder="e.g. Okta, Azure AD"
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name.message}</p>
            )}
          </div>

          {/* Provider type */}
          <div className="space-y-2">
            <Label>{t('sso.providerType')}</Label>
            <Controller
              name="providerType"
              control={control}
              render={({ field }) => (
                <div className="flex gap-4">
                  {(['OIDC', 'SAML2'] as const).map((type) => (
                    <label
                      key={type}
                      className="flex items-center gap-2 cursor-pointer"
                    >
                      <input
                        type="radio"
                        value={type}
                        checked={field.value === type}
                        onChange={() => field.onChange(type)}
                        className="accent-primary"
                      />
                      <span className="text-sm font-medium">
                        {type === 'OIDC' ? t('sso.oidc') : t('sso.saml2')}
                      </span>
                    </label>
                  ))}
                </div>
              )}
            />
          </div>

          {/* OIDC fields */}
          {providerType === 'OIDC' && (
            <>
              <div className="space-y-2">
                <Label htmlFor="sso-client-id">{t('sso.clientId')}</Label>
                <Input id="sso-client-id" {...register('clientId')} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sso-client-secret">{t('sso.clientSecret')}</Label>
                <Input
                  id="sso-client-secret"
                  type="password"
                  {...register('clientSecret')}
                  placeholder={existing ? t('sso.clientSecretPlaceholder') : ''}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sso-metadata-url-oidc">{t('sso.metadataUrl')}</Label>
                <Input
                  id="sso-metadata-url-oidc"
                  {...register('metadataUrl')}
                  placeholder="https://accounts.example.com/.well-known/openid-configuration"
                />
              </div>
            </>
          )}

          {/* SAML2 fields */}
          {providerType === 'SAML2' && (
            <>
              <div className="space-y-2">
                <Label htmlFor="sso-entity-id">{t('sso.entityId')}</Label>
                <Input id="sso-entity-id" {...register('entityId')} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sso-sso-url">{t('sso.ssoUrl')}</Label>
                <Input
                  id="sso-sso-url"
                  {...register('ssoUrl')}
                  placeholder="https://idp.example.com/sso/saml"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sso-metadata-url-saml">{t('sso.metadataUrl')}</Label>
                <Input
                  id="sso-metadata-url-saml"
                  {...register('metadataUrl')}
                  placeholder="https://idp.example.com/metadata.xml"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sso-certificate">{t('sso.certificate')}</Label>
                <Textarea
                  id="sso-certificate"
                  {...register('certificate')}
                  rows={4}
                  placeholder="-----BEGIN CERTIFICATE-----&#10;...&#10;-----END CERTIFICATE-----"
                  className="font-mono text-xs"
                />
              </div>
            </>
          )}

          {/* Enabled toggle */}
          <div className="flex items-center justify-between rounded-md border p-3">
            <Label htmlFor="sso-enabled" className="cursor-pointer">
              {t('sso.enabled')}
            </Label>
            <Controller
              name="isEnabled"
              control={control}
              render={({ field }) => (
                <Switch
                  id="sso-enabled"
                  checked={field.value}
                  onCheckedChange={field.onChange}
                />
              )}
            />
          </div>

          {/* Enforce SSO toggle */}
          <div className="space-y-2">
            <div className="flex items-center justify-between rounded-md border p-3">
              <Label htmlFor="sso-enforce" className="cursor-pointer">
                {t('sso.enforceSso')}
              </Label>
              <Controller
                name="enforceSso"
                control={control}
                render={({ field }) => (
                  <Switch
                    id="sso-enforce"
                    checked={field.value}
                    onCheckedChange={field.onChange}
                  />
                )}
              />
            </div>
            {enforceSso && (
              <Alert variant="destructive" className="py-2">
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription className="text-xs">
                  {t('sso.enforceSsoWarning')}
                </AlertDescription>
              </Alert>
            )}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting} loading={isSubmitting}>
              {t('common.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ── Main tab component ────────────────────────────────────────────────────────

export function SsoSettingsTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const queryClient = useQueryClient();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<IdentityProviderDTO | undefined>();
  const [deleteTarget, setDeleteTarget] = useState<IdentityProviderDTO | null>(null);

  const { data: providers = [], isLoading } = useQuery({
    queryKey: ['sso-providers-admin'],
    queryFn: listAllProviders,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteProvider(id),
    onSuccess: () => {
      showToast(t('sso.deleteSuccess'), 'success');
      queryClient.invalidateQueries({ queryKey: ['sso-providers-admin'] });
      queryClient.invalidateQueries({ queryKey: ['sso-providers-public'] });
      setDeleteTarget(null);
    },
    onError: () => {
      showToast(t('sso.deleteFailed'), 'error');
    },
  });

  const handleSaved = () => {
    queryClient.invalidateQueries({ queryKey: ['sso-providers-admin'] });
    queryClient.invalidateQueries({ queryKey: ['sso-providers-public'] });
  };

  const openAdd = () => {
    setEditingProvider(undefined);
    setDialogOpen(true);
  };

  const openEdit = (provider: IdentityProviderDTO) => {
    setEditingProvider(provider);
    setDialogOpen(true);
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-base font-semibold flex items-center gap-2">
            <Shield className="h-4 w-4" />
            {t('sso.title')}
          </h3>
          <p className="text-sm text-muted-foreground mt-0.5">
            {t('sso.subtitle')}
          </p>
        </div>
        <Button size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4 mr-1" />
          {t('sso.addProvider')}
        </Button>
      </div>

      {/* Provider list */}
      {isLoading ? (
        <div className="space-y-2">
          {[0, 1].map((i) => (
            <div key={i} className="h-16 rounded-lg bg-muted animate-pulse" />
          ))}
        </div>
      ) : providers.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            <Shield className="h-8 w-8 mx-auto mb-2 opacity-30" />
            <p className="text-sm">{t('sso.noProviders')}</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {providers.map((provider) => (
            <Card key={provider.id}>
              <CardContent className="py-3 px-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm truncate">
                          {provider.name}
                        </span>
                        <Badge variant="outline" className="text-[10px] shrink-0">
                          {provider.providerType}
                        </Badge>
                        <Badge
                          variant={provider.isEnabled ? 'default' : 'secondary'}
                          className="text-[10px] shrink-0"
                        >
                          {provider.isEnabled
                            ? t('sso.statusEnabled')
                            : t('sso.statusDisabled')}
                        </Badge>
                      </div>
                      {provider.enforceSso && (
                        <p className="text-xs text-orange-600 mt-0.5">
                          {t('sso.enforceSsoActive')}
                        </p>
                      )}
                    </div>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-8 w-8 shrink-0">
                        <MoreVertical className="h-4 w-4" />
                        <span className="sr-only">{t('common.actions')}</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => openEdit(provider)}>
                        <Pencil className="h-4 w-4 mr-2" />
                        {t('common.edit')}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-destructive focus:text-destructive"
                        onClick={() => setDeleteTarget(provider)}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {t('common.delete')}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Add / Edit dialog */}
      <ProviderDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        existing={editingProvider}
        onSaved={handleSaved}
      />

      {/* Delete confirm */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title={t('sso.deleteConfirmTitle')}
        description={t('sso.deleteConfirm', { name: deleteTarget?.name ?? '' })}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        variant="destructive"
        loading={deleteMutation.isPending}
      />
    </div>
  );
}
