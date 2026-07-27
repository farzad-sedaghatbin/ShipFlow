import axios, { AxiosError, AxiosResponse } from 'axios';
import { getStoredToken, clearAuth, showGlobalToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';

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
  (error: AxiosError<{ message?: string; error?: string; errorCode?: string; errors?: Record<string, string[]> }>) => {
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
      // Skip redirect if this is a login attempt (wrong credentials) — let the login form handle it
      const isLoginRequest = error.config?.url?.includes('/auth/login');
      if (!isLoginRequest) {
        // Unauthorized - token expired or invalid
        showGlobalToast(GLOBAL_ERROR_MESSAGES.unauthorized, 'error');
        clearEtagCache(); // free cached payloads and ETags on logout
        clearAuth();
        // Redirect to login
        window.location.href = '/login';
      }
    } else if (status === 403) {
      // Forbidden - no permission
      showGlobalToast(GLOBAL_ERROR_MESSAGES.forbidden, 'error');
    } else if (status === 404) {
      // Don't show toast for 404 - let the component handle it for better UX
      // Some 404s are expected (e.g., checking if something exists)
    } else if (status === 400 || status === 422) {
      // Validation errors - show specific message
      showGlobalToast(userMessage, 'error');
    } else if (status === 409) {
      // Conflict - duplicate data
      showGlobalToast(userMessage, 'warning');
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
