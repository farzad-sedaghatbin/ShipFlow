package com.github.farzadsedaghatbin.shipflow.service;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving internationalized messages. This service provides a
 * convenient way to access localized messages throughout the application,
 * respecting the user's locale settings.
 */
@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Get a localized message for the given key using the current locale.
   *
   * @param key
   *            the message key
   * @return the localized message, or the key itself if not found
   */
  public String getMessage(String key) {
    return getMessage(key, (Object[]) null);
  }

  /**
   * Get a localized message for the given key with arguments using the current
   * locale.
   *
   * @param key
   *            the message key
   * @param args
   *            optional arguments to be interpolated in the message
   * @return the localized message, or the key itself if not found
   */
  public String getMessage(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }

  /**
   * Get a localized message for the given key using a specific locale.
   *
   * @param key
   *            the message key
   * @param locale
   *            the locale to use
   * @return the localized message, or the key itself if not found
   */
  public String getMessage(String key, Locale locale) {
    return getMessage(key, locale, (Object[]) null);
  }

  /**
   * Get a localized message for the given key with arguments using a specific
   * locale.
   *
   * @param key
   *            the message key
   * @param locale
   *            the locale to use
   * @param args
   *            optional arguments to be interpolated in the message
   * @return the localized message, or the key itself if not found
   */
  public String getMessage(String key, Locale locale, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
  }

  /**
   * Get the current locale from the context.
   *
   * @return the current locale
   */
  public Locale getCurrentLocale() {
    return LocaleContextHolder.getLocale();
  }

  // Convenience methods for common messages

  public String getProjectCreatedMessage() {
    return getMessage("project.created");
  }

  public String getProjectUpdatedMessage() {
    return getMessage("project.updated");
  }

  public String getProjectDeletedMessage() {
    return getMessage("project.deleted");
  }

  public String getProjectNotFoundMessage() {
    return getMessage("project.not.found");
  }

  public String getCycleCreatedMessage() {
    return getMessage("cycle.created");
  }

  public String getCycleUpdatedMessage() {
    return getMessage("cycle.updated");
  }

  public String getCycleDeletedMessage() {
    return getMessage("cycle.deleted");
  }

  public String getCycleNotFoundMessage() {
    return getMessage("cycle.not.found");
  }

  public String getPitchCreatedMessage() {
    return getMessage("pitch.created");
  }

  public String getPitchUpdatedMessage() {
    return getMessage("pitch.updated");
  }

  public String getPitchDeletedMessage() {
    return getMessage("pitch.deleted");
  }

  public String getPitchNotFoundMessage() {
    return getMessage("pitch.not.found");
  }

  public String getTaskCreatedMessage() {
    return getMessage("task.created");
  }

  public String getTaskUpdatedMessage() {
    return getMessage("task.updated");
  }

  public String getTaskDeletedMessage() {
    return getMessage("task.deleted");
  }

  public String getTaskNotFoundMessage() {
    return getMessage("task.not.found");
  }

  public String getTeamCreatedMessage() {
    return getMessage("team.created");
  }

  public String getTeamUpdatedMessage() {
    return getMessage("team.updated");
  }

  public String getTeamDeletedMessage() {
    return getMessage("team.deleted");
  }

  public String getTeamNotFoundMessage() {
    return getMessage("team.not.found");
  }

  public String getAccessDeniedMessage() {
    return getMessage("auth.access.denied");
  }

  public String getLoginFailedMessage() {
    return getMessage("auth.login.failed");
  }

  public String getSessionExpiredMessage() {
    return getMessage("auth.session.expired");
  }

  public String getGenericErrorMessage() {
    return getMessage("error.generic");
  }

  public String getValidationErrorMessage() {
    return getMessage("error.validation");
  }

  public String getResourceNotFoundMessage() {
    return getMessage("error.not.found");
  }
}
