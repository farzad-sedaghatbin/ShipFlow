import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import { AlertCircle, Info, X } from 'lucide-react';
import { safeParseId } from '../utils/validation';
import { HillChart } from '../components/HillChart';
import { hillChartApi } from '../services/hillChartApi';
import { cycleService } from '../services/cycleService';
import { HillChartPoint, Cycle } from '../types';
import { HillChartSkeleton } from '../components/Skeletons';
import { Card, CardContent } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { cn } from '../lib/utils';

export const CycleHillChart: React.FC = () => {
  const { t } = useTranslation();
  const { cycleId: cycleIdParam } = useParams<{ cycleId: string }>();
  const cycleId = safeParseId(cycleIdParam);
  const navigate = useNavigate();
  const [points, setPoints] = useState<HillChartPoint[]>([]);
  const [cycle, setCycle] = useState<Cycle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const abortController = new AbortController();
    loadData();
    return () => abortController.abort();
  }, [cycleId]);

  const loadData = async () => {
    if (!cycleId) return;
    
    try {
      setLoading(true);
      const [cycleData, allPoints] = await Promise.all([
        cycleService.getById(cycleId),
        hillChartApi.getAllHillChartPoints(),
      ]);
      
      setCycle(cycleData.data);
      
      // Filter points that belong to pitches in this cycle
      const cyclePoints = allPoints.filter(point => point.cycleId === cycleId);
      setPoints(cyclePoints);
      setError(null);
    } catch (err) {
      setError(t('cycleHillChart.loadFailed'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handlePointClick = (point: HillChartPoint) => {
    if (point.pitchId) {
      navigate(`/pitches/${point.pitchId}`);
    }
  };

  // Group points by pitch
  const pointsByPitch = points.reduce((acc, point) => {
    const pitchTitle = point.pitchTitle || 'Unknown';
    if (!acc[pitchTitle]) {
      acc[pitchTitle] = [];
    }
    acc[pitchTitle].push(point);
    return acc;
  }, {} as Record<string, HillChartPoint[]>);

  if (cycleId === null) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{t('cycleHillChart.invalidId')}</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <HillChartSkeleton />
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-1">{t('cycleHillChart.title')}</h1>
        {cycle && (
          <p className="text-lg text-muted-foreground">{cycle.name}</p>
        )}
      </div>

      {error && (
        <div className="flex items-center justify-between gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4" />
            <span className="text-sm">{error}</span>
          </div>
          <button onClick={() => setError(null)}>
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {points.length === 0 ? (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500 animate-in fade-in duration-500">
          <Info className="h-4 w-4" />
          <span className="text-sm">No hill chart points found for this cycle. Add scopes to individual pitches to see them here.</span>
        </div>
      ) : (
        <>
          <div className="flex items-center gap-2 p-3 mb-6 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500 animate-in fade-in duration-300">
            <Info className="h-4 w-4 shrink-0" />
            <p className="text-sm">
              Showing all scopes from {Object.keys(pointsByPitch).length} pitch(es) in this cycle.
              Click on any point to view the pitch details.
            </p>
          </div>

          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <HillChart
              points={points}
              onPointClick={handlePointClick}
              readonly={true}
              animateEntrance
            />
          </div>

          <div className="mt-8">
            <h2 className="text-xl font-semibold mb-4">Scopes by Pitch</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {Object.entries(pointsByPitch).map(([pitchTitle, pitchPoints]) => (
                <Card 
                  key={pitchTitle}
                  className="cursor-pointer hover:border-primary/50 transition-colors"
                  onClick={() => pitchPoints[0]?.pitchId && navigate(`/pitches/${pitchPoints[0].pitchId}`)}
                >
                  <CardContent className="p-4">
                    <h3 className="font-semibold mb-2">{pitchTitle}</h3>
                    <div className="flex flex-wrap gap-1.5">
                      {pitchPoints.map(point => (
                        <Badge
                          key={point.id}
                          variant="secondary"
                          className={cn(
                            "text-xs",
                            point.position >= 50 
                              ? "bg-green-500/10 text-green-500" 
                              : "bg-amber-500/10 text-amber-500"
                          )}
                        >
                          {point.scope}: {point.position}%
                        </Badge>
                      ))}
                    </div>
                    <p className="text-xs text-muted-foreground mt-2">
                      {pitchPoints.length} scope(s) • Avg: {Math.round(pitchPoints.reduce((sum, p) => sum + p.position, 0) / pitchPoints.length)}%
                    </p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
};
