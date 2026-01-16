/**
 * Locale utilities for internationalization
 * Handles RTL languages, Persian calendar (Jalali), and number formatting
 * 
 * ARCHITECTURE:
 * - Backend: Stores all dates in UTC, returns ISO 8601 format (e.g., "2026-01-16T12:30:00.000Z")
 * - Frontend: Parses UTC dates and converts to user's timezone and calendar system
 * - Persian locale: Uses Jalali calendar for display, but stores/sends Gregorian UTC to backend
 */

import { format as formatGregorian, formatDistance as formatDistanceGregorian, parseISO, type Locale } from 'date-fns';
import { format as formatJalali, formatDistance as formatDistanceJalali } from 'date-fns-jalali';
import { enUS, faIR, es } from 'date-fns/locale';
import { toZonedTime, formatInTimeZone } from 'date-fns-tz';
import i18n from '@/i18n';

// ============================================
// LANGUAGE CONFIGURATION
// ============================================

export type SupportedLocale = 'en' | 'fa' | 'es';

export interface LocaleConfig {
  code: SupportedLocale;
  name: string;
  nativeName: string;
  dir: 'ltr' | 'rtl';
  calendar: 'gregorian' | 'jalali';
  dateLocale: Locale;
  numberSystem: 'latn' | 'arabext'; // Latin or Extended Arabic-Indic numerals
  weekStartsOn: 0 | 1 | 6; // Sunday=0, Monday=1, Saturday=6
}

export const LOCALE_CONFIGS: Record<SupportedLocale, LocaleConfig> = {
  en: {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    dir: 'ltr',
    calendar: 'gregorian',
    dateLocale: enUS,
    numberSystem: 'latn',
    weekStartsOn: 0,
  },
  fa: {
    code: 'fa',
    name: 'Persian',
    nativeName: 'فارسی',
    dir: 'rtl',
    calendar: 'jalali',
    dateLocale: faIR,
    numberSystem: 'arabext',
    weekStartsOn: 6, // Week starts on Saturday in Iran
  },
  es: {
    code: 'es',
    name: 'Spanish',
    nativeName: 'Español',
    dir: 'ltr',
    calendar: 'gregorian',
    dateLocale: es,
    numberSystem: 'latn',
    weekStartsOn: 1, // Week starts on Monday in Spain
  },
};

// ============================================
// TIMEZONE HELPERS
// ============================================

/**
 * Get the user's browser timezone
 * Examples: "America/New_York", "Asia/Tehran", "Europe/Madrid"
 */
export function getUserTimezone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * Convert a UTC date (from backend) to user's local timezone
 * Backend always sends dates in UTC ISO format: "2026-01-16T12:30:00.000Z"
 */
export function toUserTimezone(utcDate: Date | string | number): Date {
  const date = typeof utcDate === 'string' ? parseISO(utcDate) : new Date(utcDate);
  return toZonedTime(date, getUserTimezone());
}

/**
 * Format date in user's timezone
 */
export function formatInUserTimezone(
  date: Date | string | number,
  formatStr: string
): string {
  const locale = getCurrentLocale();
  return formatInTimeZone(date, getUserTimezone(), formatStr, {
    locale: locale.dateLocale,
  });
}

// ============================================
// CURRENT LOCALE HELPERS
// ============================================

/**
 * Get the current locale configuration
 */
export function getCurrentLocale(): LocaleConfig {
  const lang = i18n.language as SupportedLocale;
  return LOCALE_CONFIGS[lang] || LOCALE_CONFIGS.en;
}

/**
 * Check if current locale is RTL
 */
export function isRTL(): boolean {
  return getCurrentLocale().dir === 'rtl';
}

/**
 * Check if current locale uses Jalali calendar
 */
export function usesJalaliCalendar(): boolean {
  return getCurrentLocale().calendar === 'jalali';
}

// ============================================
// DATE FORMATTING
// ============================================

/**
 * Format a date according to current locale and timezone
 * Automatically uses Jalali calendar for Persian locale
 * Converts UTC dates from backend to user's local timezone
 * 
 * @param date - UTC date from backend (ISO string) or Date object
 * @param formatStr - date-fns format string (default: 'PPP')
 */
export function formatDate(
  date: Date | string | number,
  formatStr: string = 'PPP'
): string {
  const locale = getCurrentLocale();
  // Parse ISO string from backend (UTC) and convert to user's timezone
  const dateObj = typeof date === 'string' ? parseISO(date) : new Date(date);
  const localDate = toUserTimezone(dateObj);
  
  if (locale.calendar === 'jalali') {
    return formatJalali(localDate, formatStr, { locale: locale.dateLocale });
  }
  
  return formatGregorian(localDate, formatStr, { locale: locale.dateLocale });
}

/**
 * Format a date in short format (e.g., "Jan 15, 2026" or "۲۵ دی ۱۴۰۴")
 */
export function formatDateShort(date: Date | string | number): string {
  return formatDate(date, 'PP');
}

/**
 * Format a date in long format (e.g., "January 15, 2026" or "۲۵ دی ۱۴۰۴")
 */
export function formatDateLong(date: Date | string | number): string {
  return formatDate(date, 'PPP');
}

/**
 * Format a date with time
 */
export function formatDateTime(date: Date | string | number): string {
  return formatDate(date, 'PPp');
}

/**
 * Format just the time
 */
export function formatTime(date: Date | string | number): string {
  return formatDate(date, 'p');
}

/**
 * Format a date range
 */
export function formatDateRange(
  startDate: Date | string | number,
  endDate: Date | string | number
): string {
  return `${formatDateShort(startDate)} - ${formatDateShort(endDate)}`;
}

/**
 * Format relative time (e.g., "2 days ago" or "۲ روز پیش")
 * Handles UTC dates from backend
 */
export function formatRelativeTime(
  date: Date | string | number,
  baseDate: Date = new Date()
): string {
  const locale = getCurrentLocale();
  const dateObj = typeof date === 'string' ? parseISO(date) : new Date(date);
  const localDate = toUserTimezone(dateObj);
  
  if (locale.calendar === 'jalali') {
    return formatDistanceJalali(localDate, baseDate, {
      addSuffix: true,
      locale: locale.dateLocale,
    });
  }
  
  return formatDistanceGregorian(localDate, baseDate, {
    addSuffix: true,
    locale: locale.dateLocale,
  });
}

// ============================================
// NUMBER FORMATTING
// ============================================

/**
 * Persian digits mapping
 */
const PERSIAN_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];

/**
 * Convert number to Persian digits if needed
 */
export function toPersianDigits(num: number | string): string {
  return String(num).replace(/\d/g, (d) => PERSIAN_DIGITS[parseInt(d)]);
}

/**
 * Format a number according to current locale
 */
export function formatNumber(
  num: number,
  options?: Intl.NumberFormatOptions
): string {
  const locale = getCurrentLocale();
  
  // Use Intl.NumberFormat for proper locale-aware formatting
  const formatted = new Intl.NumberFormat(locale.code, {
    ...options,
    // Use locale's number system
    numberingSystem: locale.numberSystem,
  }).format(num);
  
  return formatted;
}

/**
 * Format a percentage
 */
export function formatPercent(num: number, decimals: number = 0): string {
  return formatNumber(num / 100, {
    style: 'percent',
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

/**
 * Format currency (defaults to USD, but can be customized)
 */
export function formatCurrency(
  amount: number,
  currency: string = 'USD'
): string {
  const locale = getCurrentLocale();
  
  // Use IRR for Persian locale by default
  const currencyCode = locale.code === 'fa' && currency === 'USD' ? 'IRR' : currency;
  
  return new Intl.NumberFormat(locale.code, {
    style: 'currency',
    currency: currencyCode,
    numberingSystem: locale.numberSystem,
  }).format(amount);
}

// ============================================
// RTL HELPERS
// ============================================

/**
 * Get CSS direction class
 */
export function getDirectionClass(): string {
  return isRTL() ? 'rtl' : 'ltr';
}

/**
 * Swap left/right values for RTL
 * Useful for inline styles that don't support logical properties
 */
export function rtlSwap<T>(ltrValue: T, rtlValue: T): T {
  return isRTL() ? rtlValue : ltrValue;
}

/**
 * Get logical margin/padding direction
 * Returns 'start' or 'end' based on 'left' or 'right'
 */
export function logicalDirection(direction: 'left' | 'right'): 'start' | 'end' {
  if (isRTL()) {
    return direction === 'left' ? 'end' : 'start';
  }
  return direction === 'left' ? 'start' : 'end';
}

// ============================================
// CALENDAR UTILITIES
// ============================================

/**
 * Get day names for current locale
 */
export function getDayNames(format: 'long' | 'short' | 'narrow' = 'short'): string[] {
  const locale = getCurrentLocale();
  const formatter = new Intl.DateTimeFormat(locale.code, { weekday: format });
  
  // Generate day names starting from the locale's week start
  const days: string[] = [];
  const baseDate = new Date(2024, 0, 7); // A Sunday
  
  for (let i = 0; i < 7; i++) {
    const dayIndex = (locale.weekStartsOn + i) % 7;
    const date = new Date(baseDate);
    date.setDate(date.getDate() + dayIndex);
    days.push(formatter.format(date));
  }
  
  return days;
}

/**
 * Get month names for current locale (Jalali or Gregorian)
 */
export function getMonthNames(format: 'long' | 'short' = 'long'): string[] {
  const locale = getCurrentLocale();
  
  if (locale.calendar === 'jalali') {
    // Jalali month names
    const jalaliMonths = {
      long: [
        'فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
        'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'
      ],
      short: [
        'فرو', 'ارد', 'خرد', 'تیر', 'مرد', 'شهر',
        'مهر', 'آبا', 'آذر', 'دی', 'بهم', 'اسف'
      ],
    };
    return jalaliMonths[format];
  }
  
  // Gregorian months
  const formatter = new Intl.DateTimeFormat(locale.code, { month: format });
  return Array.from({ length: 12 }, (_, i) => {
    const date = new Date(2024, i, 1);
    return formatter.format(date);
  });
}

// ============================================
// REACT HOOK FOR LOCALE
// ============================================

import { useState, useEffect } from 'react';

/**
 * React hook to get current locale and react to language changes
 */
export function useLocale(): LocaleConfig {
  const [locale, setLocale] = useState<LocaleConfig>(getCurrentLocale());
  
  useEffect(() => {
    const handleLanguageChange = () => {
      setLocale(getCurrentLocale());
    };
    
    i18n.on('languageChanged', handleLanguageChange);
    
    return () => {
      i18n.off('languageChanged', handleLanguageChange);
    };
  }, []);
  
  return locale;
}

/**
 * React hook for formatted date that updates with locale changes
 */
export function useFormattedDate(
  date: Date | string | number | undefined,
  formatStr: string = 'PPP'
): string {
  // Subscribe to locale changes to trigger re-render
  useLocale();
  
  if (!date) return '';
  
  return formatDate(date, formatStr);
}
