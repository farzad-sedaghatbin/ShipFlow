import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';

interface ProgressBarProps {
  value: number;
  label?: string;
  showPercentage?: boolean;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
}

export default function ProgressBar({ value, label, showPercentage = true, color = 'primary' }: ProgressBarProps) {
  const normalizedValue = Math.min(Math.max(value, 0), 100);
  const isOverBudget = value > 100;
  const progressId = label ? `progress-${label.replace(/\s+/g, '-').toLowerCase()}` : undefined;

  const colorClasses = {
    primary: '[&>div]:bg-primary',
    secondary: '[&>div]:bg-violet-500',
    error: '[&>div]:bg-destructive',
    info: '[&>div]:bg-blue-500',
    success: '[&>div]:bg-emerald-500',
    warning: '[&>div]:bg-amber-500',
  };

  return (
    <div className="w-full">
      {label && (
        <div className="flex justify-between mb-1">
          <span 
            className="text-sm text-foreground"
            id={progressId}
          >
            {label}
          </span>
          {showPercentage && (
            <span 
              className="text-sm text-foreground"
              aria-hidden="true"
            >
              {value.toFixed(0)}%
            </span>
          )}
        </div>
      )}
      <Progress
        value={normalizedValue}
        className={cn(
          'h-2',
          isOverBudget ? '[&>div]:bg-destructive' : colorClasses[color]
        )}
        aria-label={label ? `${label}: ${value.toFixed(0)}% complete` : `${value.toFixed(0)}% complete`}
        aria-valuenow={normalizedValue}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-labelledby={progressId}
      />
    </div>
  );
}
