import { useState, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate, formatLocalizedDateTime } from '../utils/dateLocalization';
import GridLayout, { Layout } from 'react-grid-layout';
import { DashboardWidgetConfig } from '../types/customDashboard';
import KPIWidget from './KPIWidget';
import MetricChart from './MetricChart';
import TableWidget from './TableWidget';
import { ChartConfig } from './ChartConfigPanel';
import { widgetDataService, WidgetData } from '../services/widgetDataService';
import { Button } from './ui/button';
import { Settings, Trash2, GripVertical } from 'lucide-react';

export interface DashboardGridProps {
  widgets: DashboardWidgetConfig[];
  editable?: boolean;
  userContextFilter?: boolean; // When true, filter widgets to user's context
  onLayoutChange?: (layout: Layout) => void;
  onWidgetRemove?: (widgetId: number) => void;
  onWidgetConfigure?: (widgetId: number) => void;
}

export default function DashboardGrid({
  widgets,
  editable = false,
  userContextFilter = false,
  onLayoutChange,
  onWidgetRemove,
  onWidgetConfigure
}: DashboardGridProps) {
  const { i18n } = useTranslation();
  const [layout, setLayout] = useState<Layout>([]);
  const [widgetDataMap, setWidgetDataMap] = useState<Map<number, WidgetData>>(new Map());
  const [loadingWidgets, setLoadingWidgets] = useState<Set<number>>(new Set());

  // Memoize widget IDs to prevent unnecessary re-renders
  const widgetIds = useMemo(() => 
    widgets.map(w => w.id).filter(Boolean).join(','),
    [widgets]
  );

  useEffect(() => {
    // Initialize layout from widget configurations
    const initialLayout = widgets.map((widget, index) => ({
      i: widget.id?.toString() || `widget-${index}`,
      x: Number(widget.positionX) || 0,
      y: Number(widget.positionY) || 0,
      w: Number(widget.width) || 4,
      h: Number(widget.height) || 2,
      minW: 2,
      minH: 1,
    }));
    setLayout(initialLayout);
  }, [widgetIds]);

  useEffect(() => {
    // Load data for all widgets when widgets or user context filter changes
    widgets.forEach(widget => {
      if (widget.id) {
        loadWidgetData(widget);
      }
    });
  }, [widgetIds, userContextFilter]);

  const loadWidgetData = async (widget: DashboardWidgetConfig) => {
    if (!widget.id) return;

    setLoadingWidgets(prev => new Set(prev).add(widget.id!));

    try {
      const config = widget.config as any;
      if (config && config.sourceType) {
        const data = await widgetDataService.fetchWidgetData(config, userContextFilter);
        setWidgetDataMap(prev => new Map(prev).set(widget.id!, data));
      }
    } catch (error) {
      console.error(`Error loading data for widget ${widget.id}:`, error);
    } finally {
      setLoadingWidgets(prev => {
        const newSet = new Set(prev);
        newSet.delete(widget.id!);
        return newSet;
      });
    }
  };

  const handleLayoutChange = useCallback((newLayout: Layout) => {
    // Only update if layout actually changed and we're in edit mode
    if (!editable) return;
    
    setLayout(prevLayout => {
      // Compare layouts to prevent unnecessary updates
      const layoutChanged = JSON.stringify(newLayout) !== JSON.stringify(prevLayout);
      if (!layoutChanged) return prevLayout;
      
      if (onLayoutChange) {
        onLayoutChange(newLayout);
      }
      
      return newLayout;
    });
  }, [onLayoutChange, editable]);

  const renderWidget = (widget: DashboardWidgetConfig) => {
    if (!widget.id) {
      return <div className="flex items-center justify-center h-full text-muted-foreground">Invalid widget</div>;
    }
    
    const data = widgetDataMap.get(widget.id);
    const loading = loadingWidgets.has(widget.id);
    const config = widget.config as any;

    switch (widget.widgetType) {
      case 'KPI':
        return (
          <KPIWidget
            title={config?.title || 'Metric'}
            value={data?.data?.value || 0}
            format={config?.displayFormat || 'NUMBER'}
            trend={data?.metadata?.trend}
            sparklineData={data?.data?.history?.slice(-7).map((h: any) => h.value)}
            loading={loading}
          />
        );

      case 'LINE_CHART':
      case 'BAR_CHART':
      case 'PIE_CHART':
      case 'AREA_CHART':
        const chartConfig: ChartConfig = {
          chartType: widget.widgetType.replace('_CHART', '') as any,
          ...config
        };
        
        // Transform data for chart
        let chartData = [];
        if (data?.data?.history) {
          chartData = data.data.history.map((h: any) => ({
            name: formatLocalizedDate(new Date(h.timestamp), i18n.language),
            value: h.value
          }));
        }

        return (
          <MetricChart
            config={chartConfig}
            data={chartData}
            loading={loading}
          />
        );

      case 'TABLE':
        // Transform data for table display
        let tableData = [];
        if (data?.data?.history) {
          tableData = data.data.history.map((item: any) => ({
            date: formatLocalizedDate(new Date(item.timestamp), i18n.language),
            time: formatLocalizedDateTime(new Date(item.timestamp), i18n.language).split(' ').slice(1).join(' '), // Get time part
            value: item.value,
            ...item
          }));
        } else if (Array.isArray(data?.data)) {
          tableData = data.data;
        }

        return (
          <TableWidget
            data={tableData}
            title={config?.title || 'Table'}
            loading={loading}
            pageSize={config?.pageSize || 10}
            searchable={config?.searchable !== false}
          />
        );

      default:
        return (
          <div className="p-4 h-full flex items-center justify-center text-muted-foreground">
            Unknown widget type: {widget.widgetType}
          </div>
        );
    }
  };

  return (
    <div className="w-full">
      <GridLayout
        className="layout"
        layout={layout}
        width={1200}
        gridConfig={{
          rowHeight: 100,
          cols: 12
        }}
        dragConfig={{
          enabled: editable,
          handle: ".drag-handle"
        }}
        resizeConfig={{
          enabled: editable
        }}
        onLayoutChange={handleLayoutChange}
      >
        {widgets.map((widget, index) => (
          <div
            key={widget.id?.toString() || `widget-${index}`}
            className="bg-background border rounded-lg shadow-sm relative group"
          >
            {editable && (
              <div className="absolute top-2 right-2 z-10 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <div className="drag-handle cursor-move">
                  <Button variant="ghost" size="icon" className="h-7 w-7">
                    <GripVertical className="h-4 w-4" />
                  </Button>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => widget.id && onWidgetConfigure?.(widget.id)}
                  disabled={!widget.id}
                >
                  <Settings className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive hover:text-destructive"
                  onClick={() => widget.id && onWidgetRemove?.(widget.id)}
                  disabled={!widget.id}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            )}
            <div className="h-full">
              {renderWidget(widget)}
            </div>
          </div>
        ))}
      </GridLayout>
    </div>
  );
}
