package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.Map;

/**
 * Extension point for custom report generation.
 * Implementations can add new report formats or specialized reports.
 */
public interface ReportGeneratorPlugin {

  /** Unique identifier for this plugin. */
  String getPluginId();

  /** Human-readable name for this report type. */
  String getDisplayName();

  /** The MIME type of the generated report, e.g. "application/pdf". */
  String getContentType();

  /** The suggested file extension, e.g. "pdf". */
  String getFileExtension();

  /**
   * Generate a report.
   *
   * @param context a map containing report parameters:
   *   - "projectId" (Long)
   *   - "cycleId" (Long, optional)
   *   - "format" (String)
   * @return the generated report as a byte array
   */
  byte[] generate(Map<String, Object> context);
}
