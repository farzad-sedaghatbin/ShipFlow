// Custom Metrics Types

export enum MetricDataSource {
  PITCH = 'PITCH',
  CYCLE = 'CYCLE',
  TASK = 'TASK',
  WORK_LOG = 'WORK_LOG',
  TEAM = 'TEAM',
  CUSTOM = 'CUSTOM'
}

export enum MetricAggregationType {
  SUM = 'SUM',
  AVG = 'AVG',
  COUNT = 'COUNT',
  MIN = 'MIN',
  MAX = 'MAX',
  RATIO = 'RATIO',
  PERCENTAGE = 'PERCENTAGE'
}

export enum MetricDisplayFormat {
  NUMBER = 'NUMBER',
  PERCENTAGE = 'PERCENTAGE',
  CURRENCY = 'CURRENCY',
  DURATION = 'DURATION',
  DECIMAL = 'DECIMAL'
}

export interface CustomMetric {
  id: number;
  userId: number;
  name: string;
  description?: string;
  formula: string;
  dataSource: MetricDataSource;
  aggregationType: MetricAggregationType;
  displayFormat: MetricDisplayFormat;
  filters?: string; // JSON string
  
  // Scope fields
  cycleId?: number;
  cycleName?: string;
  pitchId?: number;
  pitchName?: string;
  teamId?: number;
  teamName?: string;
  
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomMetricRequest {
  name: string;
  description?: string;
  formula: string;
  dataSource: MetricDataSource;
  aggregationType: MetricAggregationType;
  displayFormat: MetricDisplayFormat;
  filters?: string;
  
  // Scope fields
  cycleId?: number;
  pitchId?: number;
  teamId?: number;
}

export interface UpdateCustomMetricRequest {
  name?: string;
  description?: string;
  formula?: string;
  dataSource?: MetricDataSource;
  aggregationType?: MetricAggregationType;
  displayFormat?: MetricDisplayFormat;
  filters?: string;
}

export interface MetricValue {
  metricId: number;
  value: number;
  formattedValue: string;
  calculatedAt: string;
  metadata?: Record<string, any>;
}

export interface MetricValidationResult {
  valid: boolean;
  errorMessage?: string;
  formula: string;
}
