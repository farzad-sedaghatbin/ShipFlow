import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { hillChartApi } from '../services/hillChartApi';
import { HillChartPoint } from '../types';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Progress } from '@/components/ui/progress';

interface HillChartWidgetProps {
  pitchId?: number;
  cycleId?: number;
  projectId?: number;
  maxPoints?: number;
}

export const HillChartWidget: React.FC<HillChartWidgetProps> = ({ 
  pitchId, 
  cycleId,
  projectId,
  maxPoints = 5 
}) => {
  const navigate = useNavigate();
  const [points, setPoints] = useState<HillChartPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPoints();
  }, [pitchId, cycleId, projectId]);

  const loadPoints = async () => {
    try {
      setLoading(true);
      let allPoints: HillChartPoint[];
      
      if (pitchId) {
        allPoints = await hillChartApi.getHillChartPointsByPitch(pitchId);
      } else {
        allPoints = await hillChartApi.getAllHillChartPoints();
        if (cycleId) {
          allPoints = allPoints.filter(p => p.cycleId === cycleId);
        } else if (projectId) {
          allPoints = allPoints.filter(p => p.projectId === projectId);
        }
      }
      
      setPoints(allPoints.slice(0, maxPoints));
    } catch (err) {
      console.error('Failed to load hill chart points', err);
    } finally {
      setLoading(false);
    }
  };

  const handleClick = () => {
    if (pitchId) {
      navigate(`/pitches/${pitchId}/hill-chart`);
    } else if (cycleId) {
      navigate(`/cycles/${cycleId}/hill-chart`);
    } else if (points.length > 0 && points[0].cycleId) {
      navigate(`/cycles/${points[0].cycleId}/hill-chart`);
    } else if (points.length > 0 && points[0].pitchId) {
      navigate(`/pitches/${points[0].pitchId}/hill-chart`);
    }
  };

  const getPositionLabel = (position: number) => {
    if (position < 25) return 'Just started';
    if (position < 50) return 'Figuring out';
    if (position < 75) return 'Making progress';
    if (position < 100) return 'Almost done';
    return 'Complete';
  };

  const getPositionColorClass = (position: number) => {
    // Reserve green strictly for "done" (100%). Progress is already conveyed by the bar/position,
    // so a green at 75% wrongly reads as finished. Uphill = amber, downhill in-progress = blue.
    if (position >= 100) return 'bg-emerald-500';
    if (position < 50) return 'bg-amber-500';
    return 'bg-blue-500';
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-4" aria-busy="true" aria-live="polite">
          <Skeleton className="w-3/5 h-7 mb-2" />
          <Skeleton className="w-full h-28" />
        </CardContent>
      </Card>
    );
  }

  if (points.length === 0) {
    return null;
  }

  return (
    <Card className="cursor-pointer hover:shadow-md transition-shadow" onClick={handleClick}>
      <CardContent className="p-4">
        <h2 className="text-lg font-semibold text-foreground mb-3" id="hill-chart-widget-title">
          Hill Chart Progress
        </h2>
        
        <div className="space-y-3" role="list" aria-label="Hill chart progress items">
          {points.map((point, index) => (
            <div 
              key={point.id}
              role="listitem"
              aria-label={`${point.scope}: ${point.position}% complete - ${getPositionLabel(point.position)}`}
              className={cn(
                'pb-3',
                index < points.length - 1 && 'border-b border-border'
              )}
            >
              <div className="flex justify-between items-center mb-1">
                <span className="text-sm font-semibold text-foreground">
                  {point.scope}
                </span>
                <span 
                  className={cn(
                    'text-xs font-bold px-2 py-0.5 rounded text-white',
                    getPositionColorClass(point.position)
                  )}
                  aria-hidden="true"
                >
                  {point.position}%
                </span>
              </div>
              <Progress 
                value={point.position}
                className={cn('h-1.5', `[&>div]:${getPositionColorClass(point.position)}`)}
                aria-label={`${point.scope} progress: ${point.position}%`}
              />
              <span className="text-xs text-muted-foreground mt-0.5 block">
                {getPositionLabel(point.position)}
              </span>
            </div>
          ))}
        </div>

        {points.length > 0 && (
          <p className="text-xs text-primary mt-3 text-center">
            Click to view full hill chart →
          </p>
        )}
      </CardContent>
    </Card>
  );
};
