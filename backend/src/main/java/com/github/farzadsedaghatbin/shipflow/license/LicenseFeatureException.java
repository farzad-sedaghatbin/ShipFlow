package com.github.farzadsedaghatbin.shipflow.license;

import lombok.Getter;

/**
 * Thrown by {@link LicenseFeatureAspect} when a method annotated {@link
 * RequiresLicensedFeature} is called without an active licence granting that
 * feature. Mapped to HTTP 403 (messageKey {@code license.feature.required}) by
 * {@code GlobalExceptionHandler}.
 */
@Getter
public class LicenseFeatureException extends RuntimeException {

  private final String featureName;

  public LicenseFeatureException(String featureName) {
    super("Licensed feature required: " + featureName);
    this.featureName = featureName;
  }
}
