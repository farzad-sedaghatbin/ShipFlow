import { useState, useCallback, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Filter value types supported by the hook
 */
type FilterValue = string | number | boolean | string[] | number[] | null | undefined;

/**
 * Configuration for a single filter field
 */
interface FilterConfig<T> {
  /** Default value when not present in URL */
  defaultValue: T;
  /** Parse URL string to filter value */
  parse?: (value: string | null) => T;
  /** Serialize filter value to URL string (return null to remove from URL) */
  serialize?: (value: T) => string | null;
}

/**
 * Filter state configuration object
 */
type FilterConfigs<T extends Record<string, FilterValue>> = {
  [K in keyof T]: FilterConfig<T[K]>;
};

/**
 * Return type for the useFilterState hook
 */
interface UseFilterStateReturn<T extends Record<string, FilterValue>> {
  /** Current filter values */
  filters: T;
  /** Update a single filter value */
  setFilter: <K extends keyof T>(key: K, value: T[K]) => void;
  /** Update multiple filter values at once */
  setFilters: (updates: Partial<T>) => void;
  /** Reset all filters to default values */
  resetFilters: () => void;
  /** Reset a single filter to its default value */
  resetFilter: <K extends keyof T>(key: K) => void;
  /** Check if any filters are active (different from defaults) */
  hasActiveFilters: boolean;
  /** Get count of active filters */
  activeFilterCount: number;
}

/**
 * Default parsers for common filter types
 */
const defaultParsers = {
  string: (value: string | null): string => value ?? '',
  number: (value: string | null): number | undefined => 
    value ? parseInt(value, 10) : undefined,
  boolean: (value: string | null): boolean => value === 'true',
  stringArray: (value: string | null): string[] => 
    value ? value.split(',').filter(Boolean) : [],
  numberArray: (value: string | null): number[] => 
    value ? value.split(',').filter(Boolean).map(Number) : [],
};

/**
 * Default serializers for common filter types
 */
const defaultSerializers = {
  string: (value: string): string | null => value || null,
  number: (value: number | undefined): string | null => 
    value !== undefined ? String(value) : null,
  boolean: (value: boolean): string | null => value ? 'true' : null,
  stringArray: (value: string[]): string | null => 
    value.length > 0 ? value.join(',') : null,
  numberArray: (value: number[]): string | null => 
    value.length > 0 ? value.join(',') : null,
};

/**
 * Auto-detect parser based on default value type
 */
function getDefaultParser<T>(defaultValue: T): (value: string | null) => T {
  if (Array.isArray(defaultValue)) {
    if (defaultValue.length === 0 || typeof defaultValue[0] === 'string') {
      return defaultParsers.stringArray as (value: string | null) => T;
    }
    return defaultParsers.numberArray as (value: string | null) => T;
  }
  
  switch (typeof defaultValue) {
    case 'string':
      return defaultParsers.string as (value: string | null) => T;
    case 'number':
      return defaultParsers.number as (value: string | null) => T;
    case 'boolean':
      return defaultParsers.boolean as (value: string | null) => T;
    default:
      return (value: string | null) => (value ?? defaultValue) as T;
  }
}

/**
 * Auto-detect serializer based on default value type
 */
function getDefaultSerializer<T>(defaultValue: T): (value: T) => string | null {
  if (Array.isArray(defaultValue)) {
    if (defaultValue.length === 0 || typeof defaultValue[0] === 'string') {
      return defaultSerializers.stringArray as (value: T) => string | null;
    }
    return defaultSerializers.numberArray as (value: T) => string | null;
  }
  
  switch (typeof defaultValue) {
    case 'string':
      return defaultSerializers.string as (value: T) => string | null;
    case 'number':
      return defaultSerializers.number as (value: T) => string | null;
    case 'boolean':
      return defaultSerializers.boolean as (value: T) => string | null;
    default:
      return (value: T) => value != null ? String(value) : null;
  }
}

/**
 * A hook for managing filter state with URL persistence.
 * Filters are synchronized with URL search params for shareability and browser history support.
 * 
 * @example
 * ```tsx
 * const { filters, setFilter, resetFilters, hasActiveFilters } = useFilterState({
 *   search: { defaultValue: '' },
 *   status: { defaultValue: [] as string[] },
 *   cycleId: { defaultValue: undefined as number | undefined },
 *   showCompleted: { defaultValue: false },
 * });
 * 
 * // Use filters
 * <Input value={filters.search} onChange={(e) => setFilter('search', e.target.value)} />
 * 
 * // Check for active filters
 * {hasActiveFilters && <Button onClick={resetFilters}>Clear Filters</Button>}
 * ```
 */
export function useFilterState<T extends Record<string, FilterValue>>(
  configs: FilterConfigs<T>
): UseFilterStateReturn<T> {
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Initialize filters from URL or defaults
  const parseFilters = useCallback((): T => {
    const result = {} as T;
    
    for (const [key, config] of Object.entries(configs) as [keyof T, FilterConfig<T[keyof T]>][]) {
      const urlValue = searchParams.get(key as string);
      const parse = config.parse ?? getDefaultParser(config.defaultValue);
      result[key] = parse(urlValue);
    }
    
    return result;
  }, [searchParams, configs]);
  
  const [filters, setFiltersState] = useState<T>(parseFilters);
  
  // Sync filters when URL changes (e.g., browser back/forward)
  useEffect(() => {
    setFiltersState(parseFilters());
  }, [searchParams, parseFilters]);
  
  // Update URL when filters change
  const updateUrl = useCallback((newFilters: T) => {
    const newParams = new URLSearchParams(searchParams);
    
    for (const [key, config] of Object.entries(configs) as [keyof T, FilterConfig<T[keyof T]>][]) {
      const value = newFilters[key];
      const serialize = config.serialize ?? getDefaultSerializer(config.defaultValue);
      const serialized = serialize(value);
      
      if (serialized !== null) {
        newParams.set(key as string, serialized);
      } else {
        newParams.delete(key as string);
      }
    }
    
    setSearchParams(newParams, { replace: true });
  }, [configs, searchParams, setSearchParams]);
  
  // Set a single filter
  const setFilter = useCallback(<K extends keyof T>(key: K, value: T[K]) => {
    const newFilters = { ...filters, [key]: value };
    setFiltersState(newFilters);
    updateUrl(newFilters);
  }, [filters, updateUrl]);
  
  // Set multiple filters at once
  const setFilters = useCallback((updates: Partial<T>) => {
    const newFilters = { ...filters, ...updates };
    setFiltersState(newFilters);
    updateUrl(newFilters);
  }, [filters, updateUrl]);
  
  // Get default values
  const defaults = useMemo(() => {
    const result = {} as T;
    for (const [key, config] of Object.entries(configs) as [keyof T, FilterConfig<T[keyof T]>][]) {
      result[key] = config.defaultValue;
    }
    return result;
  }, [configs]);
  
  // Reset all filters
  const resetFilters = useCallback(() => {
    setFiltersState(defaults);
    updateUrl(defaults);
  }, [defaults, updateUrl]);
  
  // Reset a single filter
  const resetFilter = useCallback(<K extends keyof T>(key: K) => {
    const newFilters = { ...filters, [key]: configs[key].defaultValue };
    setFiltersState(newFilters);
    updateUrl(newFilters);
  }, [filters, configs, updateUrl]);
  
  // Check if filters are active
  const hasActiveFilters = useMemo(() => {
    for (const key of Object.keys(configs) as (keyof T)[]) {
      const current = filters[key];
      const defaultVal = configs[key].defaultValue;
      
      if (Array.isArray(current) && Array.isArray(defaultVal)) {
        if (current.length !== defaultVal.length) return true;
        if (!current.every((v, i) => v === defaultVal[i])) return true;
      } else if (current !== defaultVal) {
        return true;
      }
    }
    return false;
  }, [filters, configs]);
  
  // Count active filters
  const activeFilterCount = useMemo(() => {
    let count = 0;
    for (const key of Object.keys(configs) as (keyof T)[]) {
      const current = filters[key];
      const defaultVal = configs[key].defaultValue;
      
      if (Array.isArray(current)) {
        if (current.length > 0) count++;
      } else if (current !== defaultVal && current !== '' && current !== undefined) {
        count++;
      }
    }
    return count;
  }, [filters, configs]);
  
  return {
    filters,
    setFilter,
    setFilters,
    resetFilters,
    resetFilter,
    hasActiveFilters,
    activeFilterCount,
  };
}

/**
 * Convenience function to create filter configs with proper typing
 */
export function createFilterConfig<T extends FilterValue>(
  defaultValue: T,
  options?: Partial<FilterConfig<T>>
): FilterConfig<T> {
  return {
    defaultValue,
    ...options,
  };
}

export default useFilterState;
