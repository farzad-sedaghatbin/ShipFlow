
import {
  LineChart as RechartsLineChart,
  BarChart as RechartsBarChart,
  PieChart as RechartsPieChart,
  AreaChart as RechartsAreaChart,
  Line,
  Bar,
  Pie,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { ChartConfig } from './ChartConfigPanel';

interface ChartProps {
  config: ChartConfig;
  data: any[];
  loading?: boolean;
}

export function MetricChart({ config, data, loading = false }: ChartProps) {
  if (loading) {
    return (
      <Card className="h-full">
        <CardContent className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
        </CardContent>
      </Card>
    );
  }

  if (!data || data.length === 0) {
    return (
      <Card className="h-full">
        <CardContent className="flex items-center justify-center h-64 text-muted-foreground">
          No data available
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="h-full">
      {config.title && (
        <CardHeader>
          <CardTitle>{config.title}</CardTitle>
        </CardHeader>
      )}
      <CardContent>
        <ResponsiveContainer width="100%" height={300}>
          {renderChart(config, data)}
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}

function renderChart(config: ChartConfig, data: any[]) {
  const commonProps = {
    data,
    margin: { top: 10, right: 30, left: 0, bottom: 0 }
  };

  const colors = config.colors || ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

  switch (config.chartType) {
    case 'LINE':
      return (
        <RechartsLineChart {...commonProps}>
          {config.showGrid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis 
            dataKey="name" 
            label={config.xAxisLabel ? { value: config.xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
          />
          <YAxis 
            label={config.yAxisLabel ? { value: config.yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
          />
          <Tooltip />
          {config.showLegend && <Legend />}
          {Object.keys(data[0] || {})
            .filter(key => key !== 'name')
            .map((key, index) => (
              <Line
                key={key}
                type="monotone"
                dataKey={key}
                stroke={colors[index % colors.length]}
                strokeWidth={2}
                dot={{ r: 4 }}
                isAnimationActive={config.animated !== false}
              />
            ))}
        </RechartsLineChart>
      );

    case 'BAR':
      return (
        <RechartsBarChart {...commonProps}>
          {config.showGrid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis 
            dataKey="name"
            label={config.xAxisLabel ? { value: config.xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
          />
          <YAxis 
            label={config.yAxisLabel ? { value: config.yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
          />
          <Tooltip />
          {config.showLegend && <Legend />}
          {Object.keys(data[0] || {})
            .filter(key => key !== 'name')
            .map((key, index) => (
              <Bar
                key={key}
                dataKey={key}
                fill={colors[index % colors.length]}
                stackId={config.stacked ? 'stack' : undefined}
                isAnimationActive={config.animated !== false}
              />
            ))}
        </RechartsBarChart>
      );

    case 'PIE':
      const pieData = Object.keys(data[0] || {})
        .filter(key => key !== 'name')
        .map(key => ({
          name: key,
          value: data.reduce((sum: number, item: any) => sum + (item[key] || 0), 0)
        }));

      return (
        <RechartsPieChart>
          <Pie
            data={pieData}
            cx="50%"
            cy="50%"
            labelLine={config.showDataLabels}
            label={config.showDataLabels ? renderCustomLabel : false}
            outerRadius={100}
            fill="#8884d8"
            dataKey="value"
            isAnimationActive={config.animated !== false}
          >
            {pieData.map((_entry, index) => (
              <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
            ))}
          </Pie>
          <Tooltip />
          {config.showLegend && <Legend />}
        </RechartsPieChart>
      );

    case 'AREA':
      return (
        <RechartsAreaChart {...commonProps}>
          {config.showGrid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis 
            dataKey="name"
            label={config.xAxisLabel ? { value: config.xAxisLabel, position: 'insideBottom', offset: -5 } : undefined}
          />
          <YAxis 
            label={config.yAxisLabel ? { value: config.yAxisLabel, angle: -90, position: 'insideLeft' } : undefined}
          />
          <Tooltip />
          {config.showLegend && <Legend />}
          {Object.keys(data[0] || {})
            .filter(key => key !== 'name')
            .map((key, index) => (
              <Area
                key={key}
                type="monotone"
                dataKey={key}
                stroke={colors[index % colors.length]}
                fill={colors[index % colors.length]}
                fillOpacity={0.6}
                stackId={config.stacked ? 'stack' : undefined}
                isAnimationActive={config.animated !== false}
              />
            ))}
        </RechartsAreaChart>
      );

    default:
      return <div>Unsupported chart type</div>;
  }
}

const renderCustomLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }: any) => {
  const RADIAN = Math.PI / 180;
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);

  return (
    <text 
      x={x} 
      y={y} 
      fill="white" 
      textAnchor={x > cx ? 'start' : 'end'} 
      dominantBaseline="central"
      fontSize={12}
      fontWeight="bold"
    >
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

export default MetricChart;
