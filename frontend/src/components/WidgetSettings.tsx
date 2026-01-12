import { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { DashboardWidgetConfig } from '../types/customDashboard';
import { CustomMetric } from '../types/metrics';
import WidgetDataConfig, { WidgetDataConfig as DataConfig } from './WidgetDataConfig';
import ChartConfigPanel, { ChartConfig } from './ChartConfigPanel';
import LoadingButton from './LoadingButton';

interface WidgetSettingsProps {
  widget: DashboardWidgetConfig;
  metrics: CustomMetric[];
  onSave: (widget: DashboardWidgetConfig) => Promise<void>;
  onCancel: () => void;
}

export default function WidgetSettings({
  widget,
  metrics,
  onSave,
  onCancel
}: WidgetSettingsProps) {
  const [title, setTitle] = useState((widget.config as any)?.title || '');
  const [widgetType, setWidgetType] = useState(widget.widgetType);
  const [dataConfig, setDataConfig] = useState<DataConfig>((widget.config as any) || {});
  const [chartConfig, setChartConfig] = useState<ChartConfig>((widget.config as any) || {});
  const [saving, setSaving] = useState(false);

  const isChartWidget = ['LINE_CHART', 'BAR_CHART', 'PIE_CHART', 'AREA_CHART'].includes(widgetType);

  const handleSave = async () => {
    setSaving(true);
    try {
      const updatedWidget: DashboardWidgetConfig = {
        ...widget,
        widgetType,
        config: {
          ...dataConfig,
          ...(isChartWidget ? chartConfig : {}),
          title
        }
      };

      await onSave(updatedWidget);
    } catch (error) {
      console.error('Error saving widget settings:', error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <Label>Widget Title</Label>
        <Input
          placeholder="Enter widget title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="mt-1"
        />
      </div>

      <div>
        <Label>Widget Type</Label>
        <Select value={widgetType} onValueChange={setWidgetType}>
          <SelectTrigger className="mt-1">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="KPI">KPI Card</SelectItem>
            <SelectItem value="LINE_CHART">Line Chart</SelectItem>
            <SelectItem value="BAR_CHART">Bar Chart</SelectItem>
            <SelectItem value="PIE_CHART">Pie Chart</SelectItem>
            <SelectItem value="AREA_CHART">Area Chart</SelectItem>
            <SelectItem value="TABLE">Data Table</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Tabs defaultValue="data" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="data">Data Source</TabsTrigger>
          {isChartWidget && <TabsTrigger value="chart">Chart Style</TabsTrigger>}
        </TabsList>

        <TabsContent value="data" className="mt-4">
          <WidgetDataConfig
            widget={widget}
            metrics={metrics}
            onConfigChange={setDataConfig}
            onCancel={onCancel}
          />
        </TabsContent>

        {isChartWidget && (
          <TabsContent value="chart" className="mt-4">
            <ChartConfigPanel
              initialConfig={chartConfig}
              onConfigChange={setChartConfig}
              onCancel={onCancel}
            />
          </TabsContent>
        )}
      </Tabs>

      <div className="flex justify-end gap-2 pt-4 border-t">
        <Button variant="outline" onClick={onCancel} disabled={saving}>
          Cancel
        </Button>
        <LoadingButton onClick={handleSave} loading={saving}>
          Save Changes
        </LoadingButton>
      </div>
    </div>
  );
}
