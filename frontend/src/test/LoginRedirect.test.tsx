import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Hoisted so the vi.mock factory below can reach them. `authState` lets the
// faked AuthContext flip to "signed in" the moment Login calls login(), which is
// what ProtectedRoute reads when we land back on the protected route.
const { mockLogin, authState } = vi.hoisted(() => ({
  mockLogin: vi.fn(),
  authState: { authenticated: false },
}));

vi.mock('../contexts', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../contexts')>()),
  useAuth: () => ({
    user: null,
    token: authState.authenticated ? 'jwt' : null,
    isAuthenticated: authState.authenticated,
    isLoading: false,
    login: mockLogin,
    logout: vi.fn(),
  }),
  useToast: () => ({
    showToast: vi.fn(),
    showSuccess: vi.fn(),
    showError: vi.fn(),
    showWarning: vi.fn(),
    showInfo: vi.fn(),
  }),
}));

vi.mock('../services/authService', () => ({
  authService: {
    login: vi.fn().mockResolvedValue({
      data: { token: 'jwt', userId: 1, username: 'admin', role: 'ADMIN' },
    }),
  },
}));

vi.mock('../services/ssoService', () => ({
  getEnabledProviders: vi.fn().mockResolvedValue([]),
  initiateSSO: vi.fn(),
}));

vi.mock('../services/passkeyService', () => ({
  passkeyService: { listPasskeys: vi.fn().mockResolvedValue([]) },
}));

// `lib/pwa` pulls in `virtual:pwa-register`, which only exists once
// vite-plugin-pwa is in the pipeline (it isn't under vitest).
vi.mock('../lib/pwa', () => ({
  initPwa: vi.fn(),
  isPwaInstalled: () => false,
  canPromptPwaInstall: () => false,
  promptPwaInstall: vi.fn(),
}));

import Login from '../pages/Login';
import ProtectedRoute from '../components/ProtectedRoute';
import { clearPendingRedirect, rememberRedirect } from '../lib/redirect';

/** Always-mounted probe so assertions can read the current URL. */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
}

function renderAt(entry: string) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[entry]}>
        <LocationProbe />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <div>protected content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const location = () => screen.getByTestId('location');

async function signIn() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('login.username'), 'admin');
  await user.type(screen.getByLabelText('login.password'), 'admin123');
  await user.click(screen.getByRole('button', { name: /login\.signIn/ }));
}

describe('login redirect', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.authenticated = false;
    mockLogin.mockImplementation(() => {
      authState.authenticated = true;
    });
    clearPendingRedirect();
    localStorage.clear();
  });

  it('sends an unauthenticated deep link to login with the destination attached', async () => {
    renderAt('/backlog/42?tab=scopes');

    await waitFor(() =>
      expect(location()).toHaveTextContent('/login?redirect=%2Fbacklog%2F42%3Ftab%3Dscopes')
    );
    expect(screen.getByLabelText('login.username')).toBeInTheDocument();
  });

  it('returns to the interrupted route after signing in', async () => {
    renderAt('/backlog/42?tab=scopes');
    await waitFor(() => expect(screen.getByLabelText('login.username')).toBeInTheDocument());

    await signIn();

    await waitFor(() => expect(location()).toHaveTextContent('/backlog/42?tab=scopes'));
    expect(screen.getByText('protected content')).toBeInTheDocument();
    expect(mockLogin).toHaveBeenCalledWith('jwt', expect.objectContaining({ username: 'admin' }));
  });

  it('honours a ?redirect= landed on directly (e.g. after a 401 hard redirect)', async () => {
    renderAt('/login?redirect=%2Fcycles%2F7');
    await signIn();

    await waitFor(() => expect(location()).toHaveTextContent('/cycles/7'));
  });

  it('uses the destination stashed before an SSO round trip', async () => {
    rememberRedirect('/qa/bug-reports/9');
    renderAt('/login');
    await signIn();

    await waitFor(() => expect(location()).toHaveTextContent('/qa/bug-reports/9'));
  });

  it('ignores an off-site ?redirect= and falls back to the dashboard', async () => {
    renderAt('/login?redirect=https%3A%2F%2Fevil.com%2Fsteal');
    await signIn();

    await waitFor(() => expect(location()).toHaveTextContent('/dashboard'));
  });

  it('goes to the dashboard when there was no interrupted route', async () => {
    renderAt('/login');
    await signIn();

    await waitFor(() => expect(location()).toHaveTextContent('/dashboard'));
  });
});
