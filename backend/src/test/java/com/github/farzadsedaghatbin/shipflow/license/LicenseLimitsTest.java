package com.github.farzadsedaghatbin.shipflow.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenseLimitsTest {

  @Mock private LicenseService licenseService;

  @InjectMocks
  private LicenseLimits licenseLimits;

  @Test
  void activeUserCap_MissingStatus_ReturnsCommunityCap() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.MISSING);

    assertThat(licenseLimits.activeUserCap()).isEqualTo(LicenseLimits.COMMUNITY_USER_SEAT_CAP);
  }

  @Test
  void activeUserCap_ExpiredStatus_ReturnsCommunityCap() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.EXPIRED);

    assertThat(licenseLimits.activeUserCap()).isEqualTo(LicenseLimits.COMMUNITY_USER_SEAT_CAP);
  }

  @Test
  void activeUserCap_ValidStatus_ReturnsBufferedLicensedSeats() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.VALID);
    when(licenseService.seatLimit()).thenReturn(100);

    assertThat(licenseLimits.activeUserCap()).isEqualTo(110); // floor(100 * 1.1)
  }

  @Test
  void activeUserCap_GraceStatus_ReturnsBufferedLicensedSeats() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.GRACE);
    when(licenseService.seatLimit()).thenReturn(10);

    assertThat(licenseLimits.activeUserCap()).isEqualTo(11); // floor(10 * 1.1)
  }

  @Test
  void automationsUnlimited_ValidOrGrace_True() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.VALID);
    assertThat(licenseLimits.automationsUnlimited()).isTrue();

    when(licenseService.getStatus()).thenReturn(LicenseStatus.GRACE);
    assertThat(licenseLimits.automationsUnlimited()).isTrue();
  }

  @Test
  void automationsUnlimited_MissingOrExpired_False() {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.MISSING);
    assertThat(licenseLimits.automationsUnlimited()).isFalse();

    when(licenseService.getStatus()).thenReturn(LicenseStatus.EXPIRED);
    assertThat(licenseLimits.automationsUnlimited()).isFalse();
  }

  @Test
  void enabledAutomationCap_ReturnsCommunityConstant() {
    assertThat(licenseLimits.enabledAutomationCap()).isEqualTo(LicenseLimits.COMMUNITY_AUTOMATION_CAP);
  }
}
