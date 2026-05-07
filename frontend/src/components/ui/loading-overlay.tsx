import * as React from 'react';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

interface LoadingOverlayProps {
  /** Whether the overlay is visible */
  isLoading: boolean;
  /** Optional text to display below the spinner */
  text?: string;
  /** Size of the spinner */
  size?: 'sm' | 'md' | 'lg';
  /** Whether to use a blur effect on the background */
  blur?: boolean;
  /** Children to render behind the overlay */
  children: React.ReactNode;
  /** Additional class name for the container */
  className?: string;
}

const sizeClasses = {
  sm: 'h-4 w-4',
  md: 'h-6 w-6',
  lg: 'h-8 w-8',
};

/**
 * Loading overlay component that displays a semi-transparent overlay
 * with a spinner over its children when loading.
 * 
 * Use this to prevent double-clicks on async operations and provide
 * visual feedback that an action is in progress.
 */
export function LoadingOverlay({
  isLoading,
  text,
  size = 'md',
  blur = false,
  children,
  className,
}: LoadingOverlayProps) {
  return (
    <div className={cn('relative', className)}>
      {children}
      {isLoading && (
        <div
          className={cn(
            'absolute inset-0 z-50 flex flex-col items-center justify-center',
            'bg-background/80',
            blur && 'backdrop-blur-sm',
            'transition-opacity duration-200'
          )}
          aria-live="polite"
          aria-busy="true"
        >
          <Loader2 className={cn('animate-spin text-primary', sizeClasses[size])} />
          {text && (
            <p className="mt-2 text-sm text-muted-foreground">{text}</p>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * Full-screen loading overlay for page-level loading states
 */
export function FullScreenLoadingOverlay({
  isLoading,
  text,
}: {
  isLoading: boolean;
  text?: string;
}) {
  if (!isLoading) return null;

  return (
    <div
      className={cn(
        'fixed inset-0 z-[100] flex flex-col items-center justify-center',
        'bg-background/80 backdrop-blur-sm',
        'transition-opacity duration-200'
      )}
      aria-live="polite"
      aria-busy="true"
    >
      <Loader2 className="h-10 w-10 animate-spin text-primary" />
      {text && (
        <p className="mt-3 text-base text-muted-foreground">{text}</p>
      )}
    </div>
  );
}

export default LoadingOverlay;
