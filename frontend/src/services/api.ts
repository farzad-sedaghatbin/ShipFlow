import axios, { AxiosError, AxiosResponse } from 'axios';
import { getStoredToken, clearAuth, showGlobalToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import { AUTH_ENDPOINTS, buildLoginPath, currentRelativePath, isOnAuthPage } from '../lib/redirect';
// Import the raw i18next singleton, NOT the app's '../i18n' wrapper module.
// That wrapper's top-level code unconditionally calls i18n.init(...) with the
// full real translation bundles as a side effect of merely being imported —
// fine in production (main.tsx imports it once at bootstrap, before this
// interceptor could ever fire) but disastrous in tests: api.ts is imported by
// nearly every test file, and forcing the real bundles into the *shared*
// i18next singleton clobbers the intentionally-minimal resources
// src/test/setup.ts configures for tests that assert on raw (untranslated)
// keys — see LoginRedirect.test.tsx. Referencing the raw package here reuses
// whatever instance is already configured (by main.tsx in the app, or by
// setup.ts in tests) without re-initialising it.
import i18n from 'i18next';

// ─── ETag Cache ──────────────────────────────────────────────────────────────
// Stores the last ETag and response body for each GET URL so we can replay
// If-None-Match on the next request and resolve from cache on 304.

interface ETagEntry {
  etag: string;
  data: unknown;
}

/**
 * Maximum number of entries to keep in the ETag cache.
 * When exceeded the oldest (first-inserted) entry is evicted — simple FIFO that
 * is sufficient for browser tab lifetimes and costs O(1) per eviction.
 */
const MAX_ETAG_CACHE_SIZE = 300;

const etagCache = new Map<string, ETagEntry>();

/** Evict the oldest entry when the cache grows beyond MAX_ETAG_CACHE_SIZE. */
function trimEtagCache(): void {
  if (etagCache.size > MAX_ETAG_CACHE_SIZE) {
    const firstKey = etagCache.keys().next().value;
    if (firstKey !== undefined) etagCache.delete(firstKey);
  }
}

/** Clear all cached ETags and bodies (call on logout to free memory). */
export function clearEtagCache(): void {
  etagCache.clear();
}

/**
 * Build a stable cache key from HTTP method + URL.
 * Axios params objects are merged into the URL before normalisation so that
 * different query-string combinations never share the same key.
 */
function etagKey(
  method: string,
  url: string,
  params?: Record<string, unknown>,
): string {
  try {
    // Merge Axios config.params into the URL string before normalising.
    let fullUrl = url;
    if (params && typeof params === 'object') {
      const extra = new URLSearchParams(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null)
          .map(([k, v]) => [k, String(v)]),
      ).toString();
      if (extra) {
        fullUrl = url.includes('?') ? `${url}&${extra}` : `${url}?${extra}`;
      }
    }

    // Use URL/URLSearchParams to normalise query parameter order.
    const parsed = new URL(fullUrl, window.location.origin);
    const originalParams = new URLSearchParams(parsed.search);

    const sortedParamNames = Array.from(new Set(originalParams.keys())).sort();
    const normalizedParams = new URLSearchParams();

    for (const name of sortedParamNames) {
      const values = originalParams.getAll(name);
      values.sort();
      for (const value of values) {
        normalizedParams.append(name, value);
      }
    }

    const normalizedSearch = normalizedParams.toString();
    const normalizedUrl =
      parsed.pathname + (normalizedSearch ? `?${normalizedSearch}` : '');

    return `${method.toUpperCase()}:${normalizedUrl}`;
  } catch {
    // Fallback to the raw URL if parsing fails for any reason.
    return `${method.toUpperCase()}:${url}`;
  }
}
// ─────────────────────────────────────────────────────────────────────────────

// Hard-coded messages for global error handling (interceptor doesn't have access to i18n)
const GLOBAL_ERROR_MESSAGES = {
  unauthorized: 'Your session has expired for security reasons.',
  forbidden: "You don't have permission to perform this action.",
  serverError: 'Something went wrong on our end.',
  networkError: 'Unable to connect. Please check your internet connection.',
  offlineQueued: "You're offline — this change will be sent automatically once you're back online.",
};

const MUTATION_METHODS = new Set(['post', 'put', 'patch', 'delete']);

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token + ETag
api.interceptors.request.use(
  (config) => {
    const token = getStoredToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Remove Content-Type header for FormData to let browser set it with boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }

    // Inject If-None-Match for GET requests when we have a cached ETag.
    // Include config.params in the key so different query strings stay separate.
    if (config.method?.toUpperCase() === 'GET' && config.url) {
      const key = etagKey(config.method, config.url, config.params as Record<string, unknown>);
      const cached = etagCache.get(key);
      if (cached) {
        config.headers['If-None-Match'] = cached.etag;
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle ETags, errors with user-friendly messages
api.interceptors.response.use(
  (response: AxiosResponse) => {
    // Store ETag for successful GET responses.
    // Use config.params so the key matches what the request interceptor built.
    if (response.config.method?.toUpperCase() === 'GET' && response.config.url) {
      const etag = response.headers['etag'];
      if (etag) {
        const key = etagKey(
          response.config.method,
          response.config.url,
          response.config.params as Record<string, unknown>,
        );
        etagCache.set(key, { etag, data: response.data });
        trimEtagCache();
      }
    }
    return response;
  },
  (error: AxiosError<{ message?: string; error?: string; errorCode?: string; messageKey?: string; errors?: Record<string, string[]> }>) => {
    const status = error.response?.status;

    // Handle 304 Not Modified — return cached data as if it were 200
    if (status === 304 && error.config?.method && error.config?.url) {
      const key = etagKey(
        error.config.method,
        error.config.url,
        error.config.params as Record<string, unknown>,
      );
      const cached = etagCache.get(key);
      if (cached) {
        return Promise.resolve({
          data: cached.data,
          status: 304,
          statusText: 'Not Modified',
          headers: error.response?.headers ?? {},
          config: error.config,
        } as AxiosResponse);
      }
    }

    const userMessage = getUserFriendlyError(error);

    if (status === 401) {
      // A sign-in attempt (wrong credentials, failed passkey ceremony) means
      // "bad credentials", not "session expired" — the login form handles its
      // own error, and there's no valid token to clean up.
      const isAuthAttempt = AUTH_ENDPOINTS.some((path) => error.config?.url?.includes(path));
      if (!isAuthAttempt) {
        // Always drop the now-invalid token, even if we're already on an auth
        // page — an invalid token must never linger in storage regardless of
        // where the 401 happened to fire (e.g. a background user-sync query).
        clearEtagCache(); // free cached payloads and ETags on logout
        clearAuth();
        if (!isOnAuthPage()) {
          // Skip the toast + redirect while already on an auth page — the
          // form shows its own error, and redirecting would clobber a
          // captured `?redirect=`.
          showGlobalToast(GLOBAL_ERROR_MESSAGES.unauthorized, 'error');
          // Redirect to login, carrying the page the user was on so they land
          // back there after signing in. This is a full page load (we're
          // outside the router here), so the destination has to travel in
          // the query string.
          window.location.href = buildLoginPath(currentRelativePath());
        }
      }
    } else if (status === 403) {
      // Forbidden. Most 403s are a generic permission problem, but a disabled
      // public-registration gate (see UserService#createUser /
      // RegistrationDisabledException) has its own specific, actionable
      // message — worth a special case the same way 401's AUTH_ENDPOINTS
      // check is. i18n is imported directly (not via useTranslation) because
      // this interceptor runs outside any component; i18next initialises
      // synchronously (see i18n/index.ts) so it's always ready by the time a
      // request can fail.
      if (error.response?.data?.messageKey === 'auth.registration.disabled') {
        showGlobalToast(i18n.t('errors.domain.auth.registrationDisabled'), 'error');
      } else {
        showGlobalToast(GLOBAL_ERROR_MESSAGES.forbidden, 'error');
      }
    } else if (status === 404) {
      // Don't show toast for 404 - let the component handle it for better UX
      // Some 404s are expected (e.g., checking if something exists)
    } else if (status === 400 || status === 422) {
      // Validation errors - show specific message
      showGlobalToast(userMessage, 'error');
    } else if (status === 409) {
      // Conflict. Two distinct cases share this status code:
      //  - plain duplicate-data conflicts -> show the generic warning toast.
      //  - optimistic-lock version conflicts (v1.13.0 S64, Pitch/RetroItem/
      //    WikiPage updates with a stale `expectedVersion`) -> the calling
      //    page opens a ConflictDialog instead (see utils/conflictError.ts),
      //    so suppress the generic toast to avoid double-messaging the user.
      const data = error.response?.data as { currentVersion?: unknown } | undefined;
      const isOptimisticLockConflict = typeof data?.currentVersion === 'number';
      if (!isOptimisticLockConflict) {
        showGlobalToast(userMessage, 'warning');
      }
    } else if (status && status >= 500) {
      showGlobalToast(GLOBAL_ERROR_MESSAGES.serverError, 'error');
    } else if (!error.response) {
      // A failed API write while offline was queued by the service worker's
      // BackgroundSyncPlugin (see src/sw.ts) and will replay automatically —
      // that's a materially different situation from "the request failed",
      // so it gets its own message instead of the generic network-error one.
      const isQueueableMutation =
        !navigator.onLine && MUTATION_METHODS.has((error.config?.method ?? '').toLowerCase());
      showGlobalToast(
        isQueueableMutation ? GLOBAL_ERROR_MESSAGES.offlineQueued : GLOBAL_ERROR_MESSAGES.networkError,
        isQueueableMutation ? 'info' : 'error',
      );
    }

    // Log for debugging but don't expose technical details to users
    if (process.env.NODE_ENV === 'development') {
      console.error('API Error:', {
        status,
        message: error.response?.data?.message,
        errorCode: error.response?.data?.errorCode,
        url: error.config?.url,
      });
    }

    return Promise.reject(error);
  }
);

export default api;
