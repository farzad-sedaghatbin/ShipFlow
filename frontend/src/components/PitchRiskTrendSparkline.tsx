import { useState, useEffect } from 'react';
import { LineChart, Line, ResponsiveContainer, Tooltip } from 'recharts';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { riskService, PitchRiskHistory } from '../services/riskService';
import { cn } from '@/lib/utils';

interface PitchRiskTrendSparklineProps {
  pitchId: number;
  currentScore: number;
  days?: number;
  compact?: boolean;
}

export default function PitchRiskTrendSparkline({
  pitchId,
  currentScore,
  days = 30,
  compact = false,
}: PitchRiskTrendSparklineProps) {
  const [history, setHistory] = useState<PitchRiskHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [trend, setTrend] = useState<'up' | 'down' | 'stable'>('stable');

  useEffect(() => {
    loadHistory();
  }, [pitchId, days]);

  const loadHistory = async () => {
    try {
      setLoading(true);
      const data = await riskService.getRiskHistory(pitchId, days);
      setHistory(data);
      
      // Calculate trend
      if (data.length >= 2) {
        const oldest = data[data.length - 1].riskScore;
        const newest = data[0].riskScore;
        const change = newest - oldest;
        
        if (change > 5) setTrend('up');
        else if (change < -5) setTrend('down');
        else setTrend('stable');
      }
    } catch (error) {
      console.error('Failed to load risk history:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading || history.length < 2) {
    return null; // No sparkline if insufficient data
  }

  const chartData = history
    .slice()
    .reverse()
    .map((h) => ({
      date: new Date(h.recordedAt).toLocaleDateString(),
      score: h.riskScore,
      level: h.riskLevel,
    }));

  const getStrokeColor = () => {
    if (currentScore >= 80) return '#d32f2f'; // Critical
    if (currentScore >= 60) return '#f57c00'; // High
    if (currentScore >= 30) return '#fbc02d'; // Medium
    return '#388e3c'; // Low
  };

  const TrendIcon = trend === 'up' ? TrendingUp : trend === 'down' ? TrendingDown : Minus;

  return (
    <div className="flex items-center gap-2">
      {/* Trend indicator */}
      <div
        className={cn(
          'flex items-center gap-1 text-xs',
          trend === 'up' && 'text-destructive',
          trend === 'down' && 'text-emerald-500',
          trend === 'stable' && 'text-muted-foreground'
        )}
      >
        <TrendIcon className="w-3 h-3" />
        {!compact && <span className="text-[10px]">{days}d</span>}
      </div>

      {/* Sparkline chart */}
      <div className="flex-1" style={{ minWidth: compact ? '60px' : '100px', height: compact ? '20px' : '30px' }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData}>
            <Line
              type="monotone"
              dataKey="score"
              stroke={getStrokeColor()}
              strokeWidth={1.5}
              dot={false}
              isAnimationActive={false}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (!active || !payload || !payload[0]) return null;
                const data = payload[0].payload;
                return (
                  <div className="bg-popover border border-border rounded-md p-2 shadow-md text-xs">
                    <p className="font-semibold">{data.date}</p>
                    <p className="text-muted-foreground">
                      Score: <span className="font-medium text-foreground">{data.score}</span>
                    </p>
                    <p className="text-muted-foreground">
                      Level: <span className="font-medium text-foreground">{data.level}</span>
                    </p>
                  </div>
                );
              }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
