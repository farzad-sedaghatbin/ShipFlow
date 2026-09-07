package com.github.farzadsedaghatbin.shipflow.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Enforces {@link RequiresLicensedFeature} on any Spring-managed bean method. See that
 * annotation's Javadoc for the AOP-proxy caveat and for why no production method carries it yet.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LicenseFeatureAspect {

  private final LicenseService licenseService;

  @Before("@annotation(requiresLicensedFeature)")
  public void enforce(RequiresLicensedFeature requiresLicensedFeature) {
    String feature = requiresLicensedFeature.value();
    if (!licenseService.hasFeature(feature)) {
      log.debug("Blocked call to a '{}'-gated method — current licence status is {}", feature,
          licenseService.getStatus());
      throw new LicenseFeatureException(feature);
    }
  }
}
