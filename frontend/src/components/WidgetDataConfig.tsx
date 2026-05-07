import React from 'react';
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Combobox } from '../components/ui/combobox';
import { DashboardWidgetConfig } from '../types/customDashboard';
import { CustomMetric } from '../types/metrics';

export type WidgetDataSourceType = 'METRIC' | 'PITCH_LIST' | 'TASK_LIST' | 'BUG_LIST' | 'CYCLE_SUMMARY' | 'TEAM_STATS';

export interface WidgetDataFilter {
  field: string;
  operator: 'EQUALS' | 'NOT_EQUALS' | 'GREATER_THAN' | 'LESS_THAN' | 'CONTAINS' | 'IN';
  value: any;
}

export interface WidgetDataConfig {
  sourceType: WidgetDataSourceType;
  metricId?: number;
  filters?: WidgetDataFilter[];
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  limit?: number;
  refreshInterval?: number; // seconds
}

interface WidgetDataConfigProps {
  widget?: DashboardWidgetConfig;
  metrics?: CustomMetric[];
  onConfigChange: (config: WidgetDataConfig) => void;
  onCancel: () => void;
}

export default function WidgetDataConfig({ 
  widget, 
  metrics = [], 
  onConfigChange, 
  onCancel 
}: WidgetDataConfigProps) {
  const [sourceType, setSourceType] = React.useState<WidgetDataSourceType>(
    (widget?.config as any)?.sourceType || 'METRIC'
  );
  const [metricId, setMetricId] = React.useState<number | undefined>(
    widget?.metricId || undefined
  );
  const [filters, setFilters] = React.useState<WidgetDataFilter[]>(
    (widget?.config as any)?.filters || []
  );
  const [sortBy, setSortBy] = React.useState<string>((widget?.config as any)?.sortBy || '');
  const [sortOrder, setSortOrder] = React.useState<'ASC' | 'DESC'>(
    (widget?.config as any)?.sortOrder || 'DESC'
  );
  const [limit, setLimit] = React.useState<number>((widget?.config as any)?.limit || 10);
  const [refreshInterval, setRefreshInterval] = React.useState<number>(
    (widget?.config as any)?.refreshInterval || 60
  );

  const handleSave = () => {
    const config: WidgetDataConfig = {
      sourceType,
      metricId: sourceType === 'METRIC' ? metricId : undefined,
      filters: filters.length > 0 ? filters : undefined,
      sortBy: sortBy || undefined,
      sortOrder,
      limit,
      refreshInterval,
    };
    onConfigChange(config);
  };

  const addFilter = () => {
    setFilters([...filters, { field: '', operator: 'EQUALS', value: '' }]);
  };

  const updateFilter = (index: number, updates: Partial<WidgetDataFilter>) => {
    const newFilters = [...filters];
    newFilters[index] = { ...newFilters[index], ...updates };
    setFilters(newFilters);
  };

  const removeFilter = (index: number) => {
    setFilters(filters.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-6">
      <div>
        <Label>Data Source Type</Label>
        <Combobox
          options={[
            { value: 'METRIC', label: 'Metric Value' },
            { value: 'PITCH_LIST', label: 'Pitch List' },
            { value: 'TASK_LIST', label: 'Task List' },
            { value: 'BUG_LIST', label: 'Bug Report List' },
            { value: 'CYCLE_SUMMARY', label: 'Cycle Summary' },
            { value: 'TEAM_STATS', label: 'Team Statistics' }
          ]}
          value={sourceType}
          onValueChange={(value) => setSourceType(value as WidgetDataSourceType)}
          placeholder="Select data source"
        />
      </div>

      {sourceType === 'METRIC' && (
        <div>
          <Label>Select Metric</Label>
          <Combobox
            options={metrics.map(metric => ({ value: metric.id.toString(), label: metric.name }))}
            value={metricId?.toString()}
            onValueChange={(value) => setMetricId(parseInt(value))}
            placeholder="Choose a metric"
            searchPlaceholder="Search metrics..."
          />
        </div>
      )}

      <div>
        <div className="flex items-center justify-between mb-2">
          <Label>Filters</Label>
          <Button variant="outline" size="sm" onClick={addFilter}>
            Add Filter
          </Button>
        </div>
        {filters.map((filter, index) => (
          <div key={index} className="flex gap-2 mb-2">
            <Input
              placeholder="Field name"
              value={filter.field}
              onChange={(e) => updateFilter(index, { field: e.target.value })}
              className="flex-1"
            />
            <Combobox
              options={[
                { value: 'EQUALS', label: 'Equals' },
                { value: 'NOT_EQUALS', label: 'Not Equals' },
                { value: 'GREATER_THAN', label: 'Greater Than' },
                { value: 'LESS_THAN', label: 'Less Than' },
                { value: 'CONTAINS', label: 'Contains' },
                { value: 'IN', label: 'In List' }
              ]}
              value={filter.operator}
              onValueChange={(value) => updateFilter(index, { operator: value as any })}
              placeholder="Operator"
              triggerClassName="w-40"
            />
            <Input
              placeholder="Value"
              value={filter.value}
              onChange={(e) => updateFilter(index, { value: e.target.value })}
              className="flex-1"
            />
            <Button variant="ghost" size="sm" onClick={() => removeFilter(index)}>
              ✕
            </Button>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <Label>Sort By</Label>
          <Input
            placeholder="Field name"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
          />
        </div>
        <div>
          <Label>Sort Order</Label>
          <Combobox
            options={[
              { value: 'ASC', label: 'Ascending' },
              { value: 'DESC', label: 'Descending' }
            ]}
            value={sortOrder}
            onValueChange={(value) => setSortOrder(value as 'ASC' | 'DESC')}
            placeholder="Select order"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <Label>Limit</Label>
          <Input
            type="number"
            value={limit}
            onChange={(e) => setLimit(parseInt(e.target.value) || 10)}
            min={1}
            max={100}
          />
        </div>
        <div>
          <Label>Refresh Interval (seconds)</Label>
          <Input
            type="number"
            value={refreshInterval}
            onChange={(e) => setRefreshInterval(parseInt(e.target.value) || 60)}
            min={10}
            max={3600}
          />
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-4 border-t">
        <Button variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button onClick={handleSave}>
          Save Configuration
        </Button>
      </div>
    </div>
  );
}
