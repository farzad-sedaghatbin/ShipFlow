package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract tests for the admin-only audit-export endpoint. Envers revision rows
 * are not reliably visible inside a rolled-back transactional test, so these
 * assert the endpoint surface (status, content-type, shape, authz) rather than
 * specific revision content.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = {"ADMIN"})
class AuditExportControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void csvExportReturnsAttachmentWithHeaderRow() throws Exception {
    mockMvc.perform(get("/api/audit/export").param("entityType", "task").param("format", "csv"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("text/csv")))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("audit-task-")))
        .andExpect(content().string(containsString(
            "entityType,entityId,revision,timestamp,modifiedBy,changeType,field,oldValue,newValue")));
  }

  @Test
  void jsonExportReturnsJsonArray() throws Exception {
    mockMvc.perform(get("/api/audit/export").param("entityType", "all").param("format", "json"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("application/json")))
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void unknownEntityTypeIsRejected() throws Exception {
    mockMvc.perform(get("/api/audit/export").param("entityType", "bogus"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invertedDateRangeIsRejected() throws Exception {
    mockMvc.perform(get("/api/audit/export").param("from", "2026-06-30").param("to", "2026-06-01"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "viewer", roles = {"VIEWER"})
  void nonAdminIsForbidden() throws Exception {
    mockMvc.perform(get("/api/audit/export")).andExpect(status().isForbidden());
  }
}
