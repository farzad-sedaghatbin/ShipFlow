import axios, { AxiosError } from 'axios';
import { getStoredToken, clearAuth, showGlobalToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';

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

// Request interceptor - add auth token
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
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle errors with user-friendly messages
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string; error?: string; errorCode?: string; errors?: Record<string, string[]> }>) => {
    const status = error.response?.status;
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
