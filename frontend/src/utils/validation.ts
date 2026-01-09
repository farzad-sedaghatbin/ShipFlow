/**
 * Form validation utilities with user-friendly error messages
 */

export interface ValidationRule {
  validate: (value: unknown) => boolean;
  message: string;
}

export interface FieldValidation {
  rules: ValidationRule[];
  value: unknown;
}

// Common validation rules
export const required = (fieldName: string): ValidationRule => ({
  validate: (value) => {
    if (typeof value === 'string') return value.trim().length > 0;
    if (typeof value === 'number') return !isNaN(value);
    return value !== null && value !== undefined;
  },
  message: `${fieldName} is required`,
});

export const minLength = (fieldName: string, min: number): ValidationRule => ({
  validate: (value) => typeof value === 'string' && value.trim().length >= min,
  message: `${fieldName} must be at least ${min} characters`,
});

export const maxLength = (fieldName: string, max: number): ValidationRule => ({
  validate: (value) => typeof value === 'string' && value.trim().length <= max,
  message: `${fieldName} must be no more than ${max} characters`,
});

export const email = (): ValidationRule => ({
  validate: (value) => {
    if (typeof value !== 'string' || !value.trim()) return true; // Let required handle empty
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(value.trim());
  },
  message: 'Please enter a valid email address',
});

export const url = (): ValidationRule => ({
  validate: (value) => {
    if (typeof value !== 'string' || !value.trim()) return true; // Let required handle empty
    try {
      new URL(value);
      return true;
    } catch {
      return false;
    }
  },
  message: 'Please enter a valid URL',
});

export const pattern = (fieldName: string, regex: RegExp, description: string): ValidationRule => ({
  validate: (value) => {
    if (typeof value !== 'string' || !value.trim()) return true;
    return regex.test(value.trim());
  },
  message: `${fieldName} ${description}`,
});

export const min = (fieldName: string, minValue: number): ValidationRule => ({
  validate: (value) => {
    if (value === null || value === undefined || value === '') return true;
    const num = typeof value === 'string' ? parseFloat(value) : value;
    return typeof num === 'number' && !isNaN(num) && num >= minValue;
  },
  message: `${fieldName} must be at least ${minValue}`,
});

export const max = (fieldName: string, maxValue: number): ValidationRule => ({
  validate: (value) => {
    if (value === null || value === undefined || value === '') return true;
    const num = typeof value === 'string' ? parseFloat(value) : value;
    return typeof num === 'number' && !isNaN(num) && num <= maxValue;
  },
  message: `${fieldName} must be no more than ${maxValue}`,
});

export const positiveNumber = (fieldName: string): ValidationRule => ({
  validate: (value) => {
    if (value === null || value === undefined || value === '') return true;
    const num = typeof value === 'string' ? parseFloat(value) : value;
    return typeof num === 'number' && !isNaN(num) && num > 0;
  },
  message: `${fieldName} must be a positive number`,
});

/**
 * Safely parse a route parameter string to a number.
 * Returns null if the value is undefined, null, empty, or cannot be parsed to a valid number.
 * This prevents "NaN" from being sent to the backend when route parameters are missing or invalid.
 * 
 * @param value - The string value from route params (can be undefined)
 * @returns The parsed number or null if invalid
 * 
 * @example
 * const { id } = useParams<{ id: string }>();
 * const numericId = safeParseId(id);
 * if (!numericId) {
 *   // Handle invalid/missing ID
 *   return;
 * }
 */
export const safeParseId = (value: string | undefined | null): number | null => {
  if (!value || value.trim() === '') {
    return null;
  }
  const parsed = parseInt(value, 10);
  return isNaN(parsed) ? null : parsed;
};

export const dateNotInFuture = (fieldName: string): ValidationRule => ({
  validate: (value) => {
    if (!value) return true;
    const date = typeof value === 'string' ? new Date(value) : value as Date;
    return date <= new Date();
  },
  message: `${fieldName} cannot be in the future`,
});

export const dateAfter = (fieldName: string, afterDate: Date | string, afterFieldName: string): ValidationRule => ({
  validate: (value) => {
    if (!value || !afterDate) return true;
    const date = typeof value === 'string' ? new Date(value) : value as Date;
    const compareDate = typeof afterDate === 'string' ? new Date(afterDate) : afterDate;
    return date > compareDate;
  },
  message: `${fieldName} must be after ${afterFieldName}`,
});

export const projectKey = (): ValidationRule => ({
  validate: (value) => {
    if (typeof value !== 'string' || !value.trim()) return true;
    const keyRegex = /^[A-Z0-9]{2,10}$/;
    return keyRegex.test(value.trim());
  },
  message: 'Project key must be 2-10 uppercase letters or numbers',
});

/**
 * Validate a single field
 */
export function validateField(value: unknown, rules: ValidationRule[]): string | null {
  for (const rule of rules) {
    if (!rule.validate(value)) {
      return rule.message;
    }
  }
  return null;
}

/**
 * Validate multiple fields
 * Returns an object with field names as keys and error messages as values
 */
export function validateForm(
  fields: Record<string, FieldValidation>
): Record<string, string> {
  const errors: Record<string, string> = {};

  for (const [fieldName, { value, rules }] of Object.entries(fields)) {
    const error = validateField(value, rules);
    if (error) {
      errors[fieldName] = error;
    }
  }

  return errors;
}

/**
 * Check if form has any errors
 */
export function hasErrors(errors: Record<string, string>): boolean {
  return Object.keys(errors).length > 0;
}

/**
 * Form validation hook helper - returns field props for TextField
 */
export function getFieldProps(
  fieldName: string,
  errors: Record<string, string>,
  touched: Record<string, boolean>
) {
  const showError = touched[fieldName] && !!errors[fieldName];
  return {
    error: showError,
    helperText: showError ? errors[fieldName] : undefined,
  };
}
