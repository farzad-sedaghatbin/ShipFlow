import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { AlertCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { Alert, AlertDescription } from '../components/ui/alert';

/**
 * SsoCallbackPage — handles the browser redirect back from an SSO provider.
 *
 * Expected query params:
 *   ?token=<JWT>          — issued by the backend after successful SSO
 *   ?redirect=<pathname>  — optional deep-link to restore after login
 *
 * If the backend decides to pass user claims separately it would extend this.
 * For now the minimal contract is: token in the query string → store it →
 * navigate to the intended destination.
 */
export default function SsoCallbackPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const redirect = searchParams.get('redirect') || '/dashboard';

    if (!token) {
      setError(t('ssoCallback.noToken'));
      return;
    }

    try {
      // Decode the JWT payload (base64url) to extract user claims.
      // We do not verify the signature here — the API will reject invalid
      // tokens on the first authenticated request.
      const payloadB64 = token.split('.')[1];
      if (!payloadB64) throw new Error('malformed token');

      // base64url → standard base64 → JSON
      const json = atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(json) as {
        sub?: string;
        userId?: number;
        role?: string;
        personId?: number;
        personName?: string;
      };

      const user = {
        userId: payload.userId ?? 0,
        username: payload.sub ?? '',
        role: payload.role ?? 'VIEWER',
        personId: payload.personId,
        personName: payload.personName,
      };

      login(token, user);
      navigate(redirect, { replace: true });
    } catch {
      setError(t('ssoCallback.processingError'));
    }
  // login and navigate are stable refs — safe to omit from deps array
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background p-4">
        <div className="w-full max-w-md space-y-4">
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
          <p className="text-center text-sm text-muted-foreground">
            <a href="/login" className="underline hover:text-foreground">
              {t('ssoCallback.backToLogin')}
            </a>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-4">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        <p className="text-sm text-muted-foreground">{t('ssoCallback.processing')}</p>
      </div>
    </div>
  );
}
