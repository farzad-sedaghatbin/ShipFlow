import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';
import api, { clearEtagCache } from '../services/api';
import { getStoredToken, setToastHandler } from '../contexts';
import { peekRedirect, rememberRedirect } from '../lib/redirect';
// Raw i18next package, not the app's '../i18n' wrapper — see the comment on
// the matching import in services/api.ts for why: the wrapper's own import
// forces real translation bundles into the shared test singleton, clobbering
// other test files' (e.g. LoginRedirect.test.tsx) reliance on setup.ts's
// intentionally-minimal resources.
import i18n from 'i18next';
// Plain JSON data import (no side effects, doesn't touch the i18next
// singleton) — read the one real string this test needs directly from the
// source of truth instead of duplicating it as a hardcoded literal here.
import enTranslations from '../i18n/locales/en.json';

/**
 * Covers the 401 response interceptor's split between "always clean up an
 * invalid token" and "skip the toast + redirect while already on an auth
 * page" — see CHANGELOG.md. Uses a custom axios adapter (no real network)
 * so the real interceptor chain runs end to end.
 */
function mockAdapter(
  status: number,
  data: unknown = {},
): (config: AxiosRequestConfig) => Promise<AxiosResponse> {
  return (config: AxiosRequestConfig) => {
    const error = {
      isAxiosError: true as const,
      config,
      response: {
        status,
        statusText: status === 401 ? 'Unauthorized' : 'Error',
        data,
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

/**
 * Covers the 403 response interceptor's special case for a disabled
 * public-registration gate (messageKey: "auth.registration.disabled" — see
 * RegistrationDisabledException / GlobalExceptionHandler on the backend and
 * CHANGELOG.md) versus the generic "forbidden" toast every other 403 gets.
 */
describe('api 403 response interceptor', () => {
  const toastHandler = vi.fn();
  // setup.ts (global test i18n init) only loads a tiny hand-picked resource
  // set, not the real en.json/fa.json bundles (importing those for real is
  // what caused the api.ts/api.test.ts regression against LoginRedirect.test.tsx
  // — see the import comment above). Inject just this one real key/value pair
  // so the assertion below exercises the actual copy instead of a fallback.
  const expectedMessage = enTranslations.errors.domain.auth.registrationDisabled;

  beforeEach(() => {
    setToastHandler(toastHandler);
    i18n.addResource('en', 'translation', 'errors.domain.auth.registrationDisabled', expectedMessage);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    toastHandler.mockClear();
    setToastHandler(() => {});
    clearEtagCache();
  });

  it('shows the friendly, translated message for a disabled-registration 403', async () => {
    api.defaults.adapter = mockAdapter(403, { messageKey: 'auth.registration.disabled' });

    await expect(api.post('/auth/register', {})).rejects.toBeTruthy();

    expect(toastHandler).toHaveBeenCalledWith(expectedMessage, 'error');
    // Sanity: the translation actually resolved to real copy, not the raw key
    // (which is what a missing/renamed i18n key would silently fall back to).
    expect(toastHandler.mock.calls[0][0]).not.toBe('errors.domain.auth.registrationDisabled');
  });

  it('falls back to the generic forbidden message for any other 403', async () => {
    api.defaults.adapter = mockAdapter(403, { messageKey: 'auth.access.denied' });

    await expect(api.get('/projects/1')).rejects.toBeTruthy();

    expect(toastHandler).toHaveBeenCalledWith(
      "You don't have permission to perform this action.",
      'error',
    );
  });
});
