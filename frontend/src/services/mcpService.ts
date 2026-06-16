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
  notion: McpServerStatus;
  confluence: McpServerStatus;
}

/**
 * MCP settings stored per-organization
 */
export interface McpOrganizationSettings {
  // Figma settings
  hasFigmaAccessToken: boolean;
  // GitHub settings
  hasGithubAccessToken: boolean;
  // Notion settings
  hasNotionAccessToken: boolean;
  // Confluence settings
  hasConfluenceAccessToken: boolean;
  defaultConfluenceDomain?: string;
  defaultConfluenceSpaceKey?: string;
  // SharePoint Graph API settings
  hasSharepointClientSecret: boolean;
  sharepointTenantId?: string;
  sharepointClientId?: string;
  sharepointSiteUrl?: string;
  // Built-in MCP server runtime toggle (effective values: DB override else env default)
  mcpServerEnabled: boolean;
  mcpServerWriteEnabled: boolean;
}

/**
 * Request to update MCP organization settings
 */
export interface UpdateMcpSettingsRequest {
  // Figma settings
  figmaAccessToken?: string;  // Set to update, empty to clear, omit to leave unchanged
  // GitHub settings
  githubAccessToken?: string;  // Set to update, empty to clear, omit to leave unchanged
  // Notion settings
  notionAccessToken?: string;
  // Confluence settings
  confluenceAccessToken?: string;
  defaultConfluenceDomain?: string;
  defaultConfluenceSpaceKey?: string;
  // SharePoint Graph API settings
  sharepointTenantId?: string;
  sharepointClientId?: string;
  sharepointClientSecret?: string;
  sharepointSiteUrl?: string;
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
 * Effective runtime state of the built-in ShipFlow MCP server.
 * These are a subset of OrganizationSettingsDTO. The values are the effective
 * settings (DB override if set, otherwise the environment-variable default).
 */
export interface McpServerSettings {
  mcpServerEnabled: boolean;
  mcpServerWriteEnabled: boolean;
}

/**
 * Partial update for the MCP server runtime toggle. Omitted fields are left
 * unchanged by the backend (null is ignored).
 */
export interface McpServerToggleInput {
  mcpServerEnabled?: boolean;
  mcpServerWriteEnabled?: boolean;
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
    hasNotionAccessToken: data.hasNotionAccessToken ?? false,
    hasConfluenceAccessToken: data.hasConfluenceAccessToken ?? false,
    defaultConfluenceDomain: data.defaultConfluenceDomain ?? undefined,
    defaultConfluenceSpaceKey: data.defaultConfluenceSpaceKey ?? undefined,
    hasSharepointClientSecret: data.hasSharepointClientSecret ?? false,
    sharepointTenantId: data.sharepointTenantId ?? undefined,
    sharepointClientId: data.sharepointClientId ?? undefined,
    sharepointSiteUrl: data.sharepointSiteUrl ?? undefined,
    mcpServerEnabled: Boolean(data.mcpServerEnabled),
    mcpServerWriteEnabled: Boolean(data.mcpServerWriteEnabled),
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
    hasNotionAccessToken: data.hasNotionAccessToken ?? false,
    hasConfluenceAccessToken: data.hasConfluenceAccessToken ?? false,
    defaultConfluenceDomain: data.defaultConfluenceDomain ?? undefined,
    defaultConfluenceSpaceKey: data.defaultConfluenceSpaceKey ?? undefined,
    hasSharepointClientSecret: data.hasSharepointClientSecret ?? false,
    sharepointTenantId: data.sharepointTenantId ?? undefined,
    sharepointClientId: data.sharepointClientId ?? undefined,
    sharepointSiteUrl: data.sharepointSiteUrl ?? undefined,
    mcpServerEnabled: Boolean(data.mcpServerEnabled),
    mcpServerWriteEnabled: Boolean(data.mcpServerWriteEnabled),
  };
}

/**
 * Read the built-in MCP server runtime toggle from organization settings.
 */
export async function getMcpServerSettings(): Promise<McpServerSettings> {
  const response = await mcpApi.get('/settings');
  return {
    mcpServerEnabled: Boolean(response.data?.mcpServerEnabled),
    mcpServerWriteEnabled: Boolean(response.data?.mcpServerWriteEnabled),
  };
}

/**
 * Update the built-in MCP server runtime toggle (partial update — only the
 * provided fields are changed). Takes effect immediately, no restart required.
 */
export async function updateMcpServerToggle(
  input: McpServerToggleInput
): Promise<McpServerSettings> {
  const response = await mcpApi.put('/settings', input);
  return {
    mcpServerEnabled: Boolean(response.data?.mcpServerEnabled),
    mcpServerWriteEnabled: Boolean(response.data?.mcpServerWriteEnabled),
  };
}

export default {
  getMcpStatus,
  getMcpSettings,
  updateMcpSettings,
  getMcpServerSettings,
  updateMcpServerToggle,
};
