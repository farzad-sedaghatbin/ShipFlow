import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertTriangle, XCircle, Clock, TrendingUp } from 'lucide-react';
import { PitchHealthCard } from './PitchHealthCard';
import { 
  CycleHealthSummaryDTO, 
  pitchHealthService, 
  getHealthLabel 
} from '../services/pitchHealthService';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { cn } from '../lib/utils';

interface CycleHealthSummaryProps {
  cycleId: number;
  onPitchClick?: (pitchId: number) => void;
}

export const CycleHealthSummary: React.FC<CycleHealthSummaryProps> = ({ 
  cycleId, 
  onPitchClick 
}) => {
  const [summary, setSummary] = useState<CycleHealthSummaryDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadSummary = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await pitchHealthService.getCycleHealth(cycleId);
        setSummary(data);
      } catch (err) {
        setError('Failed to load cycle health summary');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    loadSummary();
  }, [cycleId]);

  if (loading) {
    return (
      <Card aria-busy="true" aria-live="polite">
        <CardContent className="p-6">
          <Skeleton className="h-8 w-3/5 mb-4" />
          <div className="flex gap-4 mt-4">
            <Skeleton className="h-20 w-24" />
            <Skeleton className="h-20 w-24" />
            <Skeleton className="h-20 w-24" />
          </div>
          <Skeleton className="h-48 w-full mt-4" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!summary) {
    return (
      <Alert>
        <AlertDescription>No health data available</AlertDescription>
      </Alert>
    );
  }

  const getHealthBadgeClass = (color: string) => {
    if (color.includes('4caf50') || color.includes('green')) {
      return 'bg-green-500/15 text-green-500 border-green-500/30';
    }
    if (color.includes('ff9800') || color.includes('orange') || color.includes('yellow')) {
      return 'bg-yellow-500/15 text-yellow-500 border-yellow-500/30';
    }
    if (color.includes('f44336') || color.includes('red')) {
      return 'bg-red-500/15 text-red-500 border-red-500/30';
    }
    return 'bg-primary/15 text-primary border-primary/30';
  };

  const isCritical = summary.overallHealth === 'CRITICAL' || summary.overallHealth === 'HIGH';
  const hasCriticalPitches = summary.criticalPitches > 0;

  return (
    <Card className={cn(
      isCritical && "border-red-500/60 shadow-2xl shadow-red-500/20",
      hasCriticalPitches && "ring-2 ring-red-500/30"
    )}>
      <CardContent className="p-6">
        {/* Header */}
        <div className="flex justify-between items-start mb-6">
          <div>
            <h2 className="text-xl font-semibold">{summary.cycleName}</h2>
            {summary.projectName && (
              <p className="text-sm text-muted-foreground">{summary.projectName}</p>
            )}
          </div>
          <div className="flex flex-col items-end gap-2">
            <Badge 
              variant="outline" 
              className={cn('text-sm font-semibold', getHealthBadgeClass(summary.healthColor), isCritical && 'animate-pulse')}
            >
              {getHealthLabel(summary.overallHealth)}
            </Badge>
            {isCritical && summary.criticalPitches > 0 && (
              <span className="text-xs text-red-500 font-medium flex items-center gap-1">
                <AlertTriangle className="h-3 w-3" />
                Action required
              </span>
            )}
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card className="bg-green-500/5 border-green-500/20 hover:bg-green-500/10 transition-colors">
            <CardContent className="p-4 text-center">
              <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2" aria-hidden="true" />
              <p className="text-3xl font-bold text-green-500">{summary.healthyPitches}</p>
              <p className="text-sm text-muted-foreground">Healthy</p>
            </CardContent>
          </Card>
          
          <Card className="bg-yellow-500/5 border-yellow-500/20 hover:bg-yellow-500/10 transition-colors">
            <CardContent className="p-4 text-center">
              <AlertTriangle className="h-8 w-8 text-yellow-500 mx-auto mb-2" aria-hidden="true" />
              <p className="text-3xl font-bold text-yellow-500">{summary.atRiskPitches}</p>
              <p className="text-sm text-muted-foreground">At Risk</p>
            </CardContent>
          </Card>
          
          <Card className={cn(
            "bg-red-500/5 border-red-500/20 hover:bg-red-500/10 transition-all",
            summary.criticalPitches > 0 && "border-red-600/50 shadow-xl shadow-red-500/30 ring-2 ring-red-500/20"
          )}>
            <CardContent className="p-4 text-center relative">
              {summary.criticalPitches > 0 && (
                <div className="absolute top-0 right-0 bg-red-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-bl-md rounded-tr-md">
                  ACTION NEEDED
                </div>
              )}
              <XCircle className={cn(
                "h-8 w-8 text-red-500 mx-auto mb-2",
                summary.criticalPitches > 0 && "animate-pulse"
              )} aria-hidden="true" />
              <p className={cn(
                "text-3xl font-bold text-red-500",
                summary.criticalPitches > 0 && "animate-pulse text-4xl"
              )}>{summary.criticalPitches}</p>
              <p className="text-sm text-muted-foreground font-semibold">Critical</p>
              {summary.criticalPitches > 0 && (
                <p className="text-xs text-red-600 font-bold mt-1 animate-pulse">⚠ Urgent!</p>
              )}
            </CardContent>
          </Card>
          
          <Card className={cn(
            summary.daysLeft <= 7 && "bg-orange-500/5 border-orange-500/20"
          )}>
            <CardContent className="p-4 text-center">
              <Clock className={cn(
                "h-8 w-8 mx-auto mb-2",
                summary.daysLeft <= 7 ? "text-orange-500" : "text-primary"
              )} aria-hidden="true" />
              <p className={cn(
                "text-3xl font-bold",
                summary.daysLeft <= 7 ? "text-orange-500" : "text-primary"
              )}>{summary.daysLeft}</p>
              <p className="text-sm text-muted-foreground">Days Left</p>
            </CardContent>
          </Card>
        </div>

        {/* Progress Bars */}
        <div className="space-y-4 mb-6">
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium flex items-center gap-1">
                <Clock className="h-4 w-4" aria-hidden="true" />
                Cycle Progress
              </span>
              <span className="text-sm text-muted-foreground">
                {summary.cycleProgressPercent.toFixed(0)}%
              </span>
            </div>
            <Progress 
              value={summary.cycleProgressPercent} 
              className="h-2"
              aria-label={`Cycle progress: ${summary.cycleProgressPercent.toFixed(0)}%`}
            />
          </div>
          
          <div>
            <div className="flex justify-between items-center mb-2">
              <span className="text-sm font-medium flex items-center gap-1">
                <TrendingUp className="h-4 w-4" aria-hidden="true" />
                Budget Used
                {summary.budgetUsedPercent > 100 && (
                  <Badge variant="destructive" className="text-[10px] px-1 py-0 ml-1">OVER</Badge>
                )}
                {summary.budgetUsedPercent > 80 && summary.budgetUsedPercent <= 100 && (
                  <Badge variant="outline" className="text-[10px] px-1 py-0 ml-1 bg-yellow-500/10 text-yellow-600 border-yellow-500/30">HIGH</Badge>
                )}
              </span>
              <span className={cn(
                "text-sm font-medium",
                summary.budgetUsedPercent > 100 && "text-red-600 font-bold",
                summary.budgetUsedPercent > 80 && summary.budgetUsedPercent <= 100 && "text-yellow-600",
                summary.budgetUsedPercent <= 80 && "text-muted-foreground"
              )}>
                {summary.totalActualHours.toFixed(1)}h / {summary.totalAppetiteHours.toFixed(0)}h ({summary.budgetUsedPercent.toFixed(0)}%)
              </span>
            </div>
            <Progress 
              value={Math.min(100, summary.budgetUsedPercent)} 
              className={cn(
                "h-2.5",
                summary.budgetUsedPercent > 100 
                  ? "[&>div]:bg-red-600 [&>div]:animate-pulse" 
                  : summary.budgetUsedPercent > 90
                    ? "[&>div]:bg-red-500"
                    : summary.budgetUsedPercent > 80 
                      ? "[&>div]:bg-yellow-500" 
                      : ""
              )}
              aria-label={`Budget used: ${summary.totalActualHours.toFixed(1)} hours of ${summary.totalAppetiteHours.toFixed(0)} hours`}
            />
          </div>
        </div>

        {/* Status Breakdown */}
        <div className="mb-6">
          <h3 className="text-sm font-medium mb-2">Status Breakdown</h3>
          <div className="flex flex-wrap gap-2">
            <Badge variant="outline">Pending: {summary.pendingCount}</Badge>
            <Badge variant="outline" className="bg-primary/10 text-primary border-primary/30">
              In Progress: {summary.inProgressCount}
            </Badge>
            <Badge variant="outline" className="bg-yellow-500/10 text-yellow-500 border-yellow-500/30">
              Testing: {summary.testingCount}
            </Badge>
            <Badge variant="outline" className="bg-green-500/10 text-green-500 border-green-500/30">
              Done: {summary.doneCount}
            </Badge>
          </div>
        </div>

        <Separator className="my-6" />

        {/* Pitch List */}
        <div>
          <h3 className="text-lg font-semibold mb-4">
            All Pitches ({summary.totalPitches})
          </h3>
          {summary.criticalPitches > 0 && (
            <div className="mb-4 p-4 rounded-lg bg-gradient-to-r from-red-500/20 to-red-600/10 border-2 border-red-500/40 flex items-center gap-3 shadow-lg shadow-red-500/20">
              <div className="flex-shrink-0">
                <XCircle className="h-6 w-6 text-red-600 animate-pulse" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-bold text-red-600 mb-1">
                  ⚠️ IMMEDIATE ATTENTION REQUIRED
                </p>
                <p className="text-sm text-red-600">
                  {summary.criticalPitches} critical {summary.criticalPitches === 1 ? 'pitch requires' : 'pitches require'} immediate action
                </p>
              </div>
            </div>
          )}
          {summary.atRiskPitches > 0 && summary.criticalPitches === 0 && (
            <div className="mb-3 p-3 rounded-lg bg-yellow-500/10 border border-yellow-500/30 flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-yellow-600" />
              <span className="text-sm text-yellow-600 font-medium">
                {summary.atRiskPitches} {summary.atRiskPitches === 1 ? 'pitch is' : 'pitches are'} at risk - monitor closely
              </span>
            </div>
          )}
          <div className="space-y-3">
            {/* Sort by risk level: CRITICAL, HIGH, MEDIUM, LOW */}
            {summary.pitchHealthList
              .sort((a, b) => {
                const riskOrder = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
                return (riskOrder[a.riskLevel] || 3) - (riskOrder[b.riskLevel] || 3);
              })
              .map((pitch) => (
                <PitchHealthCard 
                  key={pitch.pitchId} 
                  health={pitch} 
                  onClick={onPitchClick ? () => onPitchClick(pitch.pitchId) : undefined}
                />
              ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default CycleHealthSummary;
