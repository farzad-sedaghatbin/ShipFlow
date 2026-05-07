import { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Combobox } from '../components/ui/combobox';
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
        <Combobox
          options={[
            { value: 'KPI', label: 'KPI Card' },
            { value: 'LINE_CHART', label: 'Line Chart' },
            { value: 'BAR_CHART', label: 'Bar Chart' },
            { value: 'PIE_CHART', label: 'Pie Chart' },
            { value: 'AREA_CHART', label: 'Area Chart' },
            { value: 'TABLE', label: 'Data Table' }
          ]}
          value={widgetType}
          onValueChange={setWidgetType}
          placeholder="Select widget type"
          triggerClassName="mt-1"
        />
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
