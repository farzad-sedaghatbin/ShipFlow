/**
 * User-friendly error messages for common API errors
 * Maps technical errors to readable messages
 */

export interface ApiErrorDetails {
  title: string;
  message: string;
  action?: string;
}

// Common HTTP status code messages
export const httpStatusMessages: Record<number, ApiErrorDetails> = {
  400: {
    title: 'Invalid Request',
    message: 'The information provided is incorrect or incomplete.',
    action: 'Please check your input and try again.',
  },
  401: {
    title: 'Session Expired',
    message: 'Your session has expired for security reasons.',
    action: 'Please log in again to continue.',
  },
  403: {
    title: 'Access Denied',
    message: "You don't have permission to perform this action.",
    action: 'Contact your administrator if you need access.',
  },
  404: {
    title: 'Not Found',
    message: 'The requested item could not be found.',
    action: 'It may have been deleted or moved.',
  },
  409: {
    title: 'Conflict',
    message: 'This action conflicts with existing data.',
    action: 'The item may already exist or has been modified.',
  },
  422: {
    title: 'Validation Error',
    message: 'The provided data did not pass validation.',
    action: 'Please check the highlighted fields and try again.',
  },
  429: {
    title: 'Too Many Requests',
    message: "You've made too many requests in a short time.",
    action: 'Please wait a moment and try again.',
  },
  500: {
    title: 'Server Error',
    message: 'Something went wrong on our end.',
    action: 'Please try again in a few moments.',
  },
  502: {
    title: 'Service Unavailable',
    message: 'The server is temporarily unavailable.',
    action: 'Please try again in a few moments.',
  },
  503: {
    title: 'Service Unavailable',
    message: 'The service is temporarily unavailable.',
    action: 'Please try again later.',
  },
};

// Domain-specific error messages
export const domainErrorMessages: Record<string, string> = {
  // Project errors
  'project.name.required': 'Project name is required',
  'project.name.duplicate': 'A project with this name already exists',
  'project.key.required': 'Project key is required',
  'project.key.duplicate': 'A project with this key already exists',
  'project.key.invalid': 'Project key must be 2-10 uppercase letters or numbers',
  'project.has.cycles': 'Cannot delete project with existing cycles',
  'project.not.found': 'Project not found',

  // Cycle errors
  'cycle.name.required': 'Cycle name is required',
  'cycle.dates.required': 'Start and end dates are required',
  'cycle.dates.invalid': 'End date must be after start date',
  'cycle.project.required': 'Please select a project',
  'cycle.not.found': 'Cycle not found',
  'cycle.has.pitches': 'Cannot delete cycle with existing pitches',

  // Pitch errors
  'pitch.title.required': 'Pitch title is required',
  'pitch.cycle.required': 'Please select a cycle',
  'pitch.appetite.invalid': 'Appetite must be between 1 and 42 days',
  'pitch.not.found': 'Pitch not found',
  'pitch.has.tasks': 'Cannot delete pitch with existing tasks',

  // Task errors
  'task.title.required': 'Task title is required',
  'task.cycle.required': 'Please select a cycle',
  'task.not.found': 'Task not found',
  'task.estimate.invalid': 'Estimate hours must be a positive number',

  // Team errors
  'team.name.required': 'Team name is required',
  'team.name.duplicate': 'A team with this name already exists',
  'team.not.found': 'Team not found',
  'team.has.members': 'Cannot delete team with existing members',

  // Person errors
  'person.name.required': 'Person name is required',
  'person.email.required': 'Email is required',
  'person.email.invalid': 'Please enter a valid email address',
  'person.email.duplicate': 'A person with this email already exists',
  'person.not.found': 'Person not found',

  // Auth errors
  'auth.invalid.credentials': 'Invalid username or password',
  'auth.account.locked': 'Account is locked. Contact your administrator.',
  'auth.account.disabled': 'Account is disabled. Contact your administrator.',
  'auth.token.expired': 'Your session has expired. Please log in again.',
  'auth.token.invalid': 'Invalid session. Please log in again.',

  // Work log errors
  'worklog.hours.required': 'Hours worked is required',
  'worklog.hours.invalid': 'Hours must be between 0.25 and 24',
  'worklog.date.required': 'Work date is required',
  'worklog.date.future': 'Work date cannot be in the future',

  // Document errors
  'document.file.required': 'Please select a file to upload',
  'document.file.too.large': 'File size exceeds the maximum limit (10MB)',
  'document.type.invalid': 'This file type is not supported',

  // Generic errors
  'validation.failed': 'Please check the form for errors',
  'network.error': 'Unable to connect. Please check your internet connection.',
  'unknown.error': 'An unexpected error occurred. Please try again.',
};

/**
 * Get a user-friendly error message from an API error
 */
export function getUserFriendlyError(
  error: unknown,
  fallbackMessage = 'An unexpected error occurred'
): string {
  if (!error) return fallbackMessage;

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
  if (errorCode && domainErrorMessages[errorCode]) {
    return domainErrorMessages[errorCode];
  }

  // Check for backend message
  const backendMessage = axiosError.response?.data?.message || axiosError.response?.data?.error;
  if (backendMessage) {
    // Check if the backend message matches a domain error
    const matchedDomain = Object.entries(domainErrorMessages).find(
      ([, msg]) => msg.toLowerCase() === backendMessage.toLowerCase()
    );
    if (matchedDomain) {
      return matchedDomain[1];
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
  if (status && httpStatusMessages[status]) {
    return httpStatusMessages[status].message;
  }

  // Check for network error
  if (!axiosError.response && axiosError.message) {
    if (axiosError.message.includes('Network Error') || axiosError.message.includes('ERR_NETWORK')) {
      return domainErrorMessages['network.error'];
    }
    return axiosError.message;
  }

  return fallbackMessage;
}

/**
 * Get detailed error info including title, message, and action
 */
export function getDetailedError(error: unknown): ApiErrorDetails {
  const axiosError = error as {
    response?: {
      status?: number;
      data?: { message?: string; error?: string };
    };
    message?: string;
  };

  const status = axiosError.response?.status;
  if (status && httpStatusMessages[status]) {
    return httpStatusMessages[status];
  }

  if (!axiosError.response) {
    return {
      title: 'Connection Error',
      message: 'Unable to connect to the server.',
      action: 'Please check your internet connection and try again.',
    };
  }

  return {
    title: 'Error',
    message: getUserFriendlyError(error),
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
