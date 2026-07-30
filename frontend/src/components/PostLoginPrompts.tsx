import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Fingerprint, Download, Loader2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Button } from './ui/button';
import { useToast } from '../contexts';
import { useBreakpointHelpers } from '../hooks/useBreakpoint';
import { passkeyService } from '../services/passkeyService';
import { isWebAuthnSupported, registerPasskey, WebAuthnCeremonyError } from '../lib/webauthn';
import { canPromptPwaInstall, isPwaInstalled, promptPwaInstall } from '../lib/pwa';

const PASSKEY_DISMISS_KEY = 'shipflow_passkey_prompt_dismissed';
const INSTALL_DISMISS_KEY = 'shipflow_pwa_install_prompt_dismissed';

type Step = 'checking' | 'passkey' | 'install' | 'done';

interface PostLoginPromptsProps {
  /** Called once there's nothing left to show (may fire immediately on mount). */
  onComplete: () => void;
}

/**
 * Mount once, right after a successful login, before navigating to the
 * post-login destination. Shows at most one dismissible nudge at a time,
 * in order:
 * 1. Register a passkey — WebAuthn-supported browsers with none registered
 *    yet (skipped entirely for users who just signed in *with* a passkey,
 *    since they already have one).
 * 2. Install the PWA — mobile viewports only, and only where the browser
 *    actually captured a `beforeinstallprompt` (Chromium-based; iOS Safari
 *    never fires it, so nothing shows there).
 * Each step remembers a skip in localStorage so it only ever nags once.
 */
export function PostLoginPrompts({ onComplete }: PostLoginPromptsProps) {
  const { t } = useTranslation();
  const { showError } = useToast();
  const { isMobile } = useBreakpointHelpers();

  const [step, setStep] = useState<Step>('checking');
  const [registering, setRegistering] = useState(false);
  const [installing, setInstalling] = useState(false);

  const decideInstallOrDone = () => {
    const installDismissed = localStorage.getItem(INSTALL_DISMISS_KEY) === '1';
    if (isMobile && !installDismissed && !isPwaInstalled() && canPromptPwaInstall()) {
      setStep('install');
    } else {
      setStep('done');
    }
  };

  useEffect(() => {
    let cancelled = false;

    async function decideFirstStep() {
      const passkeyDismissed = localStorage.getItem(PASSKEY_DISMISS_KEY) === '1';
      if (isWebAuthnSupported() && !passkeyDismissed) {
        try {
          const passkeys = await passkeyService.listPasskeys();
          if (cancelled) return;
          if (passkeys.length === 0) {
            setStep('passkey');
            return;
          }
        } catch {
          // Can't tell either way — don't block the login flow on it.
        }
      }
      if (!cancelled) decideInstallOrDone();
    }

    decideFirstStep();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (step === 'done') onComplete();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  const dismissPasskey = () => {
    localStorage.setItem(PASSKEY_DISMISS_KEY, '1');
    decideInstallOrDone();
  };

  const handleRegisterPasskey = async () => {
    setRegistering(true);
    try {
      const options = await passkeyService.getRegistrationOptions();
      const verifyRequest = await registerPasskey(options, t('passkeys.deviceNamePlaceholder'));
      await passkeyService.verifyRegistration(verifyRequest);
      localStorage.setItem(PASSKEY_DISMISS_KEY, '1');
      decideInstallOrDone();
    } catch (err) {
      if (!(err instanceof WebAuthnCeremonyError) || !err.cancelled) {
        showError(t('passkeys.registerFailed'));
      }
      // Leave the dialog open either way so the user can retry or explicitly skip.
    } finally {
      setRegistering(false);
    }
  };

  const dismissInstall = () => {
    localStorage.setItem(INSTALL_DISMISS_KEY, '1');
    setStep('done');
  };

  const handleInstall = async () => {
    setInstalling(true);
    try {
      const outcome = await promptPwaInstall();
      if (outcome !== 'accepted') {
        localStorage.setItem(INSTALL_DISMISS_KEY, '1');
      }
    } finally {
      setInstalling(false);
      setStep('done');
    }
  };

  if (step === 'passkey') {
    return (
      <Dialog open onOpenChange={(open) => !open && dismissPasskey()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Fingerprint className="h-5 w-5" />
              {t('passkeys.setupPromptTitle')}
            </DialogTitle>
            <DialogDescription>{t('passkeys.setupPromptDesc')}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={dismissPasskey} disabled={registering}>
              {t('passkeys.setupPromptSkip')}
            </Button>
            <Button onClick={handleRegisterPasskey} disabled={registering}>
              {registering && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {t('passkeys.setupPromptCta')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  if (step === 'install') {
    return (
      <Dialog open onOpenChange={(open) => !open && dismissInstall()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Download className="h-5 w-5" />
              {t('pwa.installPromptTitle')}
            </DialogTitle>
            <DialogDescription>{t('pwa.installPromptDesc')}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={dismissInstall} disabled={installing}>
              {t('pwa.installPromptSkip')}
            </Button>
            <Button onClick={handleInstall} disabled={installing}>
              {installing && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {t('pwa.installPromptCta')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return null;
}
