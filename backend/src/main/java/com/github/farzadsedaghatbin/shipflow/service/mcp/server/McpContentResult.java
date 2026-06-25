package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Marker return type for MCP tools that need to emit native MCP content blocks (e.g. an {@code
 * image} block) instead of the default JSON-as-text wrapping. When {@code dispatchTool} returns one
 * of these, {@link McpToolDispatcher} passes its {@link #content()} straight through as the tool
 * result's {@code content} array.
 */
public record McpContentResult(List<Map<String, Object>> content) {

  /** A single image content block — the client (e.g. Claude Code) can render this visually. */
  public static McpContentResult image(byte[] bytes, String mimeType) {
    String base64 = Base64.getEncoder().encodeToString(bytes);
    return new McpContentResult(
        List.of(Map.of("type", "image", "data", base64, "mimeType", mimeType)));
  }

  /** A text note followed by an image block. */
  public static McpContentResult imageWithText(byte[] bytes, String mimeType, String text) {
    String base64 = Base64.getEncoder().encodeToString(bytes);
    return new McpContentResult(
        List.of(
            Map.of("type", "text", "text", text),
            Map.of("type", "image", "data", base64, "mimeType", mimeType)));
  }
}
