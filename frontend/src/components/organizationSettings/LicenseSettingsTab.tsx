import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { AxiosError } from 'axios';
import { KeyRound, Loader2, Info, AlertTriangle } from 'lucide-react';
import { useAuth, useToast } from '../../contexts';
import { useLicenseStatus, useUploadLicense, useRemoveLicense } from '../../hooks/useLicense';
import type { LicenseStatus } from '../../services/licenseService';
import { formatDate } from '../../utils/dateUtils';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Alert, AlertDescription } from '../ui/alert';
import { ConfirmDialog } from '../ui/confirm-dialog';

// ── Status → Badge variant / label mapping ──────────────────────────────────────

const STATUS_BADGE_VARIANT: Record<LicenseStatus, 'success' | 'warning' | 'destructive' | 'secondary'> = {
  VALID: 'success',
  GRACE: 'warning',
  EXPIRED: 'destructive',
  MISSING: 'secondary',
};

const STATUS_LABEL_KEY: Record<LicenseStatus, string> = {
  VALID: 'license.statusValid',
  GRACE: 'license.statusGrace',
  EXPIRED: 'license.statusExpired',
  MISSING: 'license.statusCommunity',
};

// ── Main tab component ─────────────────────────────────────────────────────────

export function LicenseSettingsTab() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { showToast } = useToast();

  const { data: license, isLoading } = useLicenseStatus();
  const uploadMutation = useUploadLicense();
  const removeMutation = useRemoveLicense();

  const [content, setContent] = useState('');
  const [invalidError, setInvalidError] = useState(false);
  const [removeConfirmOpen, setRemoveConfirmOpen] = useState(false);

  const isAdmin = user?.role === 'ADMIN';

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    // Always clear the input so re-selecting the same file still fires onChange.
    e.target.value = '';
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setContent(reader.result);
        setInvalidError(false);
      }
    };
    reader.readAsText(file);
  };

  const handleUpload = () => {
    uploadMutation.mutate(content, {
      onSuccess: () => {
        showToast(t('license.uploadSuccess'), 'success');
        setContent('');
        setInvalidError(false);
      },
      onError: (error) => {
        const messageKey = (error as AxiosError<{ messageKey?: string }>).response?.data?.messageKey;
        setInvalidError(messageKey === 'license.invalid');
      },
    });
  };

  const handleRemove = () => {
    removeMutation.mutate(undefined, {
      onSuccess: () => {
        showToast(t('license.removeSuccess'), 'success');
        setRemoveConfirmOpen(false);
      },
      onError: () => {
        setRemoveConfirmOpen(false);
      },
    });
  };

  // ── Render ────────────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!license) {
    return null;
  }

  const showCommunityText = license.status === 'MISSING' || license.status === 'EXPIRED';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h3 className="text-base font-semibold flex items-center gap-2">
          <KeyRound className="h-4 w-4" />
          {t('license.title')}
        </h3>
        <p className="text-sm text-muted-foreground mt-0.5">{t('license.subtitle')}</p>
      </div>

      {/* Status section — visible to every authenticated role */}
      <div className="space-y-3">
        <Badge variant={STATUS_BADGE_VARIANT[license.status]}>
          {t(STATUS_LABEL_KEY[license.status])}
        </Badge>

        {license.status !== 'MISSING' && (
          <div className="space-y-1.5 text-sm">
            {license.licensee && (
              <p>
                <span className="text-muted-foreground">{t('license.licensee')}: </span>
                {license.licensee}
              </p>
            )}
            {license.seats != null && (
              <p className="text-muted-foreground">
                {t('license.seatsOf', { used: license.seatsUsed, total: license.seats })}
              </p>
            )}
            {license.issuedAt && (
              <p>
                <span className="text-muted-foreground">{t('license.issuedAt')}: </span>
                {formatDate(license.issuedAt)}
              </p>
            )}
            {license.expiresAt && (
              <p>
                <span className="text-muted-foreground">{t('license.expiresAt')}: </span>
                {formatDate(license.expiresAt)}
              </p>
            )}
            {license.supportUntil && (
              <p>
                <span className="text-muted-foreground">{t('license.supportUntil')}: </span>
                {formatDate(license.supportUntil)}
              </p>
            )}
            <div className="flex items-center gap-1.5 flex-wrap pt-1">
              <span className="text-muted-foreground">{t('license.features')}:</span>
              {license.features.length > 0 ? (
                license.features.map((feature) => (
                  <Badge key={feature} variant="outline">
                    {feature}
                  </Badge>
                ))
              ) : (
                <span className="text-muted-foreground">{t('license.noFeatures')}</span>
              )}
            </div>
          </div>
        )}

        {showCommunityText && (
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              {t('license.communityText', {
                userCap: license.communityCap,
                automationCap: license.automationCap,
              })}
            </AlertDescription>
          </Alert>
        )}

        {license.status === 'GRACE' && (
          <Alert variant="warning">
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              {t('license.graceWarning', {
                graceEndsAt: license.graceEndsAt ? formatDate(license.graceEndsAt) : '',
              })}
            </AlertDescription>
          </Alert>
        )}
      </div>

      {/* Admin-only controls */}
      {isAdmin && (
        <div className="space-y-4 border-t border-border pt-6">
          <h4 className="text-sm font-semibold">{t('license.uploadLabel')}</h4>

          <div className="space-y-2">
            <Label htmlFor="license-upload-file">{t('license.uploadFile')}</Label>
            <Input
              id="license-upload-file"
              type="file"
              accept=".license,.json,application/json,text/plain"
              onChange={handleFileChange}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="license-content">{t('license.pasteLabel')}</Label>
            <Textarea
              id="license-content"
              value={content}
              onChange={(e) => {
                setContent(e.target.value);
                if (invalidError) setInvalidError(false);
              }}
              rows={8}
              className="font-mono text-xs"
            />
            {invalidError && <p className="text-sm text-destructive">{t('license.invalidError')}</p>}
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              onClick={handleUpload}
              disabled={uploadMutation.isPending || !content.trim()}
            >
              {uploadMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {uploadMutation.isPending ? t('license.uploading') : t('license.uploadButton')}
            </Button>

            {license.status !== 'MISSING' && (
              <Button type="button" variant="outline" onClick={() => setRemoveConfirmOpen(true)}>
                {t('license.removeButton')}
              </Button>
            )}
          </div>
        </div>
      )}

      {/* Remove confirm dialog */}
      <ConfirmDialog
        open={removeConfirmOpen}
        onOpenChange={(open) => !open && setRemoveConfirmOpen(false)}
        title={t('license.removeConfirmTitle')}
        description={t('license.removeConfirm')}
        confirmLabel={t('common.confirm')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleRemove}
        variant="destructive"
        loading={removeMutation.isPending}
      />
    </div>
  );
}
