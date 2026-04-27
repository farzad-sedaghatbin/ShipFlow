package com.github.farzadsedaghatbin.shipflow.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for handling internationalization (i18n) messages. Provides
 * convenient methods to retrieve localized messages.
 */
@Service
@RequiredArgsConstructor
public class LocalizationService {

  private final MessageSource messageSource;

  /**
   * Get a localized message for the given key using the current locale.
   *
   * @param key
   *            the message key
   * @return the localized message
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
   *            the message arguments for placeholders
   * @return the localized message
   */
  public String getMessage(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }

  /**
   * Get a localized message for the given key with a specific locale.
   *
   * @param key
   *            the message key
   * @param locale
   *            the locale to use
   * @return the localized message
   */
  public String getMessage(String key, Locale locale) {
    return messageSource.getMessage(key, null, key, locale);
  }

  /**
   * Get a localized message for the given key with arguments and a specific
   * locale.
   *
   * @param key
   *            the message key
   * @param args
   *            the message arguments for placeholders
   * @param locale
   *            the locale to use
   * @return the localized message
   */
  public String getMessage(String key, Object[] args, Locale locale) {
    return messageSource.getMessage(key, args, key, locale);
  }

  /**
   * Get a localized message with a default fallback if the key is not found.
   *
   * @param key
   *            the message key
   * @param defaultMessage
   *            the default message if key is not found
   * @return the localized message or default
   */
  public String getMessageWithDefault(String key, String defaultMessage) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, null, defaultMessage, locale);
  }

  /**
   * Get a localized message with arguments and a default fallback.
   *
   * @param key
   *            the message key
   * @param args
   *            the message arguments
   * @param defaultMessage
   *            the default message if key is not found
   * @return the localized message or default
   */
  public String getMessageWithDefault(String key, Object[] args, String defaultMessage) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, defaultMessage, locale);
  }
}
