package com.github.farzadsedaghatbin.shipflow.dto.plugin;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** One validated block of a plugin page. {@code type} is MARKDOWN, STAT_CARDS, TABLE, LINK_LIST or ALERT. */
@Data
@Builder
public class PluginBlockDTO {

  private String type;
  private Map<String, Object> data;
}
