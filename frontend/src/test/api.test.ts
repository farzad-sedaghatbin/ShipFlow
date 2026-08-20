import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import api, { clearEtagCache } from '../services/api';
import { getStoredToken } from '../contexts';
import { peekRedirect, rememberRedirect } from '../lib/redirect';

/**
 * Covers the 401 response interceptor's split between "always clean up an
 * invalid token" and "skip the toast + redirect while already on an auth
 * page" — see CHANGELOG.md. Uses a custom axios adapter (no real network)
 * so the real interceptor chain runs end to end.
 */
function mockAdapter(status: number): (config: AxiosRequestConfig) => Promise<AxiosResponse> {
  return (config: AxiosRequestConfig) => {
    const error = {
      isAxiosError: true as const,
      config,
      response: {
        status,
        statusText: status === 401 ? 'Unauthorized' : 'Error',
        data: {},
        headers: {},
        config,
      },
    };
    return Promise.reject(error);
  };
}

// `window.location.href = ...` triggers jsdom's real (unimplemented)
// navigation and never actually updates `location`. Stand in a plain,
// writable object so the interceptor's assignment is observable.
const realLocation = window.location;
function stubLocation(pathname: string): { href: string } {
  const stub = { pathname, search: '', hash: '', href: `http://localhost:3000${pathname}` };
  Object.defineProperty(window, 'location', { value: stub, writable: true, configurable: true });
  return stub;
}
function restoreLocation(): void {
  Object.defineProperty(window, 'location', { value: realLocation, writable: true, configurable: true });
}

describe('api 401 response interceptor', () => {
  beforeEach(() => {
    localStorage.setItem('shipflow_token', 'stale-jwt');
    localStorage.setItem('shipflow_user', JSON.stringify({ userId: 1, username: 'a', role: 'ADMIN' }));
    rememberRedirect('/qa/bug-reports/9');
  });

  afterEach(() => {
    vi.restoreAllMocks();
    restoreLocation();
    localStorage.clear();
    clearEtagCache();
  });

  it('clears the stale token and pending-redirect stash even while sitting on /login', async () => {
    stubLocation('/login');
    api.defaults.adapter = mockAdapter(401);

    await expect(api.get('/users/me')).rejects.toBeTruthy();

    expect(getStoredToken()).toBeNull();
    expect(peekRedirect()).toBeNull();
  });

  it('does not redirect away while already on an auth page', async () => {
    const stub = stubLocation('/login');
    const hrefBefore = stub.href;
    api.defaults.adapter = mockAdapter(401);

    await expect(api.get('/users/me')).rejects.toBeTruthy();

    // Redirecting here would clobber whatever error the login form is showing.
    expect(stub.href).toBe(hrefBefore);
  });

  it('still redirects to login (carrying the destination) when the 401 happens elsewhere', async () => {
    const stub = stubLocation('/backlog/42');
    api.defaults.adapter = mockAdapter(401);

    await expect(api.get('/users/me')).rejects.toBeTruthy();

    expect(getStoredToken()).toBeNull();
    expect(stub.href).toContain('/login?redirect=%2Fbacklog%2F42');
  });

  it('does not clear the token for a failed login attempt itself', async () => {
    stubLocation('/login');
    api.defaults.adapter = mockAdapter(401);

    await expect(api.post('/auth/login', {})).rejects.toBeTruthy();

    // A bad-credentials 401 on the login endpoint isn't a session expiry —
    // there's no valid token to invalidate and the form handles its own error.
    expect(getStoredToken()).toBe('stale-jwt');
  });
});
