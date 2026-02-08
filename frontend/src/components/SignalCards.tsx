import React from 'react';
import { useTranslation } from 'react-i18next';
import { 
  TrendingUp, 
  TrendingDown, 
  Minus, 
  Target, 
  Layers, 
  AlertTriangle, 
  CheckCircle2,
  ArrowRight
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import { Badge } from './ui/badge';
import { Progress } from './ui/progress';
import { cn } from '../lib/utils';
import {
  AppetiteAccuracySignal,
  ShapingPatternSignal,
  RiskCorrelationSignal,
  RetroFollowThroughSignal,
  CycleSignals,
  TrendDirection,
  ShapingQuality
} from '../types/signals';

// Re-export for backward compatibility
export type {
  AppetiteAccuracySignal,
  ShapingPatternSignal,
  RiskCorrelationSignal,
  RetroFollowThroughSignal,
  CycleSignals,
  TrendDirection,
  ShapingQuality
};

// Appetite Accuracy Signal Card
interface AppetiteAccuracyCardProps {
  signal: AppetiteAccuracySignal;
}

export const AppetiteAccuracyCard: React.FC<AppetiteAccuracyCardProps> = ({ signal }) => {
  const { t } = useTranslation();

  const getTrendKey = (trend: TrendDirection) => {
    switch (trend) {
      case 'IMPROVING': return 'trendingUp';
      case 'DECLINING': return 'trendingDown';
      case 'STABLE': return 'stable';
      case 'INSUFFICIENT_DATA': return 'insufficientData';
      default: return 'stable';
    }
  };

  const getTrendIcon = () => {
    switch (signal.trend) {
      case 'IMPROVING':
        return <TrendingUp className="h-4 w-4 text-green-500" />;
      case 'DECLINING':
        return <TrendingDown className="h-4 w-4 text-red-500" />;
      default:
        return <Minus className="h-4 w-4 text-muted-foreground" />;
    }
  };

  const getTrendColor = () => {
    switch (signal.trend) {
      case 'IMPROVING':
        return 'bg-green-500/15 text-green-500';
      case 'DECLINING':
        return 'bg-red-500/15 text-red-500';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  // Show absolute variance percentage (0-100+ scale)
  const absVariance = signal.averageVariancePercent != null ? Math.abs(signal.averageVariancePercent) : 0;
  const isOverBudget = (signal.averageVariancePercent ?? 0) > 0;

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Target className="h-4 w-4 text-primary" />
            {t('signals.appetiteAccuracy.title')}
          </CardTitle>
          <Badge variant="outline" className={cn(getTrendColor())}>
            {getTrendIcon()}
            <span className="ml-1">
              {t(`signals.appetiteAccuracy.${getTrendKey(signal.trend)}`)}
            </span>
          </Badge>
        </div>
        <CardDescription className="text-xs">
          {t('signals.appetiteAccuracy.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <div>
            <div className="flex justify-between text-sm mb-1">
              <span className="text-muted-foreground">{t('signals.appetiteAccuracy.avgVariance')}</span>
              <span className="font-medium">{absVariance.toFixed(1)}% {isOverBudget ? t('signals.appetiteAccuracy.over') : t('signals.appetiteAccuracy.under')}</span>
            </div>
            <Progress value={Math.min(absVariance, 100)} className="h-2" />
            <p className="text-xs text-muted-foreground mt-1">
              {t('signals.appetiteAccuracy.cyclesAnalyzed')}: {signal.cyclesAnalyzed}
            </p>
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.appetiteAccuracy.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendations && signal.recommendations.length > 0 && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10 space-y-1">
              {signal.recommendations.map((rec, idx) => (
                <div key={idx} className="flex items-start gap-1">
                  <ArrowRight className="h-3 w-3 inline-block mt-0.5 flex-shrink-0" />
                  <span>{rec}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Shaping Pattern Signal Card
interface ShapingPatternCardProps {
  signal: ShapingPatternSignal;
}

export const ShapingPatternCard: React.FC<ShapingPatternCardProps> = ({ signal }) => {
  const { t } = useTranslation();

  const getQualityColor = () => {
    switch (signal.overallQuality) {
      case 'EXCELLENT':
        return 'bg-green-500/15 text-green-500';
      case 'GOOD':
        return 'bg-blue-500/15 text-blue-500';
      case 'NEEDS_ATTENTION':
        return 'bg-yellow-500/15 text-yellow-500';
      case 'POOR':
        return 'bg-red-500/15 text-red-500';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  const getQualityLabel = () => {
    switch (signal.overallQuality) {
      case 'EXCELLENT':
        return t('signals.shapingPattern.excellent');
      case 'GOOD':
        return t('signals.shapingPattern.good');
      case 'NEEDS_ATTENTION':
        return t('signals.shapingPattern.needsAttention');
      case 'POOR':
        return t('signals.shapingPattern.poor');
      case 'INSUFFICIENT_DATA':
        return t('signals.shapingPattern.insufficientData');
      default:
        return t('signals.shapingPattern.insufficientData');
    }
  };

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Layers className="h-4 w-4 text-primary" />
            {t('signals.shapingPattern.title')}
          </CardTitle>
          <Badge variant="outline" className={cn(getQualityColor())}>
            {getQualityLabel()}
          </Badge>
        </div>
        <CardDescription className="text-xs">
          {t('signals.shapingPattern.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <div className="grid grid-cols-3 gap-2 text-center">
            <div className="bg-green-500/10 p-2 rounded">
              <p className="text-lg font-bold text-green-600">{signal.accuratelyShapedCount}</p>
              <p className="text-xs text-muted-foreground">{t('signals.shapingPattern.wellShaped')}</p>
            </div>
            <div className="bg-blue-500/10 p-2 rounded">
              <p className="text-lg font-bold text-blue-600">{signal.overShapedCount}</p>
              <p className="text-xs text-muted-foreground">{t('signals.shapingPattern.overShaped')}</p>
            </div>
            <div className="bg-yellow-500/10 p-2 rounded">
              <p className="text-lg font-bold text-yellow-600">{signal.underShapedCount}</p>
              <p className="text-xs text-muted-foreground">{t('signals.shapingPattern.underShaped')}</p>
            </div>
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.shapingPattern.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendations && signal.recommendations.length > 0 && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10 space-y-1">
              {signal.recommendations.map((rec, idx) => (
                <div key={idx} className="flex items-start gap-1">
                  <ArrowRight className="h-3 w-3 inline-block mt-0.5 flex-shrink-0" />
                  <span>{rec}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Risk Correlation Signal Card
interface RiskCorrelationCardProps {
  signal: RiskCorrelationSignal;
}

export const RiskCorrelationCard: React.FC<RiskCorrelationCardProps> = ({ signal }) => {
  const { t } = useTranslation();

  const getAccuracyColor = () => {
    if (signal.predictiveAccuracyPercent >= 80) return 'bg-green-500/15 text-green-500';
    if (signal.predictiveAccuracyPercent >= 60) return 'bg-yellow-500/15 text-yellow-500';
    return 'bg-red-500/15 text-red-500';
  };

  const getAccuracyLabel = () => {
    if (signal.predictiveAccuracyPercent >= 80) return t('signals.riskCorrelation.high');
    if (signal.predictiveAccuracyPercent >= 60) return t('signals.riskCorrelation.moderate');
    return t('signals.riskCorrelation.low');
  };

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-primary" />
            {t('signals.riskCorrelation.title')}
          </CardTitle>
          <Badge variant="outline" className={cn(getAccuracyColor())}>
            {getAccuracyLabel()}
          </Badge>
        </div>
        <CardDescription className="text-xs">
          {t('signals.riskCorrelation.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <div className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">{t('signals.riskCorrelation.accuracy')}</span>
            <span className="font-medium">{signal.predictiveAccuracyPercent.toFixed(0)}%</span>
          </div>
          <Progress value={signal.predictiveAccuracyPercent} className="h-2" />
          
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span className="text-muted-foreground">{t('signals.riskCorrelation.truePositives')}</span>
              <p className="font-medium text-green-600">{signal.truePositives}</p>
            </div>
            <div>
              <span className="text-muted-foreground">{t('signals.riskCorrelation.missedRisks')}</span>
              <p className="font-medium text-red-600">{signal.missedRisks}</p>
            </div>
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.riskCorrelation.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendations && signal.recommendations.length > 0 && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10 space-y-1">
              {signal.recommendations.map((rec, idx) => (
                <div key={idx} className="flex items-start gap-1">
                  <ArrowRight className="h-3 w-3 inline-block mt-0.5 flex-shrink-0" />
                  <span>{rec}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Retro Follow-Through Signal Card
interface RetroFollowThroughCardProps {
  signal: RetroFollowThroughSignal;
}

export const RetroFollowThroughCard: React.FC<RetroFollowThroughCardProps> = ({ signal }) => {
  const { t } = useTranslation();

  const getRateColor = () => {
    if (signal.followThroughRatePercent >= 80) return 'text-green-500';
    if (signal.followThroughRatePercent >= 50) return 'text-yellow-500';
    return 'text-red-500';
  };

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-primary" />
            {t('signals.retroFollowThrough.title')}
          </CardTitle>
          <span className={cn("text-lg font-bold", getRateColor())}>
            {signal.followThroughRatePercent.toFixed(0)}%
          </span>
        </div>
        <CardDescription className="text-xs">
          {t('signals.retroFollowThrough.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <Progress value={signal.followThroughRatePercent} className="h-2" />
          
          <div className="grid grid-cols-3 gap-2 text-center text-xs">
            <div>
              <p className="font-bold text-foreground">{signal.totalActionItems}</p>
              <p className="text-muted-foreground">{t('signals.retroFollowThrough.totalActions')}</p>
            </div>
            <div>
              <p className="font-bold text-green-600">{signal.actedOnCount}</p>
              <p className="text-muted-foreground">{t('signals.retroFollowThrough.actedOn')}</p>
            </div>
            <div>
              <p className="font-bold text-yellow-600">{signal.pendingCount}</p>
              <p className="text-muted-foreground">{t('signals.retroFollowThrough.pending')}</p>
            </div>
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.retroFollowThrough.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendations && signal.recommendations.length > 0 && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10 space-y-1">
              {signal.recommendations.map((rec, idx) => (
                <div key={idx} className="flex items-start gap-1">
                  <ArrowRight className="h-3 w-3 inline-block mt-0.5 flex-shrink-0" />
                  <span>{rec}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Health Score Card
interface HealthScoreCardProps {
  score: number | undefined;
}

export const HealthScoreCard: React.FC<HealthScoreCardProps> = ({ score }) => {
  const { t } = useTranslation();
  if (score === undefined) return null;

  const getScoreColor = () => {
    if (score >= 80) return 'text-green-500 border-green-500';
    if (score >= 60) return 'text-yellow-500 border-yellow-500';
    if (score >= 40) return 'text-orange-500 border-orange-500';
    return 'text-red-500 border-red-500';
  };

  return (
    <div className="flex items-center gap-4">
      <div className={cn(
        "w-16 h-16 rounded-full border-4 flex items-center justify-center",
        getScoreColor()
      )}>
        <span className="text-xl font-bold">{score}</span>
      </div>
      <div>
        <p className="font-medium">{t('signals.healthScore')}</p>
        <p className="text-xs text-muted-foreground">{t('signals.healthScoreDesc')}</p>
      </div>
    </div>
  );
};
