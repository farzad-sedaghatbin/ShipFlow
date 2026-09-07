package com.github.farzadsedaghatbin.shipflow.license;

import lombok.Getter;

/**
 * Thrown by {@code WorkflowAutomationService} when creating, updating, or toggling a workflow
 * automation rule to enabled would exceed the enabled-automation cap computed by {@link
 * LicenseLimits#enabledAutomationCap()} — Community Edition default {@value
 * LicenseLimits#COMMUNITY_AUTOMATION_CAP}; unlimited under a VALID/GRACE licence (see {@link
 * LicenseLimits#automationsUnlimited()}). Never disables an already-enabled automation. Mapped to
 * HTTP 409 (messageKey {@code license.automations.exceeded}) by {@code GlobalExceptionHandler}.
 */
@Getter
public class AutomationLimitExceededException extends RuntimeException {

  private final int limit;

  public AutomationLimitExceededException(int limit) {
    super("Enabled workflow automation limit reached (" + limit + ")");
    this.limit = limit;
  }
}
