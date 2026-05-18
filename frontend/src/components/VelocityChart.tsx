import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import {
  BarChart,
  Bar,
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
import { Gauge } from 'lucide-react';

interface VelocityChartProps {
  projectId: number;
}

export function VelocityChart({ projectId }: VelocityChartProps) {
  const { t } = useTranslation();

  const { data, isLoading } = useQuery({
    queryKey: ['velocity', projectId],
    queryFn: () => cycleService.getVelocity(projectId).then((r) => r.data),
    enabled: projectId > 0,
  });

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-base">
          <Gauge className="h-4 w-4 text-primary" />
          {t('charts.velocity.title')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : !data || data.length === 0 ? (
          <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            {t('charts.velocity.noData')}
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="cycleName" tick={{ fontSize: 12 }} />
              <YAxis
                label={{
                  value: t('charts.velocity.storyPoints'),
                  angle: -90,
                  position: 'insideLeft',
                  offset: 10,
                  style: { fontSize: 11 },
                }}
                tick={{ fontSize: 12 }}
                allowDecimals={false}
              />
              <Tooltip />
              <Legend />
              <Bar
                dataKey="plannedPoints"
                name={t('charts.velocity.planned')}
                fill="hsl(var(--muted-foreground))"
                radius={[4, 4, 0, 0]}
                opacity={0.6}
              />
              <Bar
                dataKey="completedPoints"
                name={t('charts.velocity.completed')}
                fill="hsl(var(--primary))"
                radius={[4, 4, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        )}
      </CardContent>
    </Card>
  );
}

export default VelocityChart;
