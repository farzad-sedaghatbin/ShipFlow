import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Fingerprint, Trash2, Loader2, Plus } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { useToast } from '../contexts';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { getUserFriendlyError } from '../utils/errorMessages';
import { passkeyService, type PasskeyDTO } from '../services/passkeyService';
import { isWebAuthnSupported, registerPasskey, WebAuthnCeremonyError } from '../lib/webauthn';

/**
 * Profile page card listing the current user's registered passkeys, with an
 * "Add a Passkey" ceremony and per-row delete. Entirely hidden when the
 * browser doesn't support WebAuthn (`isWebAuthnSupported()`) rather than
 * showing a button that would throw on click.
 */
export function PasskeysCard() {
  const { t, i18n } = useTranslation();
  const { showSuccess, showError } = useToast();
  const queryClient = useQueryClient();

  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [deviceName, setDeviceName] = useState('');
  const [passkeyToDelete, setPasskeyToDelete] = useState<PasskeyDTO | null>(null);

  const supported = isWebAuthnSupported();

  const { data: passkeys = [], isLoading } = useQuery({
    queryKey: ['passkeys'],
    queryFn: passkeyService.listPasskeys,
    enabled: supported,
  });

  const registerMutation = useMutation({
    mutationFn: async (name: string) => {
      const options = await passkeyService.getRegistrationOptions();
      const verifyRequest = await registerPasskey(options, name);
      return passkeyService.verifyRegistration(verifyRequest);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['passkeys'] });
      setAddDialogOpen(false);
      setDeviceName('');
      showSuccess(t('passkeys.registerSuccess'));
    },
    onError: (err: unknown) => {
      if (err instanceof WebAuthnCeremonyError) {
        showError(t(err.cancelled ? 'passkeys.registerCancelled' : 'passkeys.registerFailed'));
      } else {
        showError(getUserFriendlyError(err, t('passkeys.registerFailed')));
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => passkeyService.deletePasskey(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['passkeys'] });
      setPasskeyToDelete(null);
      showSuccess(t('passkeys.deleteSuccess'));
    },
    onError: (err: unknown) => {
      showError(getUserFriendlyError(err, t('passkeys.deleteFailed')));
      setPasskeyToDelete(null);
    },
  });

  if (!supported) {
    return null;
  }

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between gap-4">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Fingerprint className="h-5 w-5" />
                {t('passkeys.title')}
              </CardTitle>
              <p className="text-sm text-muted-foreground mt-1">{t('passkeys.description')}</p>
            </div>
            <Button
              size="sm"
              onClick={() => {
                setDeviceName('');
                setAddDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4 mr-2" />
              {t('passkeys.addPasskey')}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-muted-foreground text-center py-4">{t('common.loading')}</p>
          ) : passkeys.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">{t('passkeys.noPasskeys')}</p>
          ) : (
            <ul className="divide-y divide-border">
              {passkeys.map((passkey) => (
                <li key={passkey.id} className="flex items-center justify-between gap-4 py-3">
                  <div>
                    <p className="text-sm font-medium text-foreground">{passkey.deviceName}</p>
                    <p className="text-xs text-muted-foreground">
                      {t('passkeys.addedOn', { date: formatLocalizedDate(passkey.createdAt, i18n.language) })}
                      {' · '}
                      {passkey.lastUsedAt
                        ? t('passkeys.lastUsedOn', { date: formatLocalizedDate(passkey.lastUsedAt, i18n.language) })
                        : t('passkeys.neverUsed')}
                    </p>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={t('passkeys.deleteTitle')}
                    onClick={() => setPasskeyToDelete(passkey)}
                    disabled={deleteMutation.isPending}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      {/* Add Passkey — device-name prompt */}
      <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('passkeys.deviceNamePromptTitle')}</DialogTitle>
            <DialogDescription>{t('passkeys.deviceNamePromptDesc')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <Label htmlFor="passkey-device-name">{t('passkeys.deviceNamePlaceholder')}</Label>
            <Input
              id="passkey-device-name"
              value={deviceName}
              onChange={(e) => setDeviceName(e.target.value)}
              placeholder={t('passkeys.deviceNamePlaceholder')}
              autoFocus
            />
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setAddDialogOpen(false)}
              disabled={registerMutation.isPending}
            >
              {t('common.cancel')}
            </Button>
            <Button
              onClick={() => registerMutation.mutate(deviceName.trim() || 'My Device')}
              disabled={registerMutation.isPending}
            >
              {registerMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {t('common.add')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation */}
      <Dialog open={!!passkeyToDelete} onOpenChange={(open) => !open && setPasskeyToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('passkeys.deleteTitle')}</DialogTitle>
            <DialogDescription>
              {t('passkeys.deleteConfirm', { name: passkeyToDelete?.deviceName })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setPasskeyToDelete(null)}
              disabled={deleteMutation.isPending}
            >
              {t('common.cancel')}
            </Button>
            <Button
              variant="destructive"
              onClick={() => passkeyToDelete && deleteMutation.mutate(passkeyToDelete.id)}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {t('common.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
