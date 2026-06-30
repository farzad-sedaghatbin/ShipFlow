import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { HardDrive, Loader2, Info } from 'lucide-react';
import {
  storageService,
  StorageConfigRequest,
  StorageProvider,
} from '../../services/storageService';
import { useToast } from '../../contexts';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Card, CardContent } from '../ui/card';
import { Switch } from '../ui/switch';
import { Alert, AlertDescription } from '../ui/alert';
import { ConfirmDialog } from '../ui/confirm-dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';

// ── Zod schema ─────────────────────────────────────────────────────────────────

const storageSchema = z.object({
  activeProvider: z.enum(['LOCAL_FS', 'S3', 'MINIO']),
  bucket: z.string().optional(),
  endpoint: z.string().optional(),
  region: z.string().optional(),
  pathStyleAccess: z.boolean().optional(),
  accessKey: z.string().optional(),
  secretKey: z.string().optional(),
});

type StorageFormData = z.infer<typeof storageSchema>;

// ── Helpers ────────────────────────────────────────────────────────────────────

/**
 * Build the PUT/POST request body from form values.
 * Blank accessKey/secretKey are omitted so the backend preserves existing credentials.
 */
function buildRequest(data: StorageFormData): StorageConfigRequest {
  const config: StorageConfigRequest['config'] = {
    ...(data.bucket && { bucket: data.bucket }),
    ...(data.endpoint && { endpoint: data.endpoint }),
    ...(data.region && { region: data.region }),
    ...(data.pathStyleAccess !== undefined && { pathStyleAccess: data.pathStyleAccess }),
    ...(data.accessKey && { accessKey: data.accessKey }),
    ...(data.secretKey && { secretKey: data.secretKey }),
  };
  return { activeProvider: data.activeProvider as StorageProvider, config };
}

// ── Main tab component ─────────────────────────────────────────────────────────

export function StorageSettingsTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const queryClient = useQueryClient();

  const [testing, setTesting] = useState(false);
  const [migrateConfirmOpen, setMigrateConfirmOpen] = useState(false);

  // ── Load config ──────────────────────────────────────────────────────────────

  const { data: storageConfig, isLoading } = useQuery({
    queryKey: ['admin-storage-config'],
    queryFn: () => storageService.getStorageConfig().then((r) => r.data),
  });

  // ── Form ─────────────────────────────────────────────────────────────────────

  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { isSubmitting },
  } = useForm<StorageFormData>({
    resolver: zodResolver(storageSchema),
    defaultValues: {
      activeProvider: 'LOCAL_FS',
      bucket: '',
      endpoint: '',
      region: '',
      pathStyleAccess: false,
      accessKey: '',
      secretKey: '',
    },
  });

  // Populate form once data loads — secret fields intentionally stay empty.
  // useEffect ensures reset() runs after render, not during, avoiding React warnings.
  useEffect(() => {
    if (storageConfig) {
      reset({
        activeProvider: storageConfig.activeProvider,
        bucket: storageConfig.bucket ?? '',
        endpoint: storageConfig.endpoint ?? '',
        region: storageConfig.region ?? '',
        pathStyleAccess: storageConfig.pathStyleAccess ?? false,
        accessKey: '',  // never pre-fill — API never returns secrets
        secretKey: '',
      });
    }
  }, [storageConfig, reset]);

  const activeProvider = watch('activeProvider');

  // ── Save mutation ─────────────────────────────────────────────────────────────

  const saveMutation = useMutation({
    mutationFn: (req: StorageConfigRequest) => storageService.updateStorageConfig(req),
    onSuccess: () => {
      showToast(t('storage.saveSuccess'), 'success');
      queryClient.invalidateQueries({ queryKey: ['admin-storage-config'] });
      // The useEffect watching storageConfig will re-run reset() when fresh data arrives.
    },
    onError: () => {
      showToast(t('storage.saveFailed'), 'error');
    },
  });

  const onSubmit = (data: StorageFormData) => {
    saveMutation.mutate(buildRequest(data));
  };

  // ── Test connection ───────────────────────────────────────────────────────────

  const handleTest = async () => {
    const values = watch() as StorageFormData;
    setTesting(true);
    try {
      const res = await storageService.testStorageConnection(buildRequest(values));
      showToast(
        res.data.ok ? t('storage.testSuccess') : res.data.message,
        res.data.ok ? 'success' : 'error'
      );
    } catch {
      showToast(t('storage.testFailed'), 'error');
    } finally {
      setTesting(false);
    }
  };

  // ── Migrate ───────────────────────────────────────────────────────────────────

  const migrateMutation = useMutation({
    mutationFn: () => storageService.migrateStorage(),
    onSuccess: ({ data }) => {
      showToast(
        t('storage.migrateResult', {
          migrated: data.migrated,
          skipped: data.skipped,
          failed: data.failed,
          total: data.total,
        }),
        'success'
      );
      setMigrateConfirmOpen(false);
    },
    onError: () => {
      showToast(t('storage.migrateFailed'), 'error');
      setMigrateConfirmOpen(false);
    },
  });

  // ── Render ────────────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h3 className="text-base font-semibold flex items-center gap-2">
          <HardDrive className="h-4 w-4" />
          {t('storage.title')}
        </h3>
        <p className="text-sm text-muted-foreground mt-0.5">{t('storage.subtitle')}</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Provider selector */}
        <div className="space-y-2">
          <Label htmlFor="storage-provider">{t('storage.provider')}</Label>
          <Controller
            name="activeProvider"
            control={control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger id="storage-provider" className="w-full sm:w-72">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LOCAL_FS">{t('storage.providerLocal')}</SelectItem>
                  <SelectItem value="S3">{t('storage.providerS3')}</SelectItem>
                  <SelectItem value="MINIO">{t('storage.providerMinio')}</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
        </div>

        {/* LOCAL_FS info callout */}
        {activeProvider === 'LOCAL_FS' && (
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>{t('storage.localInfo')}</AlertDescription>
          </Alert>
        )}

        {/* S3 fields */}
        {activeProvider === 'S3' && (
          <Card>
            <CardContent className="space-y-4 pt-4">
              <div className="space-y-2">
                <Label htmlFor="storage-bucket">{t('storage.bucket')}</Label>
                <Input
                  id="storage-bucket"
                  {...register('bucket')}
                  placeholder="my-shipflow-uploads"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-region">{t('storage.region')}</Label>
                <Input
                  id="storage-region"
                  {...register('region')}
                  placeholder="us-east-1"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-access-key">{t('storage.accessKey')}</Label>
                <Input
                  id="storage-access-key"
                  type="password"
                  {...register('accessKey')}
                  autoComplete="new-password"
                />
                {storageConfig?.hasAccessKey && (
                  <p className="text-xs text-muted-foreground">{t('storage.keyConfigured')}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-secret-key">{t('storage.secretKey')}</Label>
                <Input
                  id="storage-secret-key"
                  type="password"
                  {...register('secretKey')}
                  autoComplete="new-password"
                />
                {storageConfig?.hasSecretKey && (
                  <p className="text-xs text-muted-foreground">{t('storage.keyConfigured')}</p>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* MinIO fields */}
        {activeProvider === 'MINIO' && (
          <Card>
            <CardContent className="space-y-4 pt-4">
              <div className="space-y-2">
                <Label htmlFor="storage-bucket-minio">{t('storage.bucket')}</Label>
                <Input
                  id="storage-bucket-minio"
                  {...register('bucket')}
                  placeholder="shipflow"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-endpoint">{t('storage.endpoint')}</Label>
                <Input
                  id="storage-endpoint"
                  {...register('endpoint')}
                  placeholder="http://minio.internal:9000"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-access-key-minio">{t('storage.accessKey')}</Label>
                <Input
                  id="storage-access-key-minio"
                  type="password"
                  {...register('accessKey')}
                  autoComplete="new-password"
                />
                {storageConfig?.hasAccessKey && (
                  <p className="text-xs text-muted-foreground">{t('storage.keyConfigured')}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="storage-secret-key-minio">{t('storage.secretKey')}</Label>
                <Input
                  id="storage-secret-key-minio"
                  type="password"
                  {...register('secretKey')}
                  autoComplete="new-password"
                />
                {storageConfig?.hasSecretKey && (
                  <p className="text-xs text-muted-foreground">{t('storage.keyConfigured')}</p>
                )}
              </div>

              {/* Path-style access toggle (MinIO-specific) */}
              <div className="flex items-center justify-between rounded-md border p-3">
                <Label htmlFor="storage-path-style" className="cursor-pointer">
                  {t('storage.pathStyleAccess')}
                </Label>
                <Controller
                  name="pathStyleAccess"
                  control={control}
                  render={({ field }) => (
                    <Switch
                      id="storage-path-style"
                      checked={field.value ?? false}
                      onCheckedChange={field.onChange}
                    />
                  )}
                />
              </div>
            </CardContent>
          </Card>
        )}

        {/* Action row */}
        <div className="flex flex-wrap gap-2">
          <Button type="submit" disabled={isSubmitting || saveMutation.isPending}>
            {(isSubmitting || saveMutation.isPending) && (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            )}
            {t('common.save')}
          </Button>

          {activeProvider !== 'LOCAL_FS' && (
            <Button
              type="button"
              variant="outline"
              onClick={handleTest}
              disabled={testing}
            >
              {testing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {testing ? t('storage.testing') : t('storage.testConnection')}
            </Button>
          )}

          <Button
            type="button"
            variant="outline"
            onClick={() => setMigrateConfirmOpen(true)}
          >
            {t('storage.migrateFiles')}
          </Button>
        </div>
      </form>

      {/* Migrate confirm dialog */}
      <ConfirmDialog
        open={migrateConfirmOpen}
        onOpenChange={(open) => !open && setMigrateConfirmOpen(false)}
        title={t('storage.migrateConfirmTitle')}
        description={t('storage.migrateConfirm')}
        confirmLabel={t('common.confirm')}
        cancelLabel={t('common.cancel')}
        onConfirm={() => migrateMutation.mutate()}
        loading={migrateMutation.isPending}
      />
    </div>
  );
}
