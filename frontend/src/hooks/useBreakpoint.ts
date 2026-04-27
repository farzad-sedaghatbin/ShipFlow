import { useState, useEffect, useMemo } from 'react';

/**
 * Tailwind CSS breakpoint values (in pixels)
 * Matches the default Tailwind breakpoints
 */
export const BREAKPOINTS = {
  xs: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  '2xl': 1536,
} as const;

export type Breakpoint = keyof typeof BREAKPOINTS;

/**
 * Hook to detect the current Tailwind CSS breakpoint.
 * Returns the largest breakpoint that matches the current window width.
 * 
 * @returns Current breakpoint identifier ('xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl')
 * 
 * @example
 * ```tsx
 * const breakpoint = useBreakpoint();
 * const isMobile = breakpoint === 'xs' || breakpoint === 'sm';
 * const isDesktop = breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === '2xl';
 * ```
 */
export function useBreakpoint(): Breakpoint {
  const getBreakpoint = (): Breakpoint => {
    if (typeof window === 'undefined') {
      return 'md'; // Default for SSR
    }
    
    const width = window.innerWidth;
    
    if (width >= BREAKPOINTS['2xl']) return '2xl';
    if (width >= BREAKPOINTS.xl) return 'xl';
    if (width >= BREAKPOINTS.lg) return 'lg';
    if (width >= BREAKPOINTS.md) return 'md';
    if (width >= BREAKPOINTS.sm) return 'sm';
    return 'xs';
  };

  const [breakpoint, setBreakpoint] = useState<Breakpoint>(getBreakpoint);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const handleResize = () => {
      const newBreakpoint = getBreakpoint();
      setBreakpoint(current => current !== newBreakpoint ? newBreakpoint : current);
    };

    // Listen to window resize events, since breakpoint is based on viewport width
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return breakpoint;
}

/**
 * Hook that returns boolean helpers for common breakpoint checks.
 * 
 * @returns Object with boolean flags for common responsive checks
 * 
 * @example
 * ```tsx
 * const { isMobile, isTablet, isDesktop, isSmallScreen } = useBreakpointHelpers();
 * 
 * if (isMobile) {
 *   return <MobileView />;
 * }
 * ```
 */
export function useBreakpointHelpers() {
  const breakpoint = useBreakpoint();
  
  return useMemo(() => ({
    /** true for xs and sm breakpoints (< 768px) */
    isMobile: breakpoint === 'xs' || breakpoint === 'sm',
    /** true for md breakpoint (768px - 1023px) */
    isTablet: breakpoint === 'md',
    /** true for lg, xl, and 2xl breakpoints (>= 1024px) */
    isDesktop: breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === '2xl',
    /** true for xs, sm, and md breakpoints (< 1024px) */
    isSmallScreen: breakpoint === 'xs' || breakpoint === 'sm' || breakpoint === 'md',
    /** true for lg and above (>= 1024px) */
    isLargeScreen: breakpoint === 'lg' || breakpoint === 'xl' || breakpoint === '2xl',
    /** Current breakpoint identifier */
    breakpoint,
  }), [breakpoint]);
}

export default useBreakpoint;
