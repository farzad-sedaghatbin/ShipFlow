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
      
      case 'BUG_LIST':
        return this.fetchBugList(filters, sortBy, sortOrder, limit, userContextFilter);
      
      case 'CYCLE_SUMMARY':
        return this.fetchCycleSummary(filters, userContextFilter);
      
      case 'TEAM_STATS':
        return this.fetchTeamStats(filters, userContextFilter);

      case 'UNSHAPED_PITCHES':
        return this.fetchUnshapedPitches(filters, sortBy, sortOrder, limit, userContextFilter);

      case 'STALE_BUGS':
        return this.fetchStaleBugs(filters, sortBy, sortOrder, limit, userContextFilter);

      case 'HIGH_PRIORITY_TASKS':
        return this.fetchHighPriorityTasks(filters, sortBy, sortOrder, limit, userContextFilter);

      case 'AT_RISK_EPICS':
        return this.fetchAtRiskEpics(filters, sortBy, sortOrder, limit, userContextFilter);

      case 'OVERDUE_TASKS':
        return this.fetchOverdueTasks(filters, sortBy, sortOrder, limit, userContextFilter);

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
      throw new Error(`Failed to fetch metric data for metricId: ${metricId}`);
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
      throw new Error('Failed to fetch pitch list');
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
          let user: any;
          try {
            user = JSON.parse(userStr);
          } catch (e) {
            console.warn('Invalid user data in localStorage, skipping user context filter', e);
          }
          if (user && user.id !== undefined && user.id !== null) {
            // Filter to tasks assigned to or created by current user
            filteredTasks = tasks.filter((task: any) => 
              task.assigneeId === user.id || task.createdById === user.id
            );
          }
        }
      }
      
      // Apply additional filters if provided
      if (filters && filters.length > 0) {
        filteredTasks = this.applyFilters(filteredTasks, filters);
      }

      // Use filtered count when client-side filtering is applied
      const hasClientFiltering = userContextFilter || (filters && filters.length > 0);
      return {
        type: 'TABLE',
        data: filteredTasks,
        metadata: {
          total: hasClientFiltering ? filteredTasks.length : (response.data.totalElements || filteredTasks.length),
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

  private async fetchBugList(
    filters?: any[],
    sortBy?: string,
    sortOrder?: string,
    limit?: number,
    userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      // Bug reports API uses pagination
      const response = await api.get('/qa/bug-reports', {
        params: {
          page: 0,
          size: limit || 10,
          sortBy: sortBy || 'createdAt',
          sortOrder: sortOrder || 'desc'
        }
      });

      // Response is a Page object with content array
      const bugReports = response.data.content || [];
      
      // Apply user context filter first (if enabled)
      let filteredBugs = bugReports;
      if (userContextFilter) {
        // Get current user info from auth context (stored in localStorage)
        const userStr = localStorage.getItem('user');
        if (userStr) {
          let user: any;
          try {
            user = JSON.parse(userStr);
          } catch (e) {
            console.warn('Invalid user data in localStorage, skipping user context filter', e);
          }
          if (user && user.id !== undefined && user.id !== null) {
            // Filter to bug reports assigned to or reported by current user
            filteredBugs = bugReports.filter((bug: any) => 
              bug.assigneeId === user.id || bug.reportedById === user.id
            );
          }
        }
      }
      
      // Apply additional filters if provided
      if (filters && filters.length > 0) {
        filteredBugs = this.applyFilters(filteredBugs, filters);
      }

      // Use filtered count when client-side filtering is applied
      const hasClientFiltering = userContextFilter || (filters && filters.length > 0);
      return {
        type: 'TABLE',
        data: filteredBugs,
        metadata: {
          total: hasClientFiltering ? filteredBugs.length : (response.data.totalElements || filteredBugs.length),
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching bug report list:', error);
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
          let user: any;
          try {
            user = JSON.parse(userStr);
          } catch (e) {
            console.warn('Invalid user data in localStorage, skipping user context filter', e);
          }
          if (user && user.id !== undefined && user.id !== null) {
            // Filter to teams where user is assigned
            teams = teams.filter((team: any) => 
              team.assignments?.some((assignment: any) => assignment.userId === user.id)
            );
          }
        }
      }
      
      const statsData = teams.map((team: any) => ({
        id: team.id,
        name: team.name,
        members: team.assignments?.length || 0,
        activeMembers: team.assignments?.filter((a: any) => a.isActive).length || 0
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

  private async fetchUnshapedPitches(
    filters?: any[],
    _sortBy?: string,
    _sortOrder?: string,
    limit?: number,
    _userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const [ideasResp, draftsResp] = await Promise.all([
        api.get('/pitches/ideas'),
        api.get('/pitches/drafts')
      ]);

      const ideas = Array.isArray(ideasResp.data) ? ideasResp.data : [];
      const drafts = Array.isArray(draftsResp.data) ? draftsResp.data : [];
      let unshaped = [...ideas, ...drafts];

      if (filters && filters.length > 0) {
        unshaped = this.applyFilters(unshaped, filters);
      }

      const data = unshaped.slice(0, limit || 20).map((pitch: any) => ({
        id: pitch.id,
        title: pitch.title,
        status: pitch.status,
        priority: pitch.priority || 'N/A',
        epicName: pitch.epicName || pitch.epic?.name || '-',
        createdAt: pitch.createdAt ? new Date(pitch.createdAt).toLocaleDateString() : '-',
        ageDays: pitch.createdAt
          ? Math.floor((Date.now() - new Date(pitch.createdAt).getTime()) / (1000 * 60 * 60 * 24))
          : 0
      }));

      return {
        type: 'TABLE',
        data,
        metadata: {
          total: unshaped.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching unshaped pitches:', error);
      return { type: 'TABLE', data: [], metadata: { total: 0, lastUpdated: new Date().toISOString() } };
    }
  }

  private async fetchStaleBugs(
    filters?: any[],
    _sortBy?: string,
    _sortOrder?: string,
    limit?: number,
    _userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const response = await api.get('/qa/bug-reports', {
        params: { page: 0, size: 100, sortBy: 'createdAt', sortOrder: 'asc' }
      });

      const bugs = response.data.content || [];
      const openStatuses = ['OPEN', 'IN_PROGRESS', 'REOPENED'];
      let staleBugs = bugs.filter((bug: any) => {
        if (!openStatuses.includes(bug.status)) return false;
        if (!bug.createdAt) return false;
        const ageDays = Math.floor((Date.now() - new Date(bug.createdAt).getTime()) / (1000 * 60 * 60 * 24));
        return ageDays >= 3;
      });

      if (filters && filters.length > 0) {
        staleBugs = this.applyFilters(staleBugs, filters);
      }

      const data = staleBugs.slice(0, limit || 20).map((bug: any) => ({
        id: bug.id,
        bugKey: bug.bugKey || `BUG-${bug.id}`,
        title: bug.title,
        severity: bug.severity,
        status: bug.status,
        ageDays: Math.floor((Date.now() - new Date(bug.createdAt).getTime()) / (1000 * 60 * 60 * 24)),
        assignee: bug.assigneeName || bug.assignee?.fullName || 'Unassigned'
      }));

      return {
        type: 'TABLE',
        data,
        metadata: {
          total: staleBugs.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching stale bugs:', error);
      return { type: 'TABLE', data: [], metadata: { total: 0, lastUpdated: new Date().toISOString() } };
    }
  }

  private async fetchHighPriorityTasks(
    filters?: any[],
    _sortBy?: string,
    _sortOrder?: string,
    limit?: number,
    userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const response = await api.get('/tasks', {
        params: { page: 0, size: 50, sortBy: 'priority', sortOrder: 'desc' }
      });

      const tasks = response.data.content || [];
      const highPriorities = ['URGENT', 'HIGH'];
      const activeStatuses = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW'];

      let highPriorityTasks = tasks.filter((task: any) =>
        highPriorities.includes(task.priority) && activeStatuses.includes(task.status)
      );

      if (userContextFilter) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
          try {
            const user = JSON.parse(userStr);
            if (user?.id != null) {
              highPriorityTasks = highPriorityTasks.filter((task: any) =>
                task.assigneeId === user.id || task.createdById === user.id
              );
            }
          } catch (e) { /* ignore */ }
        }
      }

      if (filters && filters.length > 0) {
        highPriorityTasks = this.applyFilters(highPriorityTasks, filters);
      }

      const data = highPriorityTasks.slice(0, limit || 20).map((task: any) => ({
        id: task.id,
        title: task.title,
        priority: task.priority,
        status: task.status,
        assignee: task.assigneeName || 'Unassigned',
        dueDate: task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '-',
        pitchTitle: task.pitchTitle || '-'
      }));

      return {
        type: 'TABLE',
        data,
        metadata: {
          total: highPriorityTasks.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching high priority tasks:', error);
      return { type: 'TABLE', data: [], metadata: { total: 0, lastUpdated: new Date().toISOString() } };
    }
  }

  private async fetchAtRiskEpics(
    filters?: any[],
    _sortBy?: string,
    _sortOrder?: string,
    limit?: number,
    _userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const response = await api.get('/epics');
      const epics = Array.isArray(response.data) ? response.data : (response.data.content || []);

      const activeStatuses = ['PLANNED', 'IN_PROGRESS'];
      const now = new Date();

      let atRiskEpics = epics.filter((epic: any) => {
        if (!activeStatuses.includes(epic.status)) return false;
        if (!epic.targetEndDate) return false;
        const endDate = new Date(epic.targetEndDate);
        const daysUntilDeadline = Math.floor((endDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        const totalPitches = epic.pitchCount || epic.pitches?.length || 0;
        const completedPitches = epic.completedPitchCount || 0;
        const completionRate = totalPitches > 0 ? completedPitches / totalPitches : 0;
        return daysUntilDeadline <= 30 || (daysUntilDeadline <= 60 && completionRate < 0.5);
      });

      if (filters && filters.length > 0) {
        atRiskEpics = this.applyFilters(atRiskEpics, filters);
      }

      const data = atRiskEpics.slice(0, limit || 20).map((epic: any) => {
        const endDate = new Date(epic.targetEndDate);
        const daysLeft = Math.floor((endDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        const totalPitches = epic.pitchCount || epic.pitches?.length || 0;
        const completedPitches = epic.completedPitchCount || 0;
        return {
          id: epic.id,
          name: epic.name,
          status: epic.status,
          priority: epic.priority || 'N/A',
          deadline: endDate.toLocaleDateString(),
          daysLeft: Math.max(0, daysLeft),
          progress: totalPitches > 0 ? `${completedPitches}/${totalPitches}` : '0/0'
        };
      });

      return {
        type: 'TABLE',
        data,
        metadata: {
          total: atRiskEpics.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching at-risk epics:', error);
      return { type: 'TABLE', data: [], metadata: { total: 0, lastUpdated: new Date().toISOString() } };
    }
  }

  private async fetchOverdueTasks(
    filters?: any[],
    _sortBy?: string,
    _sortOrder?: string,
    limit?: number,
    userContextFilter: boolean = false
  ): Promise<WidgetData> {
    try {
      const response = await api.get('/tasks', {
        params: { page: 0, size: 100, sortBy: 'dueDate', sortOrder: 'asc' }
      });

      const tasks = response.data.content || [];
      const now = new Date();
      const incompleteStatuses = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW'];

      let overdueTasks = tasks.filter((task: any) => {
        if (!incompleteStatuses.includes(task.status)) return false;
        if (!task.dueDate) return false;
        return new Date(task.dueDate) < now;
      });

      if (userContextFilter) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
          try {
            const user = JSON.parse(userStr);
            if (user?.id != null) {
              overdueTasks = overdueTasks.filter((task: any) =>
                task.assigneeId === user.id || task.createdById === user.id
              );
            }
          } catch (e) { /* ignore */ }
        }
      }

      if (filters && filters.length > 0) {
        overdueTasks = this.applyFilters(overdueTasks, filters);
      }

      const data = overdueTasks.slice(0, limit || 20).map((task: any) => {
        const dueDate = new Date(task.dueDate);
        const overdueDays = Math.floor((now.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24));
        return {
          id: task.id,
          title: task.title,
          priority: task.priority,
          status: task.status,
          assignee: task.assigneeName || 'Unassigned',
          dueDate: dueDate.toLocaleDateString(),
          overdueDays
        };
      });

      return {
        type: 'TABLE',
        data,
        metadata: {
          total: overdueTasks.length,
          lastUpdated: new Date().toISOString()
        }
      };
    } catch (error) {
      console.error('Error fetching overdue tasks:', error);
      return { type: 'TABLE', data: [], metadata: { total: 0, lastUpdated: new Date().toISOString() } };
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

        const op = (filter.operator || '').toLowerCase();
        switch (op) {
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
