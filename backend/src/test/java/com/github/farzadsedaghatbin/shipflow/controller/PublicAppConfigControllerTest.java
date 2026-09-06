package com.github.farzadsedaghatbin.shipflow.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PublicAppConfigController}. No Spring context needed. */
class PublicAppConfigControllerTest {

  @Test
  void getPublicConfig_reflectsDemoModeDisabled() throws Exception {
    PublicAppConfigController controller = new PublicAppConfigController();
    setDemoModeEnabled(controller, false);

    assertThat(controller.getPublicConfig().getBody())
        .containsEntry("demoModeEnabled", false);
  }

  @Test
  void getPublicConfig_reflectsDemoModeEnabled() throws Exception {
    PublicAppConfigController controller = new PublicAppConfigController();
    setDemoModeEnabled(controller, true);

    assertThat(controller.getPublicConfig().getBody())
        .containsEntry("demoModeEnabled", true);
  }

  private void setDemoModeEnabled(PublicAppConfigController controller, boolean value)
      throws Exception {
    Field field = PublicAppConfigController.class.getDeclaredField("demoModeEnabled");
    field.setAccessible(true);
    field.set(controller, value);
  }
}
