package com.github.farzadsedaghatbin.shipflow.dto.plugin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PluginDTO {

  private String pluginId;
  private String displayName;
  private String type; // "risk", "report", "integration"
  private boolean enabled;
}
