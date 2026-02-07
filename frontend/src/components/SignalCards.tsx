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

// Types matching backend DTOs
export interface AppetiteAccuracySignal {
  averageAccuracyRatio: number;
  trend: 'IMPROVING' | 'DECLINING' | 'STABLE';
  trendSlope: number;
  cycleCount: number;
  cycleData: Array<{
    cycleId: number;
    cycleName: string;
    appetiteTotal: number;
    actualTotal: number;
    ratio: number;
  }>;
  interpretation: string;
  recommendation: string;
}

export interface ShapingPatternSignal {
  quality: 'WELL_SHAPED' | 'OVER_SHAPED' | 'UNDER_SHAPED' | 'NEEDS_IMPROVEMENT';
  avgUncertaintyScore: number;
  avgRabbitHoles: number;
  overShapedCount: number;
  underShapedCount: number;
  wellShapedCount: number;
  totalAnalyzed: number;
  interpretation: string;
  recommendation: string;
}

export interface RiskCorrelationSignal {
  correlationStrength: 'STRONG' | 'MODERATE' | 'WEAK' | 'NONE';
  avgPredictedRisk: number;
  avgActualRisk: number;
  accuracy: number;
  pitchesAnalyzed: number;
  interpretation: string;
  recommendation: string;
}

export interface RetroFollowThroughSignal {
  overallFollowThroughRate: number;
  totalActionItems: number;
  actedOnCount: number;
  pendingCount: number;
  retroData: Array<{
    retroId: number;
    retroTitle: string;
    actionItems: number;
    actedOn: number;
    rate: number;
  }>;
  interpretation: string;
  recommendation: string;
}

export interface CycleSignals {
  cycleId?: number;
  projectId: number;
  overallHealthScore: number;
  appetiteAccuracy: AppetiteAccuracySignal | null;
  shapingPattern: ShapingPatternSignal | null;
  riskCorrelation: RiskCorrelationSignal | null;
  retroFollowThrough: RetroFollowThroughSignal | null;
  analyzedAt: string;
}

// Appetite Accuracy Signal Card
interface AppetiteAccuracyCardProps {
  signal: AppetiteAccuracySignal;
}

export const AppetiteAccuracyCard: React.FC<AppetiteAccuracyCardProps> = ({ signal }) => {
  const { t } = useTranslation();

  const getTrendKey = (trend: string) => {
    switch (trend) {
      case 'IMPROVING': return 'trendingUp';
      case 'DECLINING': return 'trendingDown';
      case 'STABLE': return 'stable';
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

  const ratioPercentage = Math.min(signal.averageAccuracyRatio * 100, 200);

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
              <span className="text-muted-foreground">{t('signals.appetiteAccuracy.avgRatio')}</span>
              <span className="font-medium">{(signal.averageAccuracyRatio * 100).toFixed(0)}%</span>
            </div>
            <Progress value={ratioPercentage > 100 ? 100 : ratioPercentage} className="h-2" />
            {ratioPercentage > 100 && (
              <p className="text-xs text-yellow-500 mt-1">Over appetite by {((signal.averageAccuracyRatio - 1) * 100).toFixed(0)}%</p>
            )}
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.appetiteAccuracy.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendation && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10">
              <ArrowRight className="h-3 w-3 inline mr-1" />
              {signal.recommendation}
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
    switch (signal.quality) {
      case 'WELL_SHAPED':
        return 'bg-green-500/15 text-green-500';
      case 'OVER_SHAPED':
        return 'bg-blue-500/15 text-blue-500';
      case 'UNDER_SHAPED':
        return 'bg-yellow-500/15 text-yellow-500';
      default:
        return 'bg-red-500/15 text-red-500';
    }
  };

  const getQualityLabel = () => {
    switch (signal.quality) {
      case 'WELL_SHAPED':
        return t('signals.shapingPattern.wellShaped');
      case 'OVER_SHAPED':
        return t('signals.shapingPattern.overShaped');
      case 'UNDER_SHAPED':
        return t('signals.shapingPattern.underShaped');
      default:
        return t('signals.shapingPattern.needsImprovement');
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
              <p className="text-lg font-bold text-green-600">{signal.wellShapedCount}</p>
              <p className="text-xs text-muted-foreground">Well Shaped</p>
            </div>
            <div className="bg-blue-500/10 p-2 rounded">
              <p className="text-lg font-bold text-blue-600">{signal.overShapedCount}</p>
              <p className="text-xs text-muted-foreground">Over Shaped</p>
            </div>
            <div className="bg-yellow-500/10 p-2 rounded">
              <p className="text-lg font-bold text-yellow-600">{signal.underShapedCount}</p>
              <p className="text-xs text-muted-foreground">Under Shaped</p>
            </div>
          </div>
          
          <div className="text-xs text-muted-foreground border-t pt-2">
            <p className="font-medium text-foreground">{t('signals.shapingPattern.interpretation')}</p>
            <p className="mt-1">{signal.interpretation}</p>
          </div>
          
          {signal.recommendation && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10">
              <ArrowRight className="h-3 w-3 inline mr-1" />
              {signal.recommendation}
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

  const getStrengthColor = () => {
    switch (signal.correlationStrength) {
      case 'STRONG':
        return 'bg-green-500/15 text-green-500';
      case 'MODERATE':
        return 'bg-yellow-500/15 text-yellow-500';
      case 'WEAK':
        return 'bg-orange-500/15 text-orange-500';
      default:
        return 'bg-red-500/15 text-red-500';
    }
  };

  const getStrengthLabel = () => {
    switch (signal.correlationStrength) {
      case 'STRONG':
        return t('signals.riskCorrelation.strong');
      case 'MODERATE':
        return t('signals.riskCorrelation.moderate');
      default:
        return t('signals.riskCorrelation.weak');
    }
  };

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-primary" />
            {t('signals.riskCorrelation.title')}
          </CardTitle>
          <Badge variant="outline" className={cn(getStrengthColor())}>
            {getStrengthLabel()}
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
            <span className="font-medium">{(signal.accuracy * 100).toFixed(0)}%</span>
          </div>
          <Progress value={signal.accuracy * 100} className="h-2" />
          
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span className="text-muted-foreground">{t('signals.riskCorrelation.avgPredicted')}</span>
              <p className="font-medium">{(signal.avgPredictedRisk * 100).toFixed(0)}%</p>
            </div>
            <div>
              <span className="text-muted-foreground">{t('signals.riskCorrelation.avgActual')}</span>
              <p className="font-medium">{(signal.avgActualRisk * 100).toFixed(0)}%</p>
            </div>
          </div>
          
          {signal.recommendation && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10">
              <ArrowRight className="h-3 w-3 inline mr-1" />
              {signal.recommendation}
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
    if (signal.overallFollowThroughRate >= 0.8) return 'text-green-500';
    if (signal.overallFollowThroughRate >= 0.5) return 'text-yellow-500';
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
            {(signal.overallFollowThroughRate * 100).toFixed(0)}%
          </span>
        </div>
        <CardDescription className="text-xs">
          {t('signals.retroFollowThrough.description')}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          <Progress value={signal.overallFollowThroughRate * 100} className="h-2" />
          
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
          
          {signal.recommendation && (
            <div className="text-xs bg-primary/5 p-2 rounded border border-primary/10">
              <ArrowRight className="h-3 w-3 inline mr-1" />
              {signal.recommendation}
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
  if (score === undefined) return null;
  const { t } = useTranslation();

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
