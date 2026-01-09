import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { LucideIcon } from 'lucide-react';

export interface OnboardingStep {
  icon: string;
  title: string;
  description: string;
}

export interface EmptyStateProps {
  /**
   * Lucide icon component to display (legacy support)
   */
  icon?: LucideIcon;
  /**
   * Custom illustration component (preferred)
   */
  illustration?: React.ReactNode;
  /**
   * Primary heading text
   */
  title: string;
  /**
   * Secondary description text
   */
  description?: string;
  /**
   * Primary action button configuration
   */
  action?: {
    label: string;
    onClick?: () => void;
    href?: string;
    startIcon?: React.ReactNode;
  };
  /**
   * Secondary action button configuration
   */
  secondaryAction?: {
    label: string;
    onClick?: () => void;
    href?: string;
    startIcon?: React.ReactNode;
  };
  /**
   * Size variant for the empty state
   */
  size?: 'small' | 'medium' | 'large';
  /**
   * Whether to reduce padding (useful for nested cards)
   */
  compact?: boolean;
  /**
   * Onboarding steps to show as a guide
   */
  onboardingSteps?: OnboardingStep[];
  /**
   * Animation variant
   */
  animate?: boolean;
}

export default function EmptyState({
  icon: Icon,
  illustration,
  title,
  description,
  action,
  secondaryAction,
  size = 'medium',
  compact = false,
  onboardingSteps,
  animate = true,
}: EmptyStateProps) {
  const sizeConfig = {
    small: {
      iconSize: 'w-12 h-12',
      iconWrapper: 'w-18 h-18',
      titleClass: 'text-lg font-semibold',
      padding: compact ? 'py-3' : 'py-4',
      illustrationScale: 'scale-[0.8]',
    },
    medium: {
      iconSize: 'w-16 h-16',
      iconWrapper: 'w-24 h-24',
      titleClass: 'text-xl font-semibold',
      padding: compact ? 'py-4' : 'py-6',
      illustrationScale: 'scale-100',
    },
    large: {
      iconSize: 'w-20 h-20',
      iconWrapper: 'w-30 h-30',
      titleClass: 'text-2xl font-bold',
      padding: compact ? 'py-5' : 'py-8',
      illustrationScale: 'scale-[1.2]',
    },
  };

  const config = sizeConfig[size];

  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-center px-2',
        config.padding,
        animate && 'animate-in fade-in duration-500'
      )}
    >
      {/* Custom Illustration (preferred) */}
      {illustration && (
        <div className={cn('mb-3 origin-center', config.illustrationScale)}>
          {illustration}
        </div>
      )}

      {/* Legacy Icon Support */}
      {!illustration && Icon && (
        <div
          className={cn(
            'mb-2 flex items-center justify-center rounded-full',
            'bg-gradient-to-br from-primary/10 to-secondary/8',
            'border-2 border-dashed border-primary/20',
            config.iconWrapper
          )}
        >
          <Icon className={cn(config.iconSize, 'text-primary opacity-60')} />
        </div>
      )}

      <h3
        className={cn(
          config.titleClass,
          'bg-gradient-to-r from-foreground to-primary bg-clip-text text-transparent'
        )}
      >
        {title}
      </h3>

      {description && (
        <p
          className={cn(
            'text-muted-foreground text-sm leading-relaxed max-w-[450px]',
            (action || secondaryAction || onboardingSteps) ? 'mb-3' : ''
          )}
        >
          {description}
        </p>
      )}

      {/* Onboarding Steps */}
      {onboardingSteps && onboardingSteps.length > 0 && (
        <div className="flex flex-col sm:flex-row gap-3 mb-4 mt-2 w-full max-w-[700px] justify-center">
          {onboardingSteps.map((step, index) => (
            <div
              key={index}
              className={cn(
                'flex-1 flex flex-col items-center p-4 rounded-xl',
                'bg-card/60 border border-border/10',
                'transition-all duration-300 ease-out relative',
                'hover:-translate-y-1 hover:shadow-lg hover:shadow-primary/10 hover:border-primary/20',
                animate && 'animate-in fade-in slide-in-from-bottom-2',
              )}
              style={{ animationDelay: `${index * 100}ms` }}
            >
              {/* Step number badge */}
              <div
                className="absolute -top-2.5 left-4 w-6 h-6 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-xs font-bold shadow-md shadow-primary/40"
              >
                {index + 1}
              </div>

              <span className="text-2xl mb-2">{step.icon}</span>
              <h4 className="text-sm font-semibold text-foreground mb-1">
                {step.title}
              </h4>
              <p className="text-xs text-muted-foreground leading-relaxed">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      )}

      {(action || secondaryAction) && (
        <div className={cn('flex gap-2', onboardingSteps ? '' : 'mt-2')}>
          {action && (
            <Button
              onClick={action.onClick}
              asChild={!!action.href}
              size={size === 'small' ? 'default' : 'lg'}
              className="shadow-lg shadow-primary/35 hover:shadow-xl hover:shadow-primary/45 focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
            >
              {action.href ? (
                <a href={action.href}>
                  {action.startIcon}
                  {action.label}
                </a>
              ) : (
                <>
                  {action.startIcon}
                  {action.label}
                </>
              )}
            </Button>
          )}
          {secondaryAction && (
            <Button
              variant="outline"
              onClick={secondaryAction.onClick}
              asChild={!!secondaryAction.href}
              size={size === 'small' ? 'default' : 'lg'}
              className="focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
            >
              {secondaryAction.href ? (
                <a href={secondaryAction.href}>
                  {secondaryAction.startIcon}
                  {secondaryAction.label}
                </a>
              ) : (
                <>
                  {secondaryAction.startIcon}
                  {secondaryAction.label}
                </>
              )}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
