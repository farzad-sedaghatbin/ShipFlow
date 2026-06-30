package com.github.farzadsedaghatbin.shipflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the v1.9.0 observability surface: the health probe and the Prometheus
 * scrape endpoint are exposed, while the raw actuator metrics browser is not.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityEndpointsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthEndpointIsPublicAndUp() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void prometheusEndpointIsExposedAndScrapable() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        // Common-tag we set via management.metrics.tags.application=shipflow must
        // be stamped onto every series, so Prometheus/Grafana can isolate this app.
        .andExpect(content().string(org.hamcrest.Matchers.containsString("application=\"shipflow\"")))
        // A baseline JVM metric proves the registry is actually wired, not empty.
        .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
  }

  @Test
  void rawMetricsBrowserIsNotExposed() throws Exception {
    // Only health,info,prometheus are exposed; the JSON /actuator/metrics browser
    // is intentionally kept off the public surface. An unexposed actuator path is
    // not handled by Spring Boot, so it never returns a 200 actuator payload (it
    // falls through to the SPA forward controller instead) — assert it is not OK.
    int status = mockMvc.perform(get("/actuator/metrics")).andReturn().getResponse().getStatus();
    assertThat(status).isNotEqualTo(200);
  }
}
