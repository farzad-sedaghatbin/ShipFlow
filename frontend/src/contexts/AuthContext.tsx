import { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCurrentUser } from '../hooks/useCurrentUser';
import { clearPendingRedirect } from '../lib/redirect';

interface User {
  userId: number;
  username: string;
  role: string;
  personId?: number;
  personName?: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const TOKEN_KEY = 'shipflow_token';
const USER_KEY = 'shipflow_user';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // Load auth state from localStorage on mount
    const storedToken = localStorage.getItem(TOKEN_KEY);
    const storedUser = localStorage.getItem(USER_KEY);

    if (storedToken && storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        setToken(storedToken);
        setUser(parsedUser);
      } catch (e) {
        // Invalid stored data, clear it
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  // ─── Live user sync via React Query ────────────────────────────────
  // Runs alongside the localStorage bootstrap: once the server responds,
  // we merge any changes (role change, deactivation, linked person, etc.)
  // back into the auth state *and* localStorage. When the response body
  // hasn't changed, the ETag 304 path ensures zero body transfer.
  const { data: serverUser } = useCurrentUser(!!token);

  useEffect(() => {
    if (!serverUser || !token) return;

    const merged: User = {
      userId: serverUser.userId,
      username: serverUser.username,
      role: serverUser.role,
      personId: serverUser.personId,
      personName: serverUser.personName,
    };

    setUser(merged);
    localStorage.setItem(USER_KEY, JSON.stringify(merged));
  }, [serverUser, token]);
  // ───────────────────────────────────────────────────────────────────

  const login = useCallback((newToken: string, newUser: User) => {
    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(USER_KEY, JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setToken(null);
    setUser(null);
    navigate('/login');
  }, [navigate]);

  const value = {
    user,
    token,
    isAuthenticated: !!token,
    isLoading,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// Export for use in api.ts (outside React)
export const getStoredToken = () => localStorage.getItem(TOKEN_KEY);
export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  // A cleared token can never be resumed, so any stashed post-login
  // destination is meaningless to keep — drop it here so both an explicit
  // logout and an auto-logout on a rejected token (api.ts's 401 handler)
  // can't leave a stale destination to hijack an unrelated later sign-in.
  clearPendingRedirect();
};
