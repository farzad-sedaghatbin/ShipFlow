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

const etagCache = new Map<string, ETagEntry>();

/** Build a stable cache key from HTTP method + URL with query params sorted for consistency. */
function etagKey(method: string, url: string): string {
  try {
    // Use URL/URLSearchParams to normalize query parameters order.
    const parsed = new URL(url, window.location.origin);
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
};

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

    // Inject If-None-Match for GET requests when we have a cached ETag
    if (config.method?.toUpperCase() === 'GET' && config.url) {
      const key = etagKey(config.method, config.url);
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
    // Store ETag for successful GET responses
    if (response.config.method?.toUpperCase() === 'GET' && response.config.url) {
      const etag = response.headers['etag'];
      if (etag) {
        const key = etagKey(response.config.method, response.config.url);
        etagCache.set(key, { etag, data: response.data });
      }
    }
    return response;
  },
  (error: AxiosError<{ message?: string; error?: string; errorCode?: string; errors?: Record<string, string[]> }>) => {
    const status = error.response?.status;

    // Handle 304 Not Modified — return cached data as if it were 200
    if (status === 304 && error.config?.method && error.config?.url) {
      const key = etagKey(error.config.method, error.config.url);
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
      showGlobalToast(GLOBAL_ERROR_MESSAGES.networkError, 'error');
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
