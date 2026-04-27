import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const mcpApi = axios.create({
  baseURL: `${API_BASE_URL}/api/admin`,
});

// Add auth interceptor
mcpApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('shipflow_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * MCP Server status for a single provider (GitHub or Figma)
 */
export interface McpServerStatus {
  enabled: boolean;
  configured: boolean;
  serverUrlMasked: string | null;
  timeoutSeconds: number;
}

/**
 * Overall MCP status from environment configuration
 */
export interface McpStatus {
  github: McpServerStatus;
  figma: McpServerStatus;
}

/**
 * MCP settings stored per-organization
 */
export interface McpOrganizationSettings {
  // Figma settings
  hasFigmaAccessToken: boolean;
  // GitHub settings
  hasGithubAccessToken: boolean;
}

/**
 * Request to update MCP organization settings
 */
export interface UpdateMcpSettingsRequest {
  // Figma settings
  figmaAccessToken?: string;  // Set to update, empty to clear, omit to leave unchanged
  // GitHub settings
  githubAccessToken?: string;  // Set to update, empty to clear, omit to leave unchanged
}

/**
 * Test connection result
 */
export interface McpTestResult {
  success: boolean;
  message: string;
  details?: string;
}

/**
 * Get MCP server status (system-wide configuration from environment)
 */
export async function getMcpStatus(): Promise<McpStatus> {
  const response = await mcpApi.get<McpStatus>('/settings/mcp-status');
  return response.data;
}

/**
 * Get MCP organization settings
 */
export async function getMcpSettings(): Promise<McpOrganizationSettings> {
  const response = await mcpApi.get('/settings');
  const data = response.data;
  return {
    hasFigmaAccessToken: data.hasFigmaAccessToken ?? false,
    hasGithubAccessToken: data.hasGithubAccessToken ?? false,
  };
}

/**
 * Update MCP organization settings
 */
export async function updateMcpSettings(request: UpdateMcpSettingsRequest): Promise<McpOrganizationSettings> {
  const response = await mcpApi.put('/settings', request);
  const data = response.data;
  return {
    hasFigmaAccessToken: data.hasFigmaAccessToken ?? false,
    hasGithubAccessToken: data.hasGithubAccessToken ?? false,
  };
}

export default {
  getMcpStatus,
  getMcpSettings,
  updateMcpSettings,
};
