import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import NProgress from 'nprogress';
import '@/styles/nprogress.css';

// Configure NProgress
NProgress.configure({
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.1,
});

/**
 * Hook to show NProgress loading bar on route transitions.
 * Call this once at the top level of your app inside a Router context.
 */
export function useRouteProgress() {
  const location = useLocation();
  
  useEffect(() => {
    // Start progress on location change
    NProgress.start();
    
    // Complete progress after a short delay (simulates page load)
    const timer = setTimeout(() => {
      NProgress.done();
    }, 200);
    
    return () => {
      clearTimeout(timer);
      NProgress.done();
    };
  }, [location.pathname]);
}

/**
 * Component wrapper that adds NProgress to route transitions.
 * Must be used inside a Router context.
 */
export function RouteProgressProvider({ children }: { children: React.ReactNode }) {
  useRouteProgress();
  return <>{children}</>;
}

/**
 * Hook for manual progress control.
 * Useful for async operations outside of routing.
 */
export function useProgress() {
  return {
    start: () => NProgress.start(),
    done: () => NProgress.done(),
    set: (n: number) => NProgress.set(n),
    inc: (amount?: number) => NProgress.inc(amount),
  };
}

export default RouteProgressProvider;
