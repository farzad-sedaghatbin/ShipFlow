/**
 * User-friendly error messages for common API errors
 * Maps technical errors to readable messages
 * Now supports i18n through translation function parameter
 */

export interface ApiErrorDetails {
  title: string;
  message: string;
  action?: string;
}

type TranslateFn = (key: string, options?: Record<string, unknown>) => string;

// Legacy domain error message mapping (for backward compatibility with backend error codes)
const domainErrorCodeMapping: Record<string, string> = {
  'project.name.required': 'errors.domain.project.name.required',
  'project.name.duplicate': 'errors.domain.project.name.duplicate',
  'project.key.required': 'errors.domain.project.key.required',
  'project.key.duplicate': 'errors.domain.project.key.duplicate',
  'project.key.invalid': 'errors.domain.project.key.invalid',
  'project.has.cycles': 'errors.domain.project.hasCycles',
  'project.not.found': 'errors.domain.project.notFound',
  
  'cycle.name.required': 'errors.domain.cycle.name.required',
  'cycle.dates.required': 'errors.domain.cycle.dates.required',
  'cycle.dates.invalid': 'errors.domain.cycle.dates.invalid',
  'cycle.project.required': 'errors.domain.cycle.project.required',
  'cycle.not.found': 'errors.domain.cycle.notFound',
  'cycle.has.pitches': 'errors.domain.cycle.hasPitches',
  
  'pitch.title.required': 'errors.domain.pitch.title.required',
  'pitch.cycle.required': 'errors.domain.pitch.cycle.required',
  'pitch.appetite.invalid': 'errors.domain.pitch.appetite.invalid',
  'pitch.not.found': 'errors.domain.pitch.notFound',
  'pitch.has.tasks': 'errors.domain.pitch.hasTasks',
  
  'task.title.required': 'errors.domain.task.title.required',
  'task.cycle.required': 'errors.domain.task.cycle.required',
  'task.not.found': 'errors.domain.task.notFound',
  'task.estimate.invalid': 'errors.domain.task.estimate.invalid',
  
  'team.name.required': 'errors.domain.team.name.required',
  'team.name.duplicate': 'errors.domain.team.name.duplicate',
  'team.not.found': 'errors.domain.team.notFound',
  'team.has.members': 'errors.domain.team.hasMembers',
  
  'person.name.required': 'errors.domain.person.name.required',
  'person.email.required': 'errors.domain.person.email.required',
  'person.email.invalid': 'errors.domain.person.email.invalid',
  'person.email.duplicate': 'errors.domain.person.email.duplicate',
  'person.not.found': 'errors.domain.person.notFound',
  
  'auth.invalid.credentials': 'errors.domain.auth.invalidCredentials',
  'auth.account.locked': 'errors.domain.auth.accountLocked',
  'auth.account.disabled': 'errors.domain.auth.accountDisabled',
  'auth.token.expired': 'errors.domain.auth.tokenExpired',
  'auth.token.invalid': 'errors.domain.auth.tokenInvalid',
  
  'worklog.hours.required': 'errors.domain.worklog.hours.required',
  'worklog.hours.invalid': 'errors.domain.worklog.hours.invalid',
  'worklog.date.required': 'errors.domain.worklog.date.required',
  'worklog.date.future': 'errors.domain.worklog.date.future',
  
  'document.file.required': 'errors.domain.document.file.required',
  'document.file.too.large': 'errors.domain.document.file.tooLarge',
  'document.type.invalid': 'errors.domain.document.file.typeInvalid',
  
  'validation.failed': 'errors.domain.validation.failed',
  'network.error': 'errors.networkError',
  'unknown.error': 'errors.domain.unknown.error',
};

/**
 * Get a user-friendly error message from an API error
 * @param error - The error object from API call
 * @param tOrFallback - Either translation function from useTranslation hook OR fallback message string (for backward compatibility)
 * @param fallbackMessage - Optional fallback message if first param is translation function
 */
export function getUserFriendlyError(
  error: unknown,
  tOrFallback?: TranslateFn | string,
  fallbackMessage = 'An unexpected error occurred'
): string {
  // Determine if second param is translation function or fallback string
  const isTranslationFn = typeof tOrFallback === 'function';
  const t = isTranslationFn ? tOrFallback : undefined;
  const fallback = isTranslationFn ? fallbackMessage : (tOrFallback || fallbackMessage);
  
  if (!error) return t ? t('errors.domain.unknown.error') : fallback;

  // Handle axios-style errors
  const axiosError = error as {
    response?: {
      status?: number;
      data?: {
        message?: string;
        error?: string;
        errorCode?: string;
        errors?: Record<string, string[]>;
      };
    };
    message?: string;
  };

  // Check for error code from backend
  const errorCode = axiosError.response?.data?.errorCode;
  if (errorCode && domainErrorCodeMapping[errorCode]) {
    return t ? t(domainErrorCodeMapping[errorCode]) : fallback;
  }

  // Check for backend message
  const backendMessage = axiosError.response?.data?.message || axiosError.response?.data?.error;
  if (backendMessage) {
    // Check if the backend message matches a domain error code
    const matchedCode = Object.keys(domainErrorCodeMapping).find(
      code => code === backendMessage || backendMessage.toLowerCase().includes(code.toLowerCase())
    );
    if (matchedCode && t) {
      return t(domainErrorCodeMapping[matchedCode]);
    }
    return backendMessage;
  }

  // Check for validation errors from backend
  const validationErrors = axiosError.response?.data?.errors;
  if (validationErrors) {
    const errorMessages = Object.values(validationErrors).flat();
    if (errorMessages.length > 0) {
      return errorMessages.join('. ');
    }
  }

  // Check for HTTP status code
  const status = axiosError.response?.status;
  if (status && t) {
    const statusKey = `errors.http.${status}.message`;
    const translated = t(statusKey);
    // Check if translation exists (doesn't return the key itself)
    if (translated !== statusKey) {
      return translated;
    }
  }

  // Check for network error
  if (!axiosError.response && axiosError.message) {
    if (axiosError.message.includes('Network Error') || axiosError.message.includes('ERR_NETWORK')) {
      return t ? t('errors.networkError') : 'Network error occurred';
    }
    return axiosError.message;
  }

  return t ? t('errors.domain.unknown.error') : fallback;
}

/**
 * Get detailed error info including title, message, and action
 * @param error - The error object from API call
 * @param t - Translation function from useTranslation hook
 */
export function getDetailedError(error: unknown, t?: TranslateFn): ApiErrorDetails {
  const axiosError = error as {
    response?: {
      status?: number;
      data?: { message?: string; error?: string };
    };
    message?: string;
  };

  const status = axiosError.response?.status;
  if (status && t) {
    const titleKey = `errors.http.${status}.title`;
    const messageKey = `errors.http.${status}.message`;
    const actionKey = `errors.http.${status}.action`;
    
    const title = t(titleKey);
    const message = t(messageKey);
    const action = t(actionKey);
    
    // Check if translations exist
    if (title !== titleKey && message !== messageKey) {
      return {
        title,
        message,
        action: action !== actionKey ? action : undefined,
      };
    }
  }

  if (!axiosError.response) {
    return {
      title: t ? t('errors.domain.connection.title') : 'Connection Error',
      message: t ? t('errors.domain.connection.message') : 'Unable to connect to the server.',
      action: t ? t('errors.domain.connection.action') : 'Please check your internet connection and try again.',
    };
  }

  return {
    title: t ? t('errors.genericError') : 'Error',
    message: getUserFriendlyError(error, t),
  };
}

/**
 * Format validation errors for form fields
 */
export function formatValidationErrors(
  errors: Record<string, string[]> | undefined
): Record<string, string> {
  if (!errors) return {};

  const formatted: Record<string, string> = {};
  for (const [field, messages] of Object.entries(errors)) {
    // Convert field names to user-friendly format
    const friendlyField = field
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim();
    formatted[field] = messages[0] || `${friendlyField} is invalid`;
  }
  return formatted;
}
