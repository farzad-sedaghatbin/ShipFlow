package com.github.farzadsedaghatbin.shipflow.service;

/** Contract for email notification delivery. A no-op stub is registered when SMTP is not configured. */
public interface IEmailNotificationService {

  /** Returns true when SMTP is configured and email sending is active. */
  boolean isEnabled();

  void sendTaskAssigned(String toEmail, String recipientName, String taskTitle, String taskUrl);

  void sendMentionedInComment(String toEmail, String recipientName, String entityTitle, String actionUrl);

  void sendPitchStatusChanged(
      String toEmail, String recipientName, String pitchTitle, String newStatus, String actionUrl);

  /**
   * A message from a workflow automation (the "Notify" actions and the "Send email" action).
   *
   * @param subject the email subject (single line)
   * @param message the body text; plain text, rendered escaped
   * @param actionUrl a path inside ShipFlow ("/backlog/12") or a full URL, or null for no button
   */
  void sendAutomationEmail(
      String toEmail, String recipientName, String subject, String message, String actionUrl);
}
