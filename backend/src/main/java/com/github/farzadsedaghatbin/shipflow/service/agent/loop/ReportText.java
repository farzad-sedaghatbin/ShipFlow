package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import java.util.List;

/** Small helpers the three builders share to lay a report out as plain text. */
final class ReportText {

  /** Most items shown in one section before "…and N more". */
  static final int MAX_ITEMS = 8;

  private ReportText() {}

  /** "✅ Finished (3)" followed by up to {@link #MAX_ITEMS} bullet lines; empty list → nothing. */
  static void section(StringBuilder out, String heading, List<String> lines) {
    if (lines.isEmpty()) {
      return;
    }
    out.append(heading).append(" (").append(lines.size()).append(")\n");
    int shown = Math.min(lines.size(), MAX_ITEMS);
    for (int i = 0; i < shown; i++) {
      out.append("  • ").append(lines.get(i)).append('\n');
    }
    if (lines.size() > shown) {
      out.append("  …and ").append(lines.size() - shown).append(" more\n");
    }
    out.append('\n');
  }

  static String who(com.github.farzadsedaghatbin.shipflow.entity.Person assignee) {
    return assignee == null || assignee.getName() == null ? "unassigned" : OutboundText.line(assignee.getName(), 40);
  }
}
