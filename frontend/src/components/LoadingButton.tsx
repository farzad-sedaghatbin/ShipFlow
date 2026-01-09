import { Loader2 } from 'lucide-react';
import { Button, ButtonProps } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface LoadingButtonProps extends ButtonProps {
  loading?: boolean;
  loadingText?: string;
}

/**
 * Button component with built-in loading state
 * Shows spinner and optionally different text when loading
 */
export function LoadingButton({
  loading = false,
  loadingText,
  children,
  disabled,
  className,
  ...props
}: LoadingButtonProps) {
  return (
    <Button
      {...props}
      disabled={disabled || loading}
      aria-disabled={disabled || loading}
      aria-busy={loading}
      aria-live={loading ? 'polite' : undefined}
      className={cn(
        'focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2',
        className
      )}
    >
      {loading && (
        <Loader2 className="w-4 h-4 mr-2 animate-spin" aria-label="Loading" />
      )}
      {loading && loadingText ? loadingText : children}
    </Button>
  );
}

export default LoadingButton;
