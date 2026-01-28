import { useEffect, useState } from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';

export interface KPIWidgetProps {
  title: string;
  value: number | string;
  format?: 'NUMBER' | 'PERCENTAGE' | 'CURRENCY' | 'DURATION';
  trend?: 'UP' | 'DOWN' | 'STABLE';
  trendValue?: number;
  sparklineData?: number[];
  subtitle?: string;
  loading?: boolean;
}

export default function KPIWidget({
  title,
  value,
  format = 'NUMBER',
  trend,
  trendValue,
  sparklineData,
  subtitle,
  loading = false
}: KPIWidgetProps) {
  const [displayValue, setDisplayValue] = useState<string>('');

  useEffect(() => {
    setDisplayValue(formatValue(value, format));
  }, [value, format]);

  const formatValue = (val: number | string, fmt: string): string => {
    if (typeof val === 'string') return val;

    switch (fmt) {
      case 'PERCENTAGE':
        return `${val.toFixed(1)}%`;
      case 'CURRENCY':
        return new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: 'USD'
        }).format(val);
      case 'DURATION':
        return formatDuration(val);
      case 'NUMBER':
      default:
        return new Intl.NumberFormat('en-US').format(val);
    }
  };

  const formatDuration = (hours: number): string => {
    if (hours < 1) return `${Math.round(hours * 60)}m`;
    if (hours < 24) return `${hours.toFixed(1)}h`;
    const days = Math.floor(hours / 24);
    const remainingHours = hours % 24;
    return remainingHours > 0 
      ? `${days}d ${Math.round(remainingHours)}h` 
      : `${days}d`;
  };

  const getTrendColor = () => {
    switch (trend) {
      case 'UP': return 'text-green-600 dark:text-green-400';
      case 'DOWN': return 'text-red-600 dark:text-red-400';
      default: return 'text-gray-600 dark:text-gray-400';
    }
  };

  const getTrendIcon = () => {
    switch (trend) {
      case 'UP': return <TrendingUp className="h-4 w-4" />;
      case 'DOWN': return <TrendingDown className="h-4 w-4" />;
      default: return <Minus className="h-4 w-4" />;
    }
  };

  return (
    <Card className="h-full">
      <CardHeader className="pb-2">
        <CardDescription className="text-xs">{title}</CardDescription>
        {subtitle && (
          <CardTitle className="text-sm font-normal text-muted-foreground">{subtitle}</CardTitle>
        )}
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex items-center justify-center h-20">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          </div>
        ) : (
          <>
            <div className="flex items-baseline gap-2">
              <div className="text-3xl font-bold tracking-tight">{displayValue}</div>
              {trend && (
                <div className={`flex items-center gap-1 text-sm ${getTrendColor()}`}>
                  {getTrendIcon()}
                  {trendValue !== undefined && (
                    <span>{Math.abs(trendValue).toFixed(1)}%</span>
                  )}
                </div>
              )}
            </div>

            {sparklineData && sparklineData.length > 0 && (
              <div className="mt-4">
                <Sparkline data={sparklineData} trend={trend} />
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}

interface SparklineProps {
  data: number[];
  trend?: 'UP' | 'DOWN' | 'STABLE';
}

function Sparkline({ data, trend }: SparklineProps) {
  if (!data || data.length === 0) return null;

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  
  const width = 200;
  const height = 40;
  const padding = 2;
  
  const points = data.map((value, index) => {
    const x = (index / (data.length - 1)) * (width - padding * 2) + padding;
    const y = height - padding - ((value - min) / range) * (height - padding * 2);
    return `${x},${y}`;
  }).join(' ');

  const getColor = () => {
    switch (trend) {
      case 'UP': return '#10b981'; // green
      case 'DOWN': return '#ef4444'; // red
      default: return '#6b7280'; // gray
    }
  };

  return (
    <svg 
      width={width} 
      height={height} 
      className="w-full" 
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
    >
      <polyline
        points={points}
        fill="none"
        stroke={getColor()}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <polyline
        points={`${padding},${height - padding} ${points} ${width - padding},${height - padding}`}
        fill={getColor()}
        fillOpacity="0.1"
        stroke="none"
      />
    </svg>
  );
}
