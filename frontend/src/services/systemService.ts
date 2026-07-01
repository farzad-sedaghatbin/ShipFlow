import api from './api';

/**
 * Read-only system status surfaced from the backend `GET /api/system/air-gapped`
 * endpoint. Mirrors the backend `AirGappedStatusDTO`.
 */
export interface AirGappedStatus {
  /** Whether air-gapped AI mode is enabled. */
  enabled: boolean;
  /** Active AI provider config value (e.g. `ollama`). */
  activeProvider: string;
  /** Whether the active provider runs entirely locally (no external egress). */
  activeProviderLocal: boolean;
  /** Configured Ollama base URL. */
  ollamaBaseUrl: string;
  /** Best-effort reachability of the Ollama base URL. */
  ollamaReachable: boolean;
  /** Active external MCP client types; empty when air-gapped is enforced. */
  externalMcpEnabled: string[];
}

export const systemService = {
  /** Fetch the current air-gapped mode status. */
  async getAirGappedStatus(): Promise<AirGappedStatus> {
    const { data } = await api.get<AirGappedStatus>('/system/air-gapped');
    return data;
  },
};

export default systemService;
