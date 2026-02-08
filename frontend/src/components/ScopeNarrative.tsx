import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  AlertTriangle, 
  TrendingUp, 
  TrendingDown, 
  Minus, 
  Clock, 
  CheckCircle2,
  Mountain,
  Zap 
} from 'lucide-react';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import { Card, CardContent } from './ui/card';
import { Tooltip, TooltipContent, TooltipTrigger, TooltipProvider } from './ui/tooltip';
import { cn } from '../lib/utils';
import { HillChartPoint } from '../types';

interface ScopeNarrativeProps {
  point: HillChartPoint;
  /** Days since last movement */
  daysSinceUpdate?: number;
  /** Previous position for trend calculation */
  previousPosition?: number;
  /** Show detailed narrative text */
  showNarrative?: boolean;
  /** Compact mode for lists */
  compact?: boolean;
  onClick?: () => void;
}

type HealthStatus = 'healthy' | 'warning' | 'critical' | 'stalled';

interface NarrativeInfo {
  icon: typeof TrendingUp;
  iconColor: string;
  bgColor: string;
  borderColor: string;
  label: string;
  description: string;
  healthStatus: HealthStatus;
}

/**
 * Generate a narrative description for a scope's progress and health
 */
function generateNarrative(
  position: number,
  daysSinceUpdate: number,
  trend: 'up' | 'down' | 'stable',
  t: (key: string, options?: Record<string, unknown>) => string
): NarrativeInfo {
  const isUphill = position < 50;
  const isAtPeak = position >= 45 && position <= 55;
  const isComplete = position >= 95;
  
  // Stagnation detection
  const isStalled = daysSinceUpdate > 5;
  const isCriticallyStalled = daysSinceUpdate > 10;
  
  // Base cases
  if (isComplete) {
    return {
      icon: CheckCircle2,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50 dark:bg-green-950/30',
      borderColor: 'border-green-200 dark:border-green-800',
      label: t('scopeNarrative.complete'),
      description: t('scopeNarrative.completeDesc'),
      healthStatus: 'healthy',
    };
  }
  
  if (isCriticallyStalled) {
    return {
      icon: AlertTriangle,
      iconColor: 'text-red-600',
      bgColor: 'bg-red-50 dark:bg-red-950/30',
      borderColor: 'border-red-200 dark:border-red-800',
      label: t('scopeNarrative.criticallyStalled'),
      description: t('scopeNarrative.criticallyDesc', { days: daysSinceUpdate }),
      healthStatus: 'critical',
    };
  }
  
  if (isAtPeak && isStalled) {
    return {
      icon: Mountain,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50 dark:bg-amber-950/30',
      borderColor: 'border-amber-200 dark:border-amber-800',
      label: t('scopeNarrative.stuckAtPeak'),
      description: t('scopeNarrative.stuckAtPeakDesc'),
      healthStatus: 'warning',
    };
  }
  
  if (isStalled) {
    return {
      icon: Clock,
      iconColor: 'text-amber-600',
      bgColor: 'bg-amber-50 dark:bg-amber-950/30',
      borderColor: 'border-amber-200 dark:border-amber-800',
      label: t('scopeNarrative.stalled'),
      description: t('scopeNarrative.stalledDesc', { days: daysSinceUpdate }),
      healthStatus: 'stalled',
    };
  }
  
  // Active progress states
  if (isUphill) {
    if (trend === 'up') {
      return {
        icon: TrendingUp,
        iconColor: 'text-blue-600',
        bgColor: 'bg-blue-50 dark:bg-blue-950/30',
        borderColor: 'border-blue-200 dark:border-blue-800',
        label: t('scopeNarrative.figuringOut'),
        description: t('scopeNarrative.figuringOutDesc'),
        healthStatus: 'healthy',
      };
    }
    return {
      icon: Minus,
      iconColor: 'text-slate-600',
      bgColor: 'bg-slate-50 dark:bg-slate-950/30',
      borderColor: 'border-slate-200 dark:border-slate-700',
      label: t('scopeNarrative.exploring'),
      description: t('scopeNarrative.exploringDesc'),
      healthStatus: 'healthy',
    };
  }
  
  if (isAtPeak) {
    return {
      icon: Mountain,
      iconColor: 'text-purple-600',
      bgColor: 'bg-purple-50 dark:bg-purple-950/30',
      borderColor: 'border-purple-200 dark:border-purple-800',
      label: t('scopeNarrative.atPeak'),
      description: t('scopeNarrative.atPeakDesc'),
      healthStatus: 'healthy',
    };
  }
  
  // Downhill (executing)
  if (trend === 'up') {
    return {
      icon: Zap,
      iconColor: 'text-green-600',
      bgColor: 'bg-green-50 dark:bg-green-950/30',
      borderColor: 'border-green-200 dark:border-green-800',
      label: t('scopeNarrative.executing'),
      description: t('scopeNarrative.executingDesc'),
      healthStatus: 'healthy',
    };
  }
  
  return {
    icon: TrendingDown,
    iconColor: 'text-slate-600',
    bgColor: 'bg-slate-50 dark:bg-slate-950/30',
    borderColor: 'border-slate-200 dark:border-slate-700',
    label: t('scopeNarrative.makingProgress'),
    description: t('scopeNarrative.makingProgressDesc'),
    healthStatus: 'healthy',
  };
}

export function ScopeNarrative({
  point,
  daysSinceUpdate = 0,
  previousPosition,
  showNarrative = true,
  compact = false,
  onClick,
}: ScopeNarrativeProps) {
  const { t } = useTranslation();
  
  const trend = useMemo(() => {
    if (previousPosition === undefined) return 'stable';
    if (point.position > previousPosition) return 'up';
    if (point.position < previousPosition) return 'down';
    return 'stable';
  }, [point.position, previousPosition]);
  
  const narrative = useMemo(() => 
    generateNarrative(point.position, daysSinceUpdate, trend, t),
    [point.position, daysSinceUpdate, trend, t]
  );
  
  const Icon = narrative.icon;
  
  const getProgressColor = () => {
    if (narrative.healthStatus === 'critical') return '[&>div]:bg-red-500';
    if (narrative.healthStatus === 'warning' || narrative.healthStatus === 'stalled') return '[&>div]:bg-amber-500';
    if (point.position >= 50) return '[&>div]:bg-green-500';
    return '[&>div]:bg-blue-500';
  };
  
  if (compact) {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div 
              className={cn(
                'flex items-center gap-2 p-2 rounded-lg cursor-pointer transition-all',
                narrative.bgColor,
                'border',
                narrative.borderColor,
                onClick && 'hover:shadow-md'
              )}
              onClick={onClick}
            >
              <Icon className={cn('h-4 w-4 flex-shrink-0', narrative.iconColor)} />
              <span className="text-sm font-medium truncate flex-1">{point.scope}</span>
              <Badge 
                variant="secondary" 
                className={cn(
                  'text-xs',
                  narrative.healthStatus === 'critical' && 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-200',
                  narrative.healthStatus === 'warning' && 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200',
                  narrative.healthStatus === 'stalled' && 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200'
                )}
              >
                {point.position}%
              </Badge>
            </div>
          </TooltipTrigger>
          <TooltipContent>
            <p className="font-medium">{narrative.label}</p>
            <p className="text-xs text-muted-foreground">{narrative.description}</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }
  
  return (
    <Card 
      className={cn(
        'transition-all',
        narrative.bgColor,
        'border',
        narrative.borderColor,
        onClick && 'cursor-pointer hover:shadow-md'
      )}
      onClick={onClick}
    >
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          <div className={cn(
            'p-2 rounded-lg',
            narrative.healthStatus === 'critical' && 'bg-red-100 dark:bg-red-900/50',
            narrative.healthStatus === 'warning' && 'bg-amber-100 dark:bg-amber-900/50',
            narrative.healthStatus === 'stalled' && 'bg-amber-100 dark:bg-amber-900/50',
            narrative.healthStatus === 'healthy' && 'bg-white/50 dark:bg-black/20'
          )}>
            <Icon className={cn('h-5 w-5', narrative.iconColor)} />
          </div>
          
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-2 mb-1">
              <h4 className="font-semibold text-sm truncate">{point.scope}</h4>
              <Badge variant="outline" className="flex-shrink-0">
                {point.position}%
              </Badge>
            </div>
            
            <Progress 
              value={point.position} 
              className={cn('h-1.5 mb-2', getProgressColor())} 
            />
            
            <div className="flex items-center gap-2">
              <Badge 
                variant="secondary"
                className={cn(
                  'text-xs',
                  narrative.healthStatus === 'critical' && 'bg-red-100 text-red-800 dark:bg-red-900/50 dark:text-red-200',
                  narrative.healthStatus === 'warning' && 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200',
                  narrative.healthStatus === 'stalled' && 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200'
                )}
              >
                {narrative.label}
              </Badge>
              
              {daysSinceUpdate > 0 && (
                <span className="text-xs text-muted-foreground">
                  {t('scopeNarrative.lastUpdated', { days: daysSinceUpdate })}
                </span>
              )}
            </div>
            
            {showNarrative && (
              <p className="text-xs text-muted-foreground mt-2">
                {narrative.description}
              </p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default ScopeNarrative;
