package com.github.farzadsedaghatbin.shipflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Contract tests for the read-only system-status endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void airGappedStatus_returnsShapeForAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/api/system/air-gapped"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").exists())
        .andExpect(jsonPath("$.activeProvider").exists())
        .andExpect(jsonPath("$.activeProviderLocal").exists())
        .andExpect(jsonPath("$.ollamaBaseUrl").exists())
        .andExpect(jsonPath("$.ollamaReachable").exists())
        .andExpect(jsonPath("$.externalMcpEnabled").isArray());
  }

  @Test
  void airGappedStatus_isGatedForAnonymousUser() throws Exception {
    // Anonymous access is rejected by the security chain (redirected to login),
    // never served the status payload.
    mockMvc.perform(get("/api/system/air-gapped"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }
}
