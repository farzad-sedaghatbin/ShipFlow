package com.github.farzadsedaghatbin.shipflow.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Canonical JSON serialization used to sign/verify a licence payload: object
 * keys sorted recursively (depth-first, alphabetical by key at every level, via
 * {@link String#compareTo}), no insignificant whitespace, UTF-8 bytes. Array
 * element order is preserved as-is — only object key order is normalised.
 *
 * <p>This is the "canonical JSON of payload" referenced by {@link
 * LicensePayload}'s class Javadoc and MUST byte-for-byte match whatever
 * canonicalisation the private licence-issuer tool uses to produce {@code
 * signature} — sorted-keys-plus-compact is the standard, widely implemented
 * definition (equivalent in spirit to RFC 8785 JCS for this purpose), so any
 * issuer tool built on a standard JSON library's "sort keys, no whitespace"
 * output should already match.
 *
 * <p>Scalar values (strings/numbers/booleans/null) are re-emitted via Jackson's
 * own single-node {@link JsonNode#toString()}, which is already valid, compact
 * JSON for that node — this class only reimplements object-key ordering, never
 * hand-rolls string escaping or number formatting.
 */
final class CanonicalJson {

  private CanonicalJson() {}

  /** Canonical UTF-8 bytes of {@code node} — see class Javadoc for the exact rules. */
  static byte[] canonicalBytes(JsonNode node) {
    StringBuilder sb = new StringBuilder();
    write(node, sb);
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static void write(JsonNode node, StringBuilder sb) {
    if (node == null || node.isNull()) {
      sb.append("null");
    } else if (node.isObject()) {
      writeObject(node, sb);
    } else if (node.isArray()) {
      writeArray(node, sb);
    } else {
      // Scalars: Jackson's own node serialization is already compact, valid
      // JSON for a single value — reuse it rather than reimplementing string
      // escaping / number formatting.
      sb.append(node.toString());
    }
  }

  private static void writeObject(JsonNode node, StringBuilder sb) {
    Map<String, JsonNode> sorted = new TreeMap<>();
    node.fields().forEachRemaining(entry -> sorted.put(entry.getKey(), entry.getValue()));

    sb.append('{');
    boolean first = true;
    for (Map.Entry<String, JsonNode> entry : sorted.entrySet()) {
      if (!first) {
        sb.append(',');
      }
      first = false;
      // Reuse Jackson's TextNode serialization for the key so quoting/escaping
      // stays identical to how value strings are escaped.
      sb.append(TextNode.valueOf(entry.getKey()).toString());
      sb.append(':');
      write(entry.getValue(), sb);
    }
    sb.append('}');
  }

  private static void writeArray(JsonNode node, StringBuilder sb) {
    sb.append('[');
    boolean first = true;
    for (JsonNode element : node) {
      if (!first) {
        sb.append(',');
      }
      first = false;
      write(element, sb);
    }
    sb.append(']');
  }
}
