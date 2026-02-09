import { useState, useEffect, useCallback } from 'react';

/**
 * Hook to detect if a media query matches.
 * Useful for responsive design logic in JavaScript.
 * 
 * @param query - CSS media query string (e.g., '(max-width: 768px)')
 * @returns true if the media query matches, false otherwise
 * 
 * @example
 * ```tsx
 * const isMobile = useMediaQuery('(max-width: 768px)');
 * const isTablet = useMediaQuery('(min-width: 769px) and (max-width: 1024px)');
 * const prefersDark = useMediaQuery('(prefers-color-scheme: dark)');
 * ```
 */
export function useMediaQuery(query: string): boolean {
  const getMatches = useCallback((query: string): boolean => {
    // Check if window is defined (for SSR compatibility)
    if (typeof window === 'undefined') {
      return false;
    }
    return window.matchMedia(query).matches;
  }, []);

  const [matches, setMatches] = useState<boolean>(() => getMatches(query));

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const mediaQuery = window.matchMedia(query);
    
    // Set initial value
    setMatches(mediaQuery.matches);

    const handleChange = (event: MediaQueryListEvent) => {
      setMatches(event.matches);
    };

    // Prefer modern event listener API when available
    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', handleChange);

      return () => {
        mediaQuery.removeEventListener('change', handleChange);
      };
    }

    // Fallback for older browsers (e.g., older Safari)
    if (typeof mediaQuery.addListener === 'function') {
      mediaQuery.addListener(handleChange);

      return () => {
        mediaQuery.removeListener(handleChange);
      };
    }

    // If neither API is available, provide a no-op cleanup
    return () => {};
  }, [query, getMatches]);

  return matches;
}

export default useMediaQuery;
