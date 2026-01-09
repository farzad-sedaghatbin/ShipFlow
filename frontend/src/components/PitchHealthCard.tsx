import React from 'react';
import { ChevronDown, Circle, Clock, TrendingUp, AlertTriangle } from 'lucide-react';
import { PitchHealthDTO, getHealthLabel, getQAStatusLabel } from '../services/pitchHealthService';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import { Button } from './ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from './ui/collapsible';
import { cn } from '../lib/utils';

interface PitchHealthCardProps {
  health: PitchHealthDTO;
  compact?: boolean;
  onClick?: () => void;
}

export const PitchHealthCard: React.FC<PitchHealthCardProps> = ({ 
  health, 
  compact = false,
  onClick 
}) => {
  const [expanded, setExpanded] = React.useState(false);

  const appetiteStatus = health.appetiteUsedPercent > 100 
    ? 'over' 
    : health.appetiteUsedPercent > 80 
      ? 'warning' 
      : 'ok';

  const getProgressClass = () => {
    if (appetiteStatus === 'over') return '[&>div]:bg-red-500';
    if (appetiteStatus === 'warning') return '[&>div]:bg-yellow-500';
    return '';
  };

  const getRiskBadgeClass = () => {
    if (health.riskColor.includes('4caf50') || health.riskColor.includes('green')) {
      return 'bg-green-500/15 text-green-500 border-green-500/30';
    }
    if (health.riskColor.includes('ff9800') || health.riskColor.includes('orange') || health.riskColor.includes('yellow')) {
      return 'bg-yellow-500/15 text-yellow-500 border-yellow-500/30';
    }
    if (health.riskColor.includes('f44336') || health.riskColor.includes('red')) {
      return 'bg-red-500/15 text-red-500 border-red-500/30';
    }
    return 'bg-primary/15 text-primary border-primary/30';
  };

  const getRiskDotColor = () => {
    if (health.riskColor.includes('4caf50') || health.riskColor.includes('green')) {
      return 'text-green-500';
    }
    if (health.riskColor.includes('ff9800') || health.riskColor.includes('orange') || health.riskColor.includes('yellow')) {
      return 'text-yellow-500';
    }
    if (health.riskColor.includes('f44336') || health.riskColor.includes('red')) {
      return 'text-red-500';
    }
    return 'text-primary';
  };

  return (
    <Collapsible open={expanded} onOpenChange={setExpanded}>
      <Card 
        className={cn(
          "mb-3 border-l-4 transition-shadow",
          onClick && "cursor-pointer hover:shadow-lg"
        )}
        style={{ borderLeftColor: health.riskColor }}
        onClick={onClick}
      >
        <CardContent className={cn("pt-4", compact ? "pb-2" : "pb-4")}>
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <h3 className={cn("font-semibold", compact ? "text-base" : "text-lg")}>
                  {health.pitchName}
                </h3>
                <Badge variant="outline" className={cn('gap-1', getRiskBadgeClass())}>
                  <Circle className={cn('h-2 w-2 fill-current', getRiskDotColor())} />
                  {getHealthLabel(health.riskLevel)}
                </Badge>
              </div>
              
              <p className="text-sm text-muted-foreground mb-2">
                {health.statusSummary}
              </p>
              
              {!compact && (
                <TooltipProvider>
                  <div className="flex flex-wrap gap-4 items-center">
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="flex items-center gap-1 text-sm text-muted-foreground">
                          <Clock className="h-4 w-4" />
                          <span>{health.daysLeft} days left</span>
                        </div>
                      </TooltipTrigger>
                      <TooltipContent>Days remaining in cycle</TooltipContent>
                    </Tooltip>
                    
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className={cn(
                          "flex items-center gap-1 text-sm",
                          appetiteStatus === 'over' 
                            ? 'text-red-500' 
                            : appetiteStatus === 'warning' 
                              ? 'text-yellow-500' 
                              : 'text-muted-foreground'
                        )}>
                          <TrendingUp className="h-4 w-4" />
                          <span>{health.appetiteUsedPercent.toFixed(0)}% budget used</span>
                        </div>
                      </TooltipTrigger>
                      <TooltipContent>Budget utilization</TooltipContent>
                    </Tooltip>
                    
                    {health.qaStatus === 'NOT_STARTED' && health.status === 'IN_PROGRESS' && (
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div className="flex items-center gap-1 text-sm text-yellow-500">
                            <AlertTriangle className="h-4 w-4" />
                            <span>{getQAStatusLabel(health.qaStatus)}</span>
                          </div>
                        </TooltipTrigger>
                        <TooltipContent>QA has not started</TooltipContent>
                      </Tooltip>
                    )}
                  </div>
                </TooltipProvider>
              )}
            </div>
            
            {!compact && (
              <CollapsibleTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon"
                  className="h-8 w-8"
                  onClick={(e) => e.stopPropagation()}
                >
                  <ChevronDown 
                    className={cn(
                      "h-4 w-4 transition-transform",
                      expanded && "rotate-180"
                    )} 
                  />
                </Button>
              </CollapsibleTrigger>
            )}
          </div>
          
          {/* Budget progress bar */}
          <div className="mt-3">
            <div className="flex justify-between mb-1">
              <span className="text-xs text-muted-foreground">
                Budget: {health.actualHours.toFixed(1)}h / {health.appetiteHours.toFixed(0)}h
              </span>
              <span className="text-xs text-muted-foreground">
                {health.teamName}
              </span>
            </div>
            <Progress 
              value={Math.min(100, health.appetiteUsedPercent)} 
              className={cn("h-1.5", getProgressClass())}
            />
          </div>
          
          <CollapsibleContent>
            <div className="mt-4 pt-4 border-t border-border">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-muted-foreground">Cycle</p>
                  <p className="text-sm">{health.cycleName}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Cycle End</p>
                  <p className="text-sm">{new Date(health.cycleEndDate).toLocaleDateString()}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Status</p>
                  <p className="text-sm">{health.status.replace('_', ' ')}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">QA Status</p>
                  <p className="text-sm">{getQAStatusLabel(health.qaStatus)}</p>
                </div>
              </div>
            </div>
          </CollapsibleContent>
        </CardContent>
      </Card>
    </Collapsible>
  );
};

export default PitchHealthCard;
