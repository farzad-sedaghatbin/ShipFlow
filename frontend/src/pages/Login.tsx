import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff, LogIn, Info, KeyRound, Fingerprint, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { useAuth, useToast } from '../contexts';
import { authService } from '../services/authService';
import { getEnabledProviders, initiateSSO } from '../services/ssoService';
import { passkeyService } from '../services/passkeyService';
import { isWebAuthnSupported, loginWithPasskey, WebAuthnCeremonyError } from '../lib/webauthn';
import { LoginIllustration } from '../components/illustrations';
import { PostLoginPrompts } from '../components/PostLoginPrompts';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';
import { loginSchema, type LoginFormData } from '@/lib/validations';
import { cn } from '@/lib/utils';
import { useSeo } from '@/hooks/useSeo';
import { clearPendingRedirect, rememberRedirect, resolvePostLoginTarget } from '@/lib/redirect';

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  // Crawlable but not indexable: a sign-in form has nothing to rank for, and
  // indexing it would surface ShipFlow for the "shipflow login" queries that
  // belong to the unrelated shipping/freight products of the same name.
  useSeo({
    title: 'Sign in',
    description: 'Sign in to your ShipFlow workspace.',
    path: '/login',
    noindex: true,
  });
  const { login } = useAuth();
  const { showSuccess } = useToast();

  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const [ssoLoading, setSsoLoading] = useState<number | null>(null);
  // Gates navigation until any post-login nudges (passkey setup, PWA install)
  // have been shown and resolved — see PostLoginPrompts.
  const [showPostLoginPrompts, setShowPostLoginPrompts] = useState(false);

  // Passkey sign-in — a username-only alternative to the password form,
  // gated on browser WebAuthn support (Safari/older browsers lack it).
  const webAuthnSupported = isWebAuthnSupported();
  const [passkeyMode, setPasskeyMode] = useState(false);
  const [passkeyUsername, setPasskeyUsername] = useState('');
  const [passkeyLoading, setPasskeyLoading] = useState(false);

  // Where to land after signing in: the route that bounced us here (router
  // state or `?redirect=`), a destination stashed before an SSO round trip, or
  // the dashboard. Resolved once on mount so a re-render can't change it, and
  // sanitised against off-site targets — see lib/redirect.ts.
  const [from] = useState(() => resolvePostLoginTarget(location));

  const goToDestination = () => {
    clearPendingRedirect();
    navigate(from, { replace: true });
  };

  // Fetch SSO providers — silently ignore errors (no SSO configured is fine)
  const { data: ssoProviders = [] } = useQuery({
    queryKey: ['sso-providers-public'],
    queryFn: getEnabledProviders,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  const handleSsoLogin = async (idpId: number) => {
    setSsoLoading(idpId);
    try {
      const result = await initiateSSO(idpId);
      // The identity provider is off-origin, so neither router state nor the
      // login URL survives the round trip — stash the destination for
      // SsoCallbackPage to pick up when the browser comes back.
      rememberRedirect(from);
      window.location.href = result.redirectUrl;
    } catch {
      setServerError(t('login.ssoError'));
      setSsoLoading(null);
    }
  };

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  });

  // Load remembered username on mount
  useEffect(() => {
    const rememberedUsername = localStorage.getItem('rememberedUsername');
    if (rememberedUsername) {
      setValue('username', rememberedUsername);
      setRememberMe(true);
    }
  }, [setValue]);

  const onSubmit = async (data: LoginFormData) => {
    setServerError('');
    setLoading(true);

    try {
      const response = await authService.login({ username: data.username, password: data.password });
      const { token, userId, username: user, role, personId, personName } = response.data;

      // Handle remember me
      if (rememberMe) {
        localStorage.setItem('rememberedUsername', data.username);
      } else {
        localStorage.removeItem('rememberedUsername');
      }

      login(token, { userId, username: user, role, personId, personName });
      showSuccess(t('login.loginSuccess'));
      setShowPostLoginPrompts(true);
    } catch (err: unknown) {
      const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || t('login.invalidCredentials');
      setServerError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handlePasskeyLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passkeyUsername.trim()) return;

    setServerError('');
    setPasskeyLoading(true);

    try {
      const options = await passkeyService.getLoginOptions(passkeyUsername.trim());
      const credential = await loginWithPasskey(options);
      const response = await passkeyService.verifyLogin({ username: passkeyUsername.trim(), ...credential });
      const { token, userId, username: user, role, personId, personName } = response;

      login(token, { userId, username: user, role, personId, personName });
      showSuccess(t('login.loginSuccess'));
      setShowPostLoginPrompts(true);
    } catch (err: unknown) {
      if (err instanceof WebAuthnCeremonyError) {
        // Cancellation/timeout vs. an actual ceremony failure get distinct,
        // friendly messages rather than a raw DOMException string.
        setServerError(t(err.cancelled ? 'passkeys.loginCancelled' : 'passkeys.loginFailed'));
      } else {
        const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
          || t('passkeys.loginFailed');
        setServerError(errorMessage);
      }
    } finally {
      setPasskeyLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/10 pointer-events-none" />
      
      <Card className="w-full max-w-4xl overflow-hidden shadow-2xl relative">
        <div className="flex flex-col md:flex-row">
          {/* Illustration Side */}
          <div className="flex-1 flex flex-col items-center justify-center p-8 bg-gradient-to-br from-primary/5 to-primary/10 min-h-[200px] md:min-h-0">
            <LoginIllustration width={240} height={180} />
            <h2 className="mt-6 text-xl font-bold text-primary text-center">
              {t('login.shapeUpProjects')}
            </h2>
            <p className="mt-2 text-sm text-muted-foreground text-center max-w-[280px]">
              {t('login.tagline')}
            </p>
          </div>

          {/* Form Side */}
          <CardContent className="flex-1 p-8 flex flex-col justify-center">
            <div className="text-center mb-8">
              <div className="inline-flex items-center gap-2 mb-2">
                <img src="/icon.png" alt="ShipFlow" className="w-10 h-10 rounded-lg" />
                <h1 className="text-2xl font-bold text-primary">ShipFlow</h1>
              </div>
              <p className="text-sm text-muted-foreground">
                {t('login.signInToContinue')}
              </p>
            </div>

            {serverError && (
              <Alert variant="destructive" className="mb-6">
                <AlertDescription>{serverError}</AlertDescription>
              </Alert>
            )}

            {!passkeyMode ? (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="username">{t('login.username')}</Label>
                  <Input
                    id="username"
                    {...register('username')}
                    autoFocus
                    autoComplete="username"
                    placeholder={t('login.username')}
                    aria-invalid={!!errors.username}
                    className={cn(errors.username && 'border-destructive focus-visible:ring-destructive')}
                  />
                  {errors.username && (
                    <p className="text-sm font-medium text-destructive">{errors.username.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="password">{t('login.password')}</Label>
                  <div className="relative">
                    <Input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      {...register('password')}
                      autoComplete="current-password"
                      placeholder={t('login.password')}
                      aria-invalid={!!errors.password}
                      className={cn('pr-10', errors.password && 'border-destructive focus-visible:ring-destructive')}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
                      onClick={() => setShowPassword(!showPassword)}
                      aria-label={showPassword ? t('login.hidePassword') : t('login.showPassword')}
                    >
                      {showPassword ? (
                        <EyeOff className="h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Eye className="h-4 w-4 text-muted-foreground" />
                      )}
                    </Button>
                  </div>
                  {errors.password && (
                    <p className="text-sm font-medium text-destructive">{errors.password.message}</p>
                  )}
                </div>

                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="remember"
                    checked={rememberMe}
                    onCheckedChange={(checked) => setRememberMe(checked as boolean)}
                  />
                  <Label htmlFor="remember" className="text-sm text-muted-foreground cursor-pointer">
                    {t('login.rememberMe')}
                  </Label>
                </div>

                <Button
                  type="submit"
                  className="w-full"
                  size="lg"
                  disabled={loading}
                  loading={loading}
                >
                  {!loading && (
                    <>
                      <LogIn className="h-4 w-4 mr-2" />
                      {t('login.signIn')}
                    </>
                  )}
                  {loading && t('common.loading')}
                </Button>

                {webAuthnSupported && (
                  <button
                    type="button"
                    className="w-full text-center text-sm text-primary hover:underline flex items-center justify-center gap-1.5"
                    onClick={() => {
                      setServerError('');
                      setPasskeyUsername('');
                      setPasskeyMode(true);
                    }}
                  >
                    <Fingerprint className="h-4 w-4" />
                    {t('passkeys.signInWithPasskey')}
                  </button>
                )}
              </form>
            ) : (
              <form onSubmit={handlePasskeyLogin} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="passkey-username">{t('passkeys.usernameLabel')}</Label>
                  <Input
                    id="passkey-username"
                    value={passkeyUsername}
                    onChange={(e) => setPasskeyUsername(e.target.value)}
                    autoFocus
                    autoComplete="username webauthn"
                    placeholder={t('login.username')}
                  />
                </div>

                <Button
                  type="submit"
                  className="w-full"
                  size="lg"
                  disabled={passkeyLoading || !passkeyUsername.trim()}
                  loading={passkeyLoading}
                >
                  {!passkeyLoading && (
                    <>
                      <Fingerprint className="h-4 w-4 mr-2" />
                      {t('passkeys.usePasskeyButton')}
                    </>
                  )}
                  {passkeyLoading && (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      {t('passkeys.authenticating')}
                    </>
                  )}
                </Button>

                <button
                  type="button"
                  className="w-full text-center text-sm text-muted-foreground hover:underline"
                  onClick={() => {
                    setServerError('');
                    setPasskeyMode(false);
                  }}
                  disabled={passkeyLoading}
                >
                  {t('passkeys.backToPassword')}
                </button>
              </form>
            )}

            {/* SSO Provider Buttons */}
            {ssoProviders.length > 0 && (
              <>
                <div className="relative my-6">
                  <Separator />
                  <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-xs text-muted-foreground">
                    {t('login.orSignInWith')}
                  </span>
                </div>
                <div className="flex flex-col gap-2">
                  {ssoProviders.map((provider) => (
                    <Button
                      key={provider.id}
                      type="button"
                      variant="outline"
                      className="w-full"
                      disabled={ssoLoading !== null}
                      loading={ssoLoading === provider.id}
                      onClick={() => handleSsoLogin(provider.id)}
                    >
                      {ssoLoading !== provider.id && (
                        <KeyRound className="h-4 w-4 mr-2" />
                      )}
                      {ssoLoading === provider.id
                        ? t('login.ssoLoading')
                        : t('login.ssoButton', { name: provider.name })}
                    </Button>
                  ))}
                </div>
              </>
            )}

            <div className="relative my-6">
              <Separator />
              <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-xs text-muted-foreground">
                Demo Access
              </span>
            </div>

            <p className="text-center text-sm text-muted-foreground">
              Use <strong className="font-semibold">admin</strong> / <strong className="font-semibold">admin123</strong> for demo
            </p>

            <div className="text-center mt-6">
              <Button variant="ghost" size="sm" asChild>
                <Link to="/">
                  <Info className="h-4 w-4 mr-2" />
                  Learn what ShipFlow can do
                </Link>
              </Button>
            </div>
          </CardContent>
        </div>
      </Card>

      {showPostLoginPrompts && <PostLoginPrompts onComplete={goToDestination} />}
    </div>
  );
}
