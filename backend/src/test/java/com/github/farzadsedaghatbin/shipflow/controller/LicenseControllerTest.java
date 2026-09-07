package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.license.UploadLicenseRequest;
import com.github.farzadsedaghatbin.shipflow.license.LicenseInvalidException;
import com.github.farzadsedaghatbin.shipflow.license.LicensePayload;
import com.github.farzadsedaghatbin.shipflow.license.LicenseService;
import com.github.farzadsedaghatbin.shipflow.license.LicenseStatus;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.WorkflowAutomationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LicenseControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private LicenseService licenseService;
  @MockBean private UserService userService;
  @MockBean private WorkflowAutomationService workflowAutomationService;

  @Test
  @WithMockUser(username = "member", roles = {"MEMBER"})
  @DisplayName("GET /api/license/status — any authenticated role -> 200 with status DTO")
  void getStatus_authenticated_returnsDto() throws Exception {
    LicensePayload payload = LicensePayload.builder()
        .id("lic-1").licensee("Acme Corp").edition("COMMERCIAL").seats(50)
        .features(List.of("sso", "audit-export"))
        .issuedAt(LocalDate.of(2026, 1, 1))
        .expiresAt(LocalDate.of(2027, 1, 1))
        .supportUntil(LocalDate.of(2027, 1, 1))
        .build();
    when(licenseService.getStatus()).thenReturn(LicenseStatus.VALID);
    when(licenseService.currentPayload()).thenReturn(Optional.of(payload));
    when(userService.countActiveUsers()).thenReturn(7L);
    when(workflowAutomationService.countEnabledAutomations()).thenReturn(2L);

    mockMvc.perform(get("/api/license/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("VALID"))
        .andExpect(jsonPath("$.licensee").value("Acme Corp"))
        .andExpect(jsonPath("$.seats").value(50))
        .andExpect(jsonPath("$.seatsUsed").value(7))
        .andExpect(jsonPath("$.automationsUsed").value(2))
        .andExpect(jsonPath("$.communityCap").value(10))
        .andExpect(jsonPath("$.automationCap").value(5))
        .andExpect(jsonPath("$.graceEndsAt").value("2027-01-31"))
        .andExpect(jsonPath("$.features", hasItem("sso")));
  }

  @Test
  @WithMockUser(username = "member", roles = {"MEMBER"})
  @DisplayName("GET /api/license/status — no licence -> 200 with MISSING and null licence fields")
  void getStatus_noLicence_returnsMissing() throws Exception {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.MISSING);
    when(licenseService.currentPayload()).thenReturn(Optional.empty());
    when(userService.countActiveUsers()).thenReturn(3L);
    when(workflowAutomationService.countEnabledAutomations()).thenReturn(1L);

    mockMvc.perform(get("/api/license/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MISSING"))
        .andExpect(jsonPath("$.licensee").doesNotExist())
        .andExpect(jsonPath("$.seats").doesNotExist())
        .andExpect(jsonPath("$.seatsUsed").value(3))
        .andExpect(jsonPath("$.communityCap").value(10))
        .andExpect(jsonPath("$.automationCap").value(5));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("POST /api/license — admin, valid content -> 200 with refreshed DTO")
  void uploadLicense_admin_validContent_returns200() throws Exception {
    LicensePayload payload = LicensePayload.builder()
        .id("lic-2").licensee("New Co").edition("COMMERCIAL").seats(20).features(List.of()).build();
    when(licenseService.uploadLicense(eq("{\"payload\":{}}"))).thenReturn(payload);
    when(licenseService.getStatus()).thenReturn(LicenseStatus.VALID);
    when(licenseService.currentPayload()).thenReturn(Optional.of(payload));

    UploadLicenseRequest request = new UploadLicenseRequest("{\"payload\":{}}");

    mockMvc.perform(post("/api/license").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.licensee").value("New Co"));

    verify(licenseService).uploadLicense("{\"payload\":{}}");
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("POST /api/license — admin, invalid content -> 400 license.invalid")
  void uploadLicense_admin_invalidContent_returns400() throws Exception {
    when(licenseService.uploadLicense(anyString())).thenThrow(new LicenseInvalidException("bad"));

    UploadLicenseRequest request = new UploadLicenseRequest("garbage");

    mockMvc.perform(post("/api/license").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.messageKey").value("license.invalid"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("POST /api/license — blank content -> 400 (bean validation)")
  void uploadLicense_admin_blankContent_returns400() throws Exception {
    UploadLicenseRequest request = new UploadLicenseRequest("   ");

    mockMvc.perform(post("/api/license").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "member", roles = {"MEMBER"})
  @DisplayName("POST /api/license — non-admin -> 403")
  void uploadLicense_nonAdmin_returns403() throws Exception {
    UploadLicenseRequest request = new UploadLicenseRequest("{}");

    mockMvc.perform(post("/api/license").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("DELETE /api/license — admin -> 200, removeLicense called")
  void removeLicense_admin_returns200() throws Exception {
    when(licenseService.getStatus()).thenReturn(LicenseStatus.MISSING);
    when(licenseService.currentPayload()).thenReturn(Optional.empty());

    mockMvc.perform(delete("/api/license"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MISSING"));

    verify(licenseService).removeLicense();
  }

  @Test
  @WithMockUser(username = "member", roles = {"MEMBER"})
  @DisplayName("DELETE /api/license — non-admin -> 403")
  void removeLicense_nonAdmin_returns403() throws Exception {
    mockMvc.perform(delete("/api/license")).andExpect(status().isForbidden());

    verify(licenseService, org.mockito.Mockito.never()).removeLicense();
  }
}
