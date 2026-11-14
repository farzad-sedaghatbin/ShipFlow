package com.github.farzadsedaghatbin.shipflow.dto.plugin;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** A sidebar entry contributed by a UI plugin (already validated by the backend). */
@Data
@Builder
public class PluginMenuItemDTO {

  private String pluginId;
  private String key;
  /** Display text by language code; always contains "en". */
  private Map<String, String> labels;
  private String icon;
  /** ANY, MANAGER or ADMIN. Informational: the backend enforces it on the page endpoint. */
  private String requiredRole;
}
