package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.List;

/**
 * A page a plugin returns. It is data, not code: ShipFlow renders the blocks with its own
 * components, so no plugin JavaScript ever runs in the browser.
 */
public record PluginPage(String title, List<PluginBlock> blocks) {

  public PluginPage {
    blocks = blocks == null ? List.of() : List.copyOf(blocks);
  }
}
