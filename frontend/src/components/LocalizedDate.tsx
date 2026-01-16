/**
 * LocalizedDate Component
 * Displays dates in the user's locale format (Jalali for Persian, Gregorian for others)
 */

import { formatDate, formatDateShort, formatDateLong, formatDateTime, formatRelativeTime, useLocale } from '@/lib/locale';
import { cn } from '@/lib/utils';

interface LocalizedDateProps {
  date: Date | string | number;
  format?: 'short' | 'long' | 'datetime' | 'relative' | string;
  className?: string;
  showTooltip?: boolean;
}

/**
 * A date component that automatically formats dates based on current locale
 * - For Persian (fa): Uses Jalali calendar with Persian digits
 * - For English/Spanish: Uses Gregorian calendar
 */
export function LocalizedDate({
  date,
  format = 'short',
  className,
  showTooltip = true,
}: LocalizedDateProps) {
  const locale = useLocale();
  
  const getFormattedDate = (): string => {
    switch (format) {
      case 'short':
        return formatDateShort(date);
      case 'long':
        return formatDateLong(date);
      case 'datetime':
        return formatDateTime(date);
      case 'relative':
        return formatRelativeTime(date);
      default:
        // Custom format string
        return formatDate(date, format);
    }
  };

  const formattedDate = getFormattedDate();
  
  // For tooltip, show the full date in locale format
  const tooltipText = format !== 'long' ? formatDateLong(date) : undefined;

  return (
    <time
      dateTime={new Date(date).toISOString()}
      className={cn('whitespace-nowrap', className)}
      title={showTooltip ? tooltipText : undefined}
      dir={locale.dir}
    >
      {formattedDate}
    </time>
  );
}

/**
 * A date range component
 */
interface LocalizedDateRangeProps {
  startDate: Date | string | number;
  endDate: Date | string | number;
  className?: string;
}

export function LocalizedDateRange({
  startDate,
  endDate,
  className,
}: LocalizedDateRangeProps) {
  const locale = useLocale();
  
  return (
    <span className={cn('whitespace-nowrap', className)} dir={locale.dir}>
      <LocalizedDate date={startDate} format="short" showTooltip={false} />
      <span className="mx-1">-</span>
      <LocalizedDate date={endDate} format="short" showTooltip={false} />
    </span>
  );
}

export default LocalizedDate;
