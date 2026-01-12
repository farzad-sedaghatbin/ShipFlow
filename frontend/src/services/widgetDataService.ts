import api from './api';
import { WidgetDataConfig } from '../components/WidgetDataConfig';

export interface WidgetData {
  type: 'METRIC' | 'LIST' | 'CHART' | 'TABLE';
  data: any;
  metadata?: {
    total?: number;
    lastUpdated?: string;
    trend?: 'UP' | 'DOWN' | 'STABLE';
  };
}

class WidgetDataService {
  /**
   * Fetch data for a widget based on its configuration
   * @param config Widget data source configuration
   * @param userContextFilter When true, filter data to current user's context
   */
  async fetchWidgetData(config: WidgetDataConfig, userContextFilter: boolean = false): Promise<WidgetData> {
    const { sourceType, metricId, filters, sortBy, sortOrder, limit } = config;

    switch (sourceType) {
      case 'METRIC':
        return this.fetchMetricData(metricId!, filters, userContextFilter);
      
      case 'PITCH_LIST':
        return this.fetchPitchList(filters, sortBy, sortOrder, limit, userContextFilter);
      
      case 'TASK_LIST':
        return this.fetchTaskList(filters, sortBy, sortOrder, limit, userContextFilter);
      
      case 'CYCLE_SUMMARY':
        return this.fetchCycleSummary(filters, userContextFilter);
      
      case 'TEAM_STATS':
        return this.fetchTeamStats(filters, userContextFilter);
      
      default:
        throw new Error(`Unknown source type: ${sourceType}`);
    }
  }

  private async fetchMetricData(metricId: number, filters?: any[], _userContextFilter: boolean = false): Promise<WidgetData> {
    try {
      // Calculate current value
      const valueResponse = await api.post(`/metrics/custom/${metricId}/calculate`, {
        filters: filters || []
      });

      // Get historical data for trend
      const historyResponse = await api.get(`/metrics/custom/${metricId}/history`, {
        params: { limit: 10 }
      });

      const currentValue = valueResponse.data.value;
      const history = historyResponse.data;

      // Calculate trend
      let trend: 'UP' | 'DOWN' | 'STABLE' = 'STABLE';
      if (history.length >= 2) {
        const previous = history[history.length - 2].value;
        if (currentValue > previous) trend = 'UP';
        else if (currentValue < previous) trend = 'DOWN';
      }

      return {
        type: 'METRIC',
        data: {
          value: currentValue,
          history: history.map((h: any) => ({
            timestamp: h.timestamp,
            value: h.value
          }))
        },
        metadata: {
          lastUpdated: new Date().toISOString(),
          trend
        }
      };
    } catch (error) {
      console.error(`Failed to fetch metric data for metricId: ${metricId}`, error);
      throw new Error(`Failed to fetch metric data for metricId: ${metricId}`, { cause: error });
    }
  }

  private async fetchPitchList(
    filters?: any[], 
    sortBy?: string, 
    sortOrder?: string, 
    limit?: number,
    _userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const response = await api.get('/pitches', {
        params: {
          filters: JSON.stringify(filters || []),
          sortBy,
          sortOrder,
          limit: limit || 10
        }
      });

      return {
        type: 'LIST',
        data: response.data,
        metadata: {
          total: response.data.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Failed to fetch pitch list', error);
      throw new Error('Failed to fetch pitch list', { cause: error });
    }
  }

  private async fetchTaskList(
    filters?: any[],
    sortBy?: string,
    sortOrder?: string,
    limit?: number,
    userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      // Tasks API uses pagination, not filters
      const response = await api.get('/tasks', {
        params: {
          page: 0,
          size: limit || 10,
          sortBy: sortBy || 'createdAt',
          sortOrder: sortOrder || 'desc'
        }
      });

      // Response is a Page object with content array
      const tasks = response.data.content || [];
      
      // Apply user context filter first (if enabled)
      let filteredTasks = tasks;
      if (userContextFilter) {
        // Get current user info from auth context (stored in localStorage)
        const userStr = localStorage.getItem('user');
        if (userStr) {
          const user = JSON.parse(userStr);
          // Filter to tasks assigned to current user
          filteredTasks = tasks.filter((task: any) => 
            task.assigneeId === user.id || task.createdById === user.id
          );
        }
      }
      
      // Apply additional filters if provided
      if (filters && filters.length > 0) {
        filteredTasks = this.applyFilters(filteredTasks, filters);
      }

      return {
        type: 'TABLE',
        data: filteredTasks,
        metadata: {
          total: response.data.totalElements || filteredTasks.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching task list:', error);
      return {
        type: 'TABLE',
        data: [],
        metadata: {
          total: 0,
          lastUpdated: new Date().toISOString()
        }
      };
    }
  }

  private async fetchCycleSummary(_filters?: any[], userContextFilter: boolean = false): Promise<WidgetData> {
    try {
      // Use the existing cycles endpoint
      const response = await api.get('/cycles');
      
      // Transform to table format with summary information
      let cycles = Array.isArray(response.data) ? response.data : [];
      
      // Apply user context filter (if enabled)
      if (userContextFilter) {
        // Note: Cycle-level user filtering is complex and would require
        // fetching user's team assignments and checking cycle relationships.
        // For now, we show all cycles regardless of user context.
        // In a future enhancement, this could filter to cycles where the user
        // has assigned teams or active pitches.
      }
      
      const summaryData = cycles.map((cycle: any) => ({
        id: cycle.id,
        name: cycle.name,
        phase: cycle.phase,
        startDate: new Date(cycle.startDate).toLocaleDateString(),
        endDate: new Date(cycle.endDate).toLocaleDateString(),
        status: cycle.phase === 'BUILD' ? 'Active' : cycle.phase
      }));

      return {
        type: 'TABLE',
        data: summaryData,
        metadata: {
          total: summaryData.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching cycle summary:', error);
      // Return empty data on error
      return {
        type: 'TABLE',
        data: [],
        metadata: {
          total: 0,
          lastUpdated: new Date().toISOString()
        }
      };
    }
  }

  private async fetchTeamStats(_filters?: any[], userContextFilter: boolean = false): Promise<WidgetData> {
    try {
      // Use the existing teams endpoint
      const response = await api.get('/teams');
      
      // Transform to table format with team statistics
      let teams = Array.isArray(response.data) ? response.data : [];
      
      // Apply user context filter (if enabled)
      if (userContextFilter) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
          const user = JSON.parse(userStr);
          // Filter to teams where user is assigned
          teams = teams.filter((team: any) => 
            team.assignments?.some((assignment: any) => assignment.userId === user.id)
          );
        }
      }
      
      const statsData = teams.map((team: any) => ({
        id: team.id,
        name: team.name,
        members: team.assignments?.length || 0,
        cycle: team.cycleName || 'Not assigned',
        project: team.projectName || 'N/A'
      }));

      return {
        type: 'TABLE',
        data: statsData,
        metadata: {
          total: statsData.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching team stats:', error);
      // Return empty data on error
      return {
        type: 'TABLE',
        data: [],
        metadata: {
          total: 0,
          lastUpdated: new Date().toISOString()
        }
      };
    }
  }

  /**
   * Transform raw data based on widget type and configuration
   */
  transformData(rawData: any, widgetType: string, config: any): any {
    switch (widgetType) {
      case 'KPI':
        return this.transformForKPI(rawData);
      
      case 'LINE_CHART':
      case 'BAR_CHART':
      case 'PIE_CHART':
        return this.transformForChart(rawData, widgetType);
      
      case 'TABLE':
        return this.transformForTable(rawData, config);
      
      case 'LIST':
        return this.transformForList(rawData, config);
      
      default:
        return rawData;
    }
  }

  private transformForKPI(data: any): any {
    if (data.type === 'METRIC') {
      return {
        value: data.data.value,
        trend: data.metadata?.trend,
        sparkline: data.data.history?.slice(-7).map((h: any) => h.value) || []
      };
    }
    return data;
  }

  private transformForChart(data: any, _chartType: string): any {
    if (data.type === 'METRIC' && data.data.history) {
      return {
        labels: data.data.history.map((h: any) => new Date(h.timestamp).toLocaleDateString()),
        datasets: [{
          label: 'Metric Value',
          data: data.data.history.map((h: any) => h.value)
        }]
      };
    }
    return data;
  }

  private transformForTable(data: any, config: any): any {
    if (Array.isArray(data.data)) {
      return {
        columns: config.columns || this.extractColumns(data.data[0]),
        rows: data.data
      };
    }
    return data;
  }

  private transformForList(data: any, config: any): any {
    if (Array.isArray(data.data)) {
      return {
        items: data.data.map((item: any) => ({
          id: item.id,
          title: item.name || item.title,
          subtitle: item.description || item.status,
          metadata: this.extractMetadata(item, config.metadataFields)
        }))
      };
    }
    return data;
  }

  private extractColumns(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  private extractMetadata(obj: any, fields?: string[]): any {
    if (!fields) return {};
    return fields.reduce((acc, field) => {
      acc[field] = obj[field];
      return acc;
    }, {} as any);
  }

  /**
   * Apply client-side filters to data
   */
  private applyFilters(data: any[], filters: any[]): any[] {
    return data.filter(item => {
      return filters.every(filter => {
        const value = item[filter.field];
        const filterValue = filter.value;

        switch (filter.operator) {
          case 'equals':
            return value === filterValue;
          case 'not_equals':
            return value !== filterValue;
          case 'in':
            return Array.isArray(filterValue) && filterValue.includes(value);
          case 'contains':
            return String(value).toLowerCase().includes(String(filterValue).toLowerCase());
          case 'greater_than':
            return value > filterValue;
          case 'less_than':
            return value < filterValue;
          default:
            return true;
        }
      });
    });
  }
}

export const widgetDataService = new WidgetDataService();
export default widgetDataService;
