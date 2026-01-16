import { format as formatGregorian } from 'date-fns';
import { format as formatJalali } from 'date-fns-jalali';

/**
 * Format a date string or Date object according to the current locale
 * @param date - ISO date string, Date object, or null/undefined
 * @param locale - Current locale (e.g., 'fa', 'en')
 * @param formatPattern - Optional custom format pattern
 * @returns Formatted date string
 */
export function formatLocalizedDate(
  date: string | Date | null | undefined,
  locale: string,
  formatPattern?: string
): string {
  if (!date) return '';

  try {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return '';
    }

    const isPersian = locale === 'fa';
    
    if (isPersian) {
      // Default Jalali format: YYYY/MM/DD
      const pattern = formatPattern || 'yyyy/MM/dd';
      return formatJalali(dateObj, pattern);
    } else {
      // Default Gregorian format: MMM DD, YYYY (e.g., Jan 16, 2026)
      const pattern = formatPattern || 'MMM dd, yyyy';
      return formatGregorian(dateObj, pattern);
    }
  } catch (error) {
    console.error('Error formatting date:', error);
    return '';
  }
}

/**
 * Format a date-time string or Date object according to the current locale
 * @param dateTime - ISO date-time string, Date object, or null/undefined
 * @param locale - Current locale (e.g., 'fa', 'en')
 * @returns Formatted date-time string
 */
export function formatLocalizedDateTime(
  dateTime: string | Date | null | undefined,
  locale: string
): string {
  if (!dateTime) return '';

  try {
    const dateObj = typeof dateTime === 'string' ? new Date(dateTime) : dateTime;
    
    if (isNaN(dateObj.getTime())) {
      return '';
    }

    const isPersian = locale === 'fa';
    
    if (isPersian) {
      // Jalali format with time: YYYY/MM/DD HH:mm
      return formatJalali(dateObj, 'yyyy/MM/dd HH:mm');
    } else {
      // Gregorian format with time: MMM DD, YYYY HH:mm
      return formatGregorian(dateObj, 'MMM dd, yyyy HH:mm');
    }
  } catch (error) {
    console.error('Error formatting date-time:', error);
    return '';
  }
}

/**
 * Format a date for display in tables (short format)
 * @param date - ISO date string, Date object, or null/undefined
 * @param locale - Current locale
 * @returns Formatted date string (short format)
 */
export function formatLocalizedDateShort(
  date: string | Date | null | undefined,
  locale: string
): string {
  if (!date) return '';

  try {
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return '';
    }

    const isPersian = locale === 'fa';
    
    if (isPersian) {
      // Short Jalali format: YY/MM/DD
      return formatJalali(dateObj, 'yy/MM/dd');
    } else {
      // Short Gregorian format: MM/DD/YY
      return formatGregorian(dateObj, 'MM/dd/yy');
    }
  } catch (error) {
    console.error('Error formatting date:', error);
    return '';
  }
}
