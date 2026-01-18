import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { format as formatGregorian } from 'date-fns';
import DatePicker from 'react-multi-date-picker';
import persian from 'react-date-object/calendars/persian';
import persian_fa from 'react-date-object/locales/persian_fa';
import 'react-multi-date-picker/styles/colors/green.css';
import { Input } from './ui/input';

interface LocalizedDateInputProps {
  id?: string;
  value: string; // ISO date string (YYYY-MM-DD)
  onChange: (value: string) => void; // Returns ISO date string
  min?: string; // ISO date string
  max?: string; // ISO date string
  disabled?: boolean;
  className?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
}

/**
 * LocalizedDateInput - A date input component that automatically shows:
 * - Visual Jalali calendar picker for Persian (fa) locale
 * - Gregorian calendar for other locales
 * 
 * The component handles conversion between Jalali and Gregorian dates internally.
 * Input/output is always in ISO format (YYYY-MM-DD) for consistency with the backend.
 */
export function LocalizedDateInput({
  id,
  value,
  onChange,
  min,
  max,
  disabled,
  className,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedby,
}: LocalizedDateInputProps) {
  const { i18n } = useTranslation();
  const isPersian = i18n.language === 'fa';
  
  const [internalValue, setInternalValue] = useState<any>(null);

  // Convert ISO date string to Date object for the picker
  useEffect(() => {
    if (value) {
      try {
        const date = new Date(value);
        if (!isNaN(date.getTime())) {
          setInternalValue(date);
        }
      } catch (error) {
        console.error('Invalid date:', value);
        setInternalValue(null);
      }
    } else {
      setInternalValue(null);
    }
  }, [value]);

  const handleDateChange = (dateObject: any) => {
    if (dateObject && dateObject.isValid) {
      // Convert to JavaScript Date
      const jsDate = dateObject.toDate();
      // Format as ISO string (YYYY-MM-DD)
      const isoString = formatGregorian(jsDate, 'yyyy-MM-dd');
      onChange(isoString);
    } else if (!dateObject) {
      onChange('');
    }
  };

  // For non-Persian, use native HTML5 date input
  if (!isPersian) {
    return (
      <Input
        id={id}
        type="date"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        min={min}
        max={max}
        disabled={disabled}
        className={className}
        aria-label={ariaLabel}
        aria-describedby={ariaDescribedby}
      />
    );
  }

  // For Persian, use react-multi-date-picker with Jalali calendar
  return (
    <DatePicker
      id={id}
      value={internalValue}
      onChange={handleDateChange}
      calendar={persian}
      locale={persian_fa}
      format="YYYY/MM/DD"
      disabled={disabled}
      className={`green ${className}`}
      containerClassName="w-full"
      inputClass="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
      aria-label={ariaLabel}
      aria-describedby={ariaDescribedby}
      style={{
        width: '100%',
      }}
    />
  );
}
