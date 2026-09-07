package com.github.farzadsedaghatbin.shipflow.license;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring-managed bean method as requiring a specific licensed commercial feature.
 * Enforced by {@link LicenseFeatureAspect}: a call while {@link LicenseService#hasFeature(String)}
 * is false throws {@link LicenseFeatureException}, mapped to HTTP 403 (messageKey {@code
 * license.feature.required}) by {@code GlobalExceptionHandler}.
 *
 * <p>Feature names match a licence payload's {@code features} array entries, e.g. {@code "sso"},
 * {@code "audit-export"}, {@code "automations-unlimited"}, {@code "custom-fields"}, {@code
 * "channels"}, {@code "ldap"}, {@code "jira-server-import"}, {@code "ai-credits"}, {@code
 * "white-label"}.
 *
 * <p>No production code applies this annotation yet — v1.14.0 ships the licensing plumbing only,
 * gating no existing public feature. A future PR that turns some existing feature into a
 * commercial one is expected to add this annotation there.
 *
 * <p>Like any Spring AOP proxy-based advice, this only applies to calls that go through the Spring
 * proxy (i.e. from a different bean) — a method calling another {@code @RequiresLicensedFeature}
 * method on {@code this} bypasses the proxy and is not gated.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresLicensedFeature {

  /** Feature name, matching a licence payload's {@code features} entry. */
  String value();
}
