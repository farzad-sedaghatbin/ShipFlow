/**
 * Parse a date string and ensure it's treated as UTC if no timezone is specified.
 * This handles the case where the server sends dates without timezone info.
 */
export function parseDate(date: Date | string): Date {
  if (date instanceof Date) {
    return date;
  }
  
  // If the string doesn't have timezone info (no Z or +/-offset), assume it's UTC
  const dateStr = date.toString();
  if (dateStr && !dateStr.includes('Z') && !dateStr.match(/[+-]\d{2}:\d{2}$/)) {
    // Append 'Z' to treat as UTC
    return new Date(dateStr + 'Z');
  }
  
  return new Date(date);
}

/**
 * Format a date relative to now (e.g., "2 hours ago", "3 days ago")
 */
export function formatDistanceToNow(date: Date | string): string {
  const parsedDate = parseDate(date);
  const now = new Date();
  const diffMs = now.getTime() - parsedDate.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);
  const diffWeeks = Math.floor(diffDays / 7);
  const diffMonths = Math.floor(diffDays / 30);

  // Handle future dates (negative diff)
  if (diffSecs < 0) {
    return 'just now';
  }
  
  if (diffSecs < 60) {
    return 'just now';
  }
  if (diffMins < 60) {
    return `${diffMins}m ago`;
  }
  if (diffHours < 24) {
    return `${diffHours}h ago`;
  }
  if (diffDays < 7) {
    return `${diffDays}d ago`;
  }
  if (diffWeeks < 4) {
    return `${diffWeeks}w ago`;
  }
  if (diffMonths < 12) {
    return `${diffMonths}mo ago`;
  }
  return parsedDate.toLocaleDateString();
}

/**
 * Format a date for display (e.g., "Dec 30, 2025")
 */
export function formatDate(date: Date | string): string {
  const d = parseDate(date);
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Format a date with time (e.g., "Dec 30, 2025 at 3:45 PM")
 */
export function formatDateTime(date: Date | string): string {
  const d = parseDate(date);
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/**
 * Get the number of days remaining until a date
 */
export function getDaysRemaining(endDate: Date | string): number {
  const end = parseDate(endDate);
  const now = new Date();
  const diffMs = end.getTime() - now.getTime();
  return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
}

/**
 * Check if a date is today
 */
export function isToday(date: Date | string): boolean {
  const d = parseDate(date);
  const today = new Date();
  return (
    d.getDate() === today.getDate() &&
    d.getMonth() === today.getMonth() &&
    d.getFullYear() === today.getFullYear()
  );
}

/**
 * Check if a date is in the past
 */
export function isPast(date: Date | string): boolean {
  const d = parseDate(date);
  return d.getTime() < new Date().getTime();
}
