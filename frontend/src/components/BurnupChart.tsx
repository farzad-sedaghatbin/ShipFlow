import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { cycleService } from '../services/cycleService';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TrendingUp } from 'lucide-react';
import dayjs from 'dayjs';

interface BurnupChartProps {
  cycleId: number;
  cycleName: string;
}

export function BurnupChart({ cycleId, cycleName }: BurnupChartProps) {
  const { t } = useTranslation();

  const { data, isLoading } = useQuery({
    queryKey: ['burnup', cycleId],
    queryFn: () => cycleService.getBurnup(cycleId).then((r) => r.data),
    enabled: cycleId > 0,
  });

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-base">
          <TrendingUp className="h-4 w-4 text-primary" />
          {t('scrumReports.burnup.title')} — {cycleName}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : !data || data.length === 0 ? (
          <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            {t('scrumReports.burnup.noData')}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis
                dataKey="date"
                tickFormatter={(v) => dayjs(v).format('MMM D')}
                tick={{ fontSize: 12 }}
              />
              <YAxis
                label={{
                  value: t('scrumReports.burnup.storyPoints'),
                  angle: -90,
                  position: 'insideLeft',
                  offset: 10,
                  style: { fontSize: 11 },
                }}
                tick={{ fontSize: 12 }}
                allowDecimals={false}
              />
              <Tooltip
                labelFormatter={(v) => dayjs(String(v)).format('MMM D, YYYY')}
                formatter={(value: number, name: string) => [
                  value,
                  name === 'completedPoints'
                    ? t('scrumReports.burnup.completed')
                    : t('scrumReports.burnup.scope'),
                ]}
              />
              <Legend
                formatter={(value) =>
                  value === 'completedPoints'
                    ? t('scrumReports.burnup.completed')
                    : t('scrumReports.burnup.scope')
                }
              />
              <Line
                type="monotone"
                dataKey="completedPoints"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4 }}
              />
              <Line
                type="monotone"
                dataKey="totalScopePoints"
                stroke="hsl(var(--muted-foreground))"
                strokeWidth={2}
                strokeDasharray="6 3"
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}

export default BurnupChart;
