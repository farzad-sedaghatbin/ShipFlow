package com.github.farzadsedaghatbin.shipflow.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Proves {@link LicenseFeatureAspect} actually gates a Spring-proxied bean method annotated
 * {@link RequiresLicensedFeature} — a plain unit test of the aspect's {@code enforce} method
 * wouldn't exercise the AOP proxy itself, which is the part most likely to be misconfigured
 * (missing {@code @EnableAspectJAutoProxy}, wrong pointcut expression, etc.).
 */
@SpringJUnitConfig(classes = LicenseFeatureAspectTest.TestConfig.class)
class LicenseFeatureAspectTest {

  @Autowired private ProbeService probeService;
  @Autowired private LicenseService licenseService;

  @Test
  void gatedMethod_WithoutFeature_ThrowsLicenseFeatureException() {
    when(licenseService.hasFeature("sso")).thenReturn(false);

    assertThatThrownBy(() -> probeService.ssoOnlyMethod())
        .isInstanceOf(LicenseFeatureException.class)
        .extracting(ex -> ((LicenseFeatureException) ex).getFeatureName())
        .isEqualTo("sso");
  }

  @Test
  void gatedMethod_WithFeature_ProceedsNormally() {
    when(licenseService.hasFeature("sso")).thenReturn(true);

    assertThat(probeService.ssoOnlyMethod()).isEqualTo("ok");
  }

  @Test
  void ungatedMethod_NeverConsultsLicenseService() {
    assertThat(probeService.plainMethod()).isEqualTo("plain");
  }

  /** Proxied test-only bean carrying one gated and one ungated method. */
  @Component
  static class ProbeService {
    @RequiresLicensedFeature("sso")
    public String ssoOnlyMethod() {
      return "ok";
    }

    public String plainMethod() {
      return "plain";
    }
  }

  @Configuration
  @EnableAspectJAutoProxy
  static class TestConfig {
    @Bean
    LicenseService licenseService() {
      return mock(LicenseService.class);
    }

    @Bean
    LicenseFeatureAspect licenseFeatureAspect(LicenseService licenseService) {
      return new LicenseFeatureAspect(licenseService);
    }

    @Bean
    ProbeService probeService() {
      return new ProbeService();
    }
  }
}
