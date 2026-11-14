package com.github.farzadsedaghatbin.shipflow.dto.plugin;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** A plugin page ready for the browser: data only, no code. */
@Data
@Builder
public class PluginPageDTO {

  private String pluginId;
  private String pageKey;
  private String title;
  private List<PluginBlockDTO> blocks;
}
