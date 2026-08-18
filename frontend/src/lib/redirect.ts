/**
 * Post-login redirect helpers.
 *
 * When an unauthenticated (or expired-token) visitor opens a deep link, the
 * intended destination must survive the trip through the login page. Three
 * channels carry it, in priority order:
 *
 * 1. `location.state.from` — set by `ProtectedRoute` on a client-side
 *    `<Navigate>`; richest, but lost on a full page reload.
 * 2. `?redirect=` on `/login` — set by `ProtectedRoute` *and* by the axios
 *    401 interceptor (which does a hard `window.location` navigation, so state
 *    is not an option there). Survives reloads and copy/pasted URLs.
 * 3. `sessionStorage` — the only channel that survives leaving the origin
 *    entirely, i.e. the round trip to an external SSO identity provider.
 *
 * Everything read back from these channels goes through `sanitizeRedirect`
 * so a crafted `?redirect=` can never bounce a user off-site (open redirect).
 */

/** Where to land when no intended destination was recorded. */
export const DEFAULT_POST_LOGIN_PATH = '/dashboard';

/** Query-string parameter carrying the intended destination. */
export const REDIRECT_PARAM = 'redirect';

const PENDING_REDIRECT_KEY = 'shipflow_post_login_redirect';

/**
 * Routes that must never be a post-login destination — bouncing back to the
 * login page after logging in would loop.
 */
const AUTH_PATHS = ['/login', '/sso-callback'];

interface PathLike {
  pathname: string;
  search?: string;
  hash?: string;
}

/** Collapses a location into the `pathname + search + hash` form routers take. */
export function toRelativePath(location: PathLike): string {
  return `${location.pathname}${location.search ?? ''}${location.hash ?? ''}`;
}

/** The URL currently shown in the address bar, as a relative path. */
export function currentRelativePath(): string {
  return toRelativePath(window.location);
}

/** True when the browser is already sitting on `/login` or `/sso-callback`. */
export function isOnAuthPage(pathname: string = window.location.pathname): boolean {
  return AUTH_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`));
}

/**
 * Validates an untrusted redirect target.
 *
 * Returns a normalised same-origin relative path, or `null` when the value is
 * unusable. Rejected: absolute/scheme-relative URLs (`https://evil.com`,
 * `//evil.com`, `/\evil.com`), anything with whitespace or control characters,
 * the auth routes themselves, and the bare landing page (`/dashboard` is a
 * better place to land than the marketing page).
 */
export function sanitizeRedirect(raw: string | null | undefined): string | null {
  if (typeof raw !== 'string') return null;

  const value = raw.trim();
  if (!value.startsWith('/')) return null;
  // Scheme-relative (`//host`) and the backslash variant browsers normalise to
  // it both escape the origin despite the leading slash.
  if (value.startsWith('//') || value.startsWith('/\\')) return null;
  // eslint-disable-next-line no-control-regex
  if (/[\s\u0000-\u001f\u007f]/.test(value)) return null;

  let url: URL;
  try {
    url = new URL(value, 'http://localhost');
  } catch {
    return null;
  }

  if (url.pathname === '/' && !url.search && !url.hash) return null;
  if (isOnAuthPage(url.pathname)) return null;

  return `${url.pathname}${url.search}${url.hash}`;
}

/**
 * Builds the login URL for an interrupted navigation, carrying the intended
 * destination in the query string.
 */
export function buildLoginPath(target?: string | null): string {
  const safe = sanitizeRedirect(target);
  return safe ? `/login?${REDIRECT_PARAM}=${encodeURIComponent(safe)}` : '/login';
}

/**
 * Stashes the intended destination across a navigation that leaves the app
 * (the SSO hand-off to an external identity provider).
 */
export function rememberRedirect(target: string | null | undefined): void {
  const safe = sanitizeRedirect(target);
  try {
    if (safe) sessionStorage.setItem(PENDING_REDIRECT_KEY, safe);
    else sessionStorage.removeItem(PENDING_REDIRECT_KEY);
  } catch {
    // Private-mode / storage-disabled browsers: the query-string channel still works.
  }
}

/** Reads the stashed destination without clearing it. */
export function peekRedirect(): string | null {
  try {
    return sanitizeRedirect(sessionStorage.getItem(PENDING_REDIRECT_KEY));
  } catch {
    return null;
  }
}

/** Reads and clears the stashed destination. */
export function consumeRedirect(): string | null {
  const value = peekRedirect();
  clearPendingRedirect();
  return value;
}

/** Drops any stashed destination (on logout, and once one has been used). */
export function clearPendingRedirect(): void {
  try {
    sessionStorage.removeItem(PENDING_REDIRECT_KEY);
  } catch {
    // Nothing to clean up if storage is unavailable.
  }
}

/**
 * Resolves where to send the user after a successful sign-in, checking every
 * channel in priority order. Read-only — call `clearPendingRedirect()` once the
 * navigation actually happens.
 */
export function resolvePostLoginTarget(location: {
  state?: unknown;
  search?: string;
}): string {
  const from = (location.state as { from?: PathLike | string } | null | undefined)?.from;
  if (from) {
    const fromPath = typeof from === 'string' ? from : toRelativePath(from);
    const safe = sanitizeRedirect(fromPath);
    if (safe) return safe;
  }

  const fromQuery = sanitizeRedirect(
    new URLSearchParams(location.search ?? '').get(REDIRECT_PARAM),
  );
  if (fromQuery) return fromQuery;

  return peekRedirect() ?? DEFAULT_POST_LOGIN_PATH;
}
