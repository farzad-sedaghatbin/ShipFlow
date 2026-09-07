package com.github.farzadsedaghatbin.shipflow.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Configuration for internationalization (i18n) support. Supports English
 * (default), Persian (Farsi), and Spanish.
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

  /** Supported locales for the application. */
  public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, // en
      Locale.forLanguageTag("fa"), // Persian/Farsi
      Locale.forLanguageTag("es") // Spanish
  );

  /**
   * Configure the MessageSource for loading i18n messages from property files.
   */
  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:i18n/messages", "classpath:i18n/errors", "classpath:i18n/validation");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setDefaultLocale(Locale.ENGLISH);
    messageSource.setCacheSeconds(3600); // Reload every hour in production
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  /**
   * Configure the LocaleResolver. Defaults to the Accept-Language header but
   * remembers an explicit choice (set via the {@code lang} query param, see
   * {@link #localeChangeInterceptor()}) in a cookie for subsequent requests.
   *
   * <p>Must be a resolver that implements {@code setLocale} —
   * {@code AcceptHeaderLocaleResolver} throws {@code UnsupportedOperationException}
   * from that method by design, which {@code LocaleChangeInterceptor} would
   * otherwise let escape as an uncaught 500 on any request carrying {@code ?lang=}.
   */
  @Bean
  public LocaleResolver localeResolver() {
    CookieLocaleResolver resolver = new CookieLocaleResolver("shipflow.locale");
    resolver.setDefaultLocale(Locale.ENGLISH);
    // Unlike AcceptHeaderLocaleResolver, CookieLocaleResolver has no setSupportedLocales — an
    // unsupported ?lang= value just resolves to a Locale with no matching message bundle, and
    // ReloadableResourceBundleMessageSource's existing fallback (useCodeAsDefaultMessage) handles
    // that the same way it already handles any other missing translation key.
    return resolver;
  }

  /**
   * Configure the LocaleChangeInterceptor to allow changing locale via query
   * parameter. Example: ?lang=fa or ?lang=es
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }

  /**
   * Configure the LocalValidatorFactoryBean to use our MessageSource for
   * validation messages.
   */
  @Bean
  public LocalValidatorFactoryBean getValidator() {
    LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
    bean.setValidationMessageSource(messageSource());
    return bean;
  }
}
