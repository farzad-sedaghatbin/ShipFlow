import { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff, LogIn, Info, Loader2 } from 'lucide-react';
import { useAuth, useToast } from '../contexts';
import { authService } from '../services/authService';
import { LoginIllustration } from '../components/illustrations';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';

export default function Login() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { showSuccess } = useToast();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  // Load remembered username on mount
  useEffect(() => {
    const rememberedUsername = localStorage.getItem('rememberedUsername');
    if (rememberedUsername) {
      setUsername(rememberedUsername);
      setRememberMe(true);
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authService.login({ username, password });
      const { token, userId, username: user, role, personId, personName } = response.data;

      // Handle remember me
      if (rememberMe) {
        localStorage.setItem('rememberedUsername', username);
      } else {
        localStorage.removeItem('rememberedUsername');
      }

      login(token, { userId, username: user, role, personId, personName });
      showSuccess(t('login.loginSuccess'));
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const errorMessage = (err as { response?: { data?: { message?: string } } })?.response?.data?.message 
        || t('login.invalidCredentials');
      setError(errorMessage);
    } finally {
      setLoading(false);
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

            {error && (
              <Alert variant="destructive" className="mb-6">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="username">{t('login.username')}</Label>
                <Input
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoFocus={!username}
                  autoComplete="username"
                  placeholder={t('login.username')}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">{t('login.password')}</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                    placeholder={t('login.password')}
                    className="pr-10"
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
                disabled={loading || !username || !password}
              >
                {loading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    {t('common.loading')}
                  </>
                ) : (
                  <>
                    <LogIn className="h-4 w-4 mr-2" />
                    {t('login.signIn')}
                  </>
                )}
              </Button>
            </form>

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
    </div>
  );
}
