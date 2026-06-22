import api from './api';

export interface McpUsageSummary {
  totalCalls: number;
  successCalls: number;
  failureCalls: number;
  successRate: number;
  activeUsersLast30Days: number;
  uniqueToolsUsed: number;
}

export interface McpUserUsage {
  username: string;
  email: string;
  totalCalls: number;
  successCalls: number;
  successRate: number;
  lastCalledAt: string | null;
}

export interface McpToolUsage {
  toolName: string;
  totalCalls: number;
  successCalls: number;
  successRate: number;
  uniqueUsers: number;
}

export interface McpUsageTimelinePoint {
  date: string;
  totalCalls: number;
  successCalls: number;
}

export interface McpUsageLogEntry {
  id: number;
  username: string;
  toolName: string;
  success: boolean;
  errorMessage: string | null;
  durationMs: number | null;
  calledAt: string;
  apiKeyPrefix: string | null;
}

export async function getMcpUsageSummary(): Promise<McpUsageSummary> {
  const res = await api.get<McpUsageSummary>('/api/admin/mcp/usage/summary');
  return res.data;
}

export async function getMcpUserUsage(): Promise<McpUserUsage[]> {
  const res = await api.get<McpUserUsage[]>('/api/admin/mcp/usage/by-user');
  return res.data;
}

export async function getMcpToolUsage(): Promise<McpToolUsage[]> {
  const res = await api.get<McpToolUsage[]>('/api/admin/mcp/usage/by-tool');
  return res.data;
}

export async function getMcpUsageTimeline(days = 30): Promise<McpUsageTimelinePoint[]> {
  const res = await api.get<McpUsageTimelinePoint[]>(
    `/api/admin/mcp/usage/timeline?days=${days}`
  );
  return res.data;
}

export async function getMcpRecentLogs(limit = 50): Promise<McpUsageLogEntry[]> {
  const res = await api.get<McpUsageLogEntry[]>(
    `/api/admin/mcp/usage/recent?limit=${limit}`
  );
  return res.data;
}
