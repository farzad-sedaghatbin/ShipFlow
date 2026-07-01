package com.github.farzadsedaghatbin.shipflow.dto.system;

import java.util.List;

/**
 * Read-only view of ShipFlow's air-gapped mode state, returned by
 * {@code GET /api/system/air-gapped}.
 *
 * @param enabled
 *            whether air-gapped mode is turned on
 * @param activeProvider
 *            the configured active AI provider config value (e.g. {@code ollama})
 * @param activeProviderLocal
 *            whether the active provider runs entirely locally (no external egress)
 * @param ollamaBaseUrl
 *            the configured Ollama base URL
 * @param ollamaReachable
 *            best-effort reachability check against the Ollama base URL
 * @param externalMcpEnabled
 *            provider types of any currently-active external MCP clients; always
 *            empty when air-gapped mode is correctly enforced
 */
public record AirGappedStatusDTO(
    boolean enabled,
    String activeProvider,
    boolean activeProviderLocal,
    String ollamaBaseUrl,
    boolean ollamaReachable,
    List<String> externalMcpEnabled) {
}
