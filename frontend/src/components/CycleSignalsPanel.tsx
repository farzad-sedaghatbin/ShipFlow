import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Activity, RefreshCcw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertDescription } from './ui/alert';
import { 
  AppetiteAccuracyCard, 
  ShapingPatternCard, 
  RiskCorrelationCard, 
  RetroFollowThroughCard,
  HealthScoreCard,
  CycleSignals
} from './SignalCards';
import { signalService } from '../services/signalService';

interface CycleSignalsPanelProps {
  cycleId?: number;
  projectId: number;
  className?: string;
}

export const CycleSignalsPanel: React.FC<CycleSignalsPanelProps> = ({ 
  cycleId, 
  projectId,
  className 
}) => {
  const { t } = useTranslation();
  const [signals, setSignals] = useState<CycleSignals | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadSignals = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = cycleId 
        ? await signalService.getCycleSignals(cycleId)
        : await signalService.getProjectSignals(projectId);
      setSignals(data);
    } catch (err) {
      setError(t('signals.loadError'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSignals();
  }, [cycleId, projectId]);

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader>
          <Skeleton className="h-6 w-40" />
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Skeleton className="h-48" />
            <Skeleton className="h-48" />
            <Skeleton className="h-48" />
            <Skeleton className="h-48" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={className}>
        <CardContent className="p-6">
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (!signals) {
    return (
      <Card className={className}>
        <CardContent className="p-6">
          <Alert>
            <AlertDescription>{t('signals.noData')}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            {t('signals.title')}
          </CardTitle>
          <div className="flex items-center gap-4">
            <HealthScoreCard score={signals.overallHealthScore} />
            <Button variant="outline" size="sm" onClick={loadSignals}>
              <RefreshCcw className="h-4 w-4 mr-1" />
              {t('signals.refresh')}
            </Button>
          </div>
        </div>
        <p className="text-sm text-muted-foreground">{t('signals.subtitle')}</p>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {signals.appetiteAccuracy && (
            <AppetiteAccuracyCard signal={signals.appetiteAccuracy} />
          )}
          {signals.shapingPattern && (
            <ShapingPatternCard signal={signals.shapingPattern} />
          )}
          {signals.riskCorrelation && (
            <RiskCorrelationCard signal={signals.riskCorrelation} />
          )}
          {signals.retroFollowThrough && (
            <RetroFollowThroughCard signal={signals.retroFollowThrough} />
          )}
        </div>
        
        {!signals.appetiteAccuracy && !signals.shapingPattern && 
         !signals.riskCorrelation && !signals.retroFollowThrough && (
          <Alert>
            <AlertDescription>
              {t('signals.noHistoricalData')}
            </AlertDescription>
          </Alert>
        )}
        
        <p className="text-xs text-muted-foreground text-right mt-4">
          {t('narratives.generatedAt')} {new Date(signals.analyzedAt).toLocaleString()}
        </p>
      </CardContent>
    </Card>
  );
};

export default CycleSignalsPanel;
