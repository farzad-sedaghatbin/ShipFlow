import React from 'react';
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
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
        <Select value={sourceType} onValueChange={(value) => setSourceType(value as WidgetDataSourceType)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="METRIC">Metric Value</SelectItem>
            <SelectItem value="PITCH_LIST">Pitch List</SelectItem>
            <SelectItem value="TASK_LIST">Task List</SelectItem>
            <SelectItem value="BUG_LIST">Bug Report List</SelectItem>
            <SelectItem value="CYCLE_SUMMARY">Cycle Summary</SelectItem>
            <SelectItem value="TEAM_STATS">Team Statistics</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {sourceType === 'METRIC' && (
        <div>
          <Label>Select Metric</Label>
          <Select 
            value={metricId?.toString()} 
            onValueChange={(value) => setMetricId(parseInt(value))}
          >
            <SelectTrigger>
              <SelectValue placeholder="Choose a metric" />
            </SelectTrigger>
            <SelectContent>
              {metrics.map((metric) => (
                <SelectItem key={metric.id} value={metric.id.toString()}>
                  {metric.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
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
            <Select
              value={filter.operator}
              onValueChange={(value) => updateFilter(index, { operator: value as any })}
            >
              <SelectTrigger className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="EQUALS">Equals</SelectItem>
                <SelectItem value="NOT_EQUALS">Not Equals</SelectItem>
                <SelectItem value="GREATER_THAN">Greater Than</SelectItem>
                <SelectItem value="LESS_THAN">Less Than</SelectItem>
                <SelectItem value="CONTAINS">Contains</SelectItem>
                <SelectItem value="IN">In List</SelectItem>
              </SelectContent>
            </Select>
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
          <Select value={sortOrder} onValueChange={(value) => setSortOrder(value as 'ASC' | 'DESC')}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ASC">Ascending</SelectItem>
              <SelectItem value="DESC">Descending</SelectItem>
            </SelectContent>
          </Select>
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
