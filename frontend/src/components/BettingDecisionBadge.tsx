import { Check, X, Clock, Pencil } from 'lucide-react';
import { Badge } from './ui/badge';
import { cn } from '../lib/utils';
import { BettingDecisionDTO } from '../services/bettingDecisionService';

type DecisionType = 'COMMITTED' | 'REJECTED' | 'DEFERRED' | 'NEEDS_SHAPING';

interface BettingDecisionBadgeProps {
  decision: BettingDecisionDTO | DecisionType;
  showIcon?: boolean;
  size?: 'sm' | 'md';
}

const decisionStyles: Record<DecisionType, {
  icon: typeof Check;
  text: string;
  className: string;
}> = {
  COMMITTED: {
    icon: Check,
    text: 'Committed',
    className: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 border-green-300 dark:border-green-700',
  },
  REJECTED: {
    icon: X,
    text: 'Rejected',
    className: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300 border-red-300 dark:border-red-700',
  },
  DEFERRED: {
    icon: Clock,
    text: 'Deferred',
    className: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300 border-amber-300 dark:border-amber-700',
  },
  NEEDS_SHAPING: {
    icon: Pencil,
    text: 'Needs Shaping',
    className: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300 border-blue-300 dark:border-blue-700',
  },
};

export function BettingDecisionBadge({ 
  decision, 
  showIcon = true, 
  size = 'sm' 
}: BettingDecisionBadgeProps) {
  // Handle both BettingDecisionDTO objects and plain string types
  const decisionType: DecisionType = typeof decision === 'string' 
    ? decision 
    : decision.decision as DecisionType;
  const style = decisionStyles[decisionType];
  const Icon = style.icon;

  return (
    <Badge 
      variant="outline" 
      className={cn(
        'gap-1 font-medium border',
        style.className,
        size === 'sm' ? 'text-[0.65rem] px-1.5 py-0.5 h-5' : 'text-xs px-2 py-1'
      )}
    >
      {showIcon && <Icon className={cn('flex-shrink-0', size === 'sm' ? 'h-3 w-3' : 'h-3.5 w-3.5')} />}
      {style.text}
    </Badge>
  );
}

export default BettingDecisionBadge;
