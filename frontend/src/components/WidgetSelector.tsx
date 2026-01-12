import React, { useState } from 'react';
import { Plus, BarChart3, PieChart, LineChart, Activity, Table2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Button } from '../components/ui/button';
import { Card, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { CustomMetric } from '../types/metrics';
import { AddWidgetRequest } from '../types/customDashboard';
import LoadingButton from './LoadingButton';

export type WidgetType = 'KPI' | 'LINE_CHART' | 'BAR_CHART' | 'PIE_CHART' | 'AREA_CHART' | 'TABLE';

interface WidgetTypeOption {
  type: WidgetType;
  name: string;
  description: string;
  icon: React.ElementType;
}

const WIDGET_TYPES: WidgetTypeOption[] = [
  {
    type: 'KPI',
    name: 'KPI Card',
    description: 'Single metric value with trend indicator',
    icon: Activity
  },
  {
    type: 'LINE_CHART',
    name: 'Line Chart',
    description: 'Show trends over time',
    icon: LineChart
  },
  {
    type: 'BAR_CHART',
    name: 'Bar Chart',
    description: 'Compare values across categories',
    icon: BarChart3
  },
  {
    type: 'PIE_CHART',
    name: 'Pie Chart',
    description: 'Show proportions and percentages',
    icon: PieChart
  },
  {
    type: 'AREA_CHART',
    name: 'Area Chart',
    description: 'Visualize cumulative totals',
    icon: Activity
  },
  {
    type: 'TABLE',
    name: 'Data Table',
    description: 'Display detailed data in rows',
    icon: Table2
  }
];

interface WidgetSelectorProps {
  open: boolean;
  onClose: () => void;
  metrics: CustomMetric[];
  onWidgetAdd: (request: AddWidgetRequest) => Promise<void>;
}

export default function WidgetSelector({
  open,
  onClose,
  metrics,
  onWidgetAdd
}: WidgetSelectorProps) {
  const [selectedType, setSelectedType] = useState<WidgetType | null>(null);
  const [selectedMetricId, setSelectedMetricId] = useState<number | null>(null);
  const [adding, setAdding] = useState(false);

  const handleAdd = async () => {
    if (!selectedType) return;

    setAdding(true);
    try {
      const request: AddWidgetRequest = {
        widgetType: selectedType,
        metricId: selectedMetricId || undefined,
        positionX: 0,
        positionY: 0,
        width: selectedType === 'KPI' ? 3 : 6,
        height: selectedType === 'KPI' ? 2 : 4,
        config: {
          sourceType: selectedMetricId ? 'METRIC' : 'PITCH_LIST',
          metricId: selectedMetricId || undefined,
          title: selectedMetricId 
            ? metrics.find(m => m.id === selectedMetricId)?.name 
            : 'New Widget'
        }
      };

      await onWidgetAdd(request);
      handleClose();
    } catch (error) {
      console.error('Error adding widget:', error);
    } finally {
      setAdding(false);
    }
  };

  const handleClose = () => {
    setSelectedType(null);
    setSelectedMetricId(null);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add Widget to Dashboard</DialogTitle>
          <DialogDescription>
            Choose a widget type and configure its data source
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Step 1: Select Widget Type */}
          <div>
            <h3 className="text-sm font-medium mb-3">Select Widget Type</h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {WIDGET_TYPES.map((widgetType) => {
                const Icon = widgetType.icon;
                const isSelected = selectedType === widgetType.type;
                
                return (
                  <Card
                    key={widgetType.type}
                    className={`cursor-pointer transition-colors ${
                      isSelected 
                        ? 'border-primary bg-primary/5' 
                        : 'hover:border-primary/50'
                    }`}
                    onClick={() => setSelectedType(widgetType.type)}
                  >
                    <CardHeader className="p-4">
                      <div className="flex items-start gap-3">
                        <div className={`p-2 rounded-lg ${
                          isSelected ? 'bg-primary text-primary-foreground' : 'bg-muted'
                        }`}>
                          <Icon className="h-5 w-5" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <CardTitle className="text-sm">{widgetType.name}</CardTitle>
                          <CardDescription className="text-xs mt-1">
                            {widgetType.description}
                          </CardDescription>
                        </div>
                      </div>
                    </CardHeader>
                  </Card>
                );
              })}
            </div>
          </div>

          {/* Step 2: Select Metric (if applicable) */}
          {selectedType && selectedType !== 'TABLE' && (
            <div>
              <h3 className="text-sm font-medium mb-3">Select Metric (Optional)</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-60 overflow-y-auto">
                <Card
                  className={`cursor-pointer transition-colors ${
                    selectedMetricId === null 
                      ? 'border-primary bg-primary/5' 
                      : 'hover:border-primary/50'
                  }`}
                  onClick={() => setSelectedMetricId(null)}
                >
                  <CardHeader className="p-4">
                    <CardTitle className="text-sm">No Metric</CardTitle>
                    <CardDescription className="text-xs">
                      Configure data source manually
                    </CardDescription>
                  </CardHeader>
                </Card>

                {metrics.map((metric) => (
                  <Card
                    key={metric.id}
                    className={`cursor-pointer transition-colors ${
                      selectedMetricId === metric.id 
                        ? 'border-primary bg-primary/5' 
                        : 'hover:border-primary/50'
                    }`}
                    onClick={() => setSelectedMetricId(metric.id)}
                  >
                    <CardHeader className="p-4">
                      <CardTitle className="text-sm">{metric.name}</CardTitle>
                      <CardDescription className="text-xs line-clamp-2">
                        {metric.description}
                      </CardDescription>
                    </CardHeader>
                  </Card>
                ))}
              </div>

              {metrics.length === 0 && (
                <div className="text-sm text-muted-foreground text-center py-8">
                  No metrics available. Create a metric first.
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={adding}>
            Cancel
          </Button>
          <LoadingButton
            onClick={handleAdd}
            disabled={!selectedType}
            loading={adding}
          >
            <Plus className="h-4 w-4 mr-2" />
            Add Widget
          </LoadingButton>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
