import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Shield, Copy, Check, Loader2, AlertTriangle } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { Switch } from '../ui/switch';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { OrganizationSettings } from '../../types/organizationSettings';
import { organizationSettingsService } from '../../services/organizationSettingsService';
import { useToast } from '../../contexts';

interface ScimSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function ScimSettingsTab({ formData, setFormData }: ScimSettingsTabProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [generatingToken, setGeneratingToken] = useState(false);
  const [tokenDialogOpen, setTokenDialogOpen] = useState(false);
  const [rawToken, setRawToken] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleGenerateToken = async () => {
    setGeneratingToken(true);
    try {
      const response = await organizationSettingsService.generateScimToken();
      setRawToken(response.data.token);
      setTokenDialogOpen(true);
    } catch {
      showToast(t('scim.generateTokenFailed'), 'error');
    } finally {
      setGeneratingToken(false);
    }
  };

  const handleCopyToken = async () => {
    if (!rawToken) return;
    try {
      await navigator.clipboard.writeText(rawToken);
      setCopied(true);
      showToast(t('scim.tokenCopied'), 'success');
      setTimeout(() => setCopied(false), 2000);
    } catch {
      showToast(t('scim.copyFailed'), 'error');
    }
  };

  const handleDialogClose = (open: boolean) => {
    if (!open) {
      setTokenDialogOpen(false);
      setRawToken(null);
      setCopied(false);
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5" />
            {t('scim.title')}
          </CardTitle>
          <CardDescription>{t('scim.enabledDesc')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Enable toggle */}
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label htmlFor="scim-enabled">{t('scim.enabled')}</Label>
              <p className="text-sm text-muted-foreground">{t('scim.enabledDesc')}</p>
            </div>
            <Switch
              id="scim-enabled"
              checked={formData.scimEnabled ?? false}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, scimEnabled: checked })
              }
            />
          </div>

          <Separator />

          {/* Token generation */}
          <div className="space-y-3">
            <div>
              <Label>{t('scim.generateToken')}</Label>
              <p className="text-sm text-muted-foreground mt-0.5">
                {t('scim.noTokenGenerated')}
              </p>
            </div>
            <Button
              variant="outline"
              onClick={handleGenerateToken}
              disabled={generatingToken}
            >
              {generatingToken ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {t('scim.generating')}
                </>
              ) : (
                <>
                  <Shield className="h-4 w-4 mr-2" />
                  {t('scim.generateToken')}
                </>
              )}
            </Button>
          </div>

          {/* SCIM endpoint info */}
          <Separator />
          <div className="space-y-2">
            <Label>{t('scim.endpointLabel')}</Label>
            <p className="text-sm text-muted-foreground font-mono bg-muted px-3 py-2 rounded-md">
              {window.location.origin}/scim/v2
            </p>
          </div>
        </CardContent>
      </Card>

      {/* One-time token dialog */}
      <Dialog open={tokenDialogOpen} onOpenChange={handleDialogClose}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              {t('scim.tokenDialogTitle')}
            </DialogTitle>
            <DialogDescription>{t('scim.tokenWarning')}</DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="flex items-center gap-2 p-3 bg-amber-50 border border-amber-200 rounded-md dark:bg-amber-950 dark:border-amber-800">
              <AlertTriangle className="h-4 w-4 text-amber-600 flex-shrink-0" />
              <p className="text-sm text-amber-700 dark:text-amber-300">
                {t('scim.tokenWarning')}
              </p>
            </div>

            <div className="space-y-2">
              <Label>{t('scim.copyToken')}</Label>
              <div className="flex items-center gap-2">
                <div className="flex-1 font-mono text-sm bg-muted px-3 py-2 rounded-md break-all overflow-hidden">
                  {rawToken}
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleCopyToken}
                  className="flex-shrink-0"
                >
                  {copied ? (
                    <Check className="h-4 w-4" />
                  ) : (
                    <Copy className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>

            <Button className="w-full" onClick={() => handleDialogClose(false)}>
              {t('scim.tokenSaved')}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
