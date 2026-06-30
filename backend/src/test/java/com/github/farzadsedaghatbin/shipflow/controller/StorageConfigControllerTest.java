package com.github.farzadsedaghatbin.shipflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.storage.ConnectionTestResponse;
import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.StorageConfigDTO;
import com.github.farzadsedaghatbin.shipflow.dto.storage.UpdateStorageConfigRequest;
import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.service.StorageConfigService;
import com.github.farzadsedaghatbin.shipflow.service.StorageMigrationService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import java.time.OffsetDateTime;
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
class StorageConfigControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private StorageConfigService storageConfigService;
  @MockBean private StorageMigrationService storageMigrationService;

  private StorageConfig makeConfig(String configJson) {
    return StorageConfig.builder()
        .id(1L)
        .activeProvider(StorageProviderType.S3)
        .config(configJson)
        .createdAt(OffsetDateTime.now())
        .updatedAt(OffsetDateTime.now())
        .build();
  }

  @Test
  @WithMockUser(username = "dev", roles = {"DEVELOPER"})
  @DisplayName("GET /api/admin/storage — non-admin → 403")
  void getConfig_nonAdmin_returns403() throws Exception {
    mockMvc.perform(get("/api/admin/storage")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName(
      "GET /api/admin/storage — admin, config with secrets → response has hasAccessKey/hasSecretKey, no raw secret values")
  void getConfig_admin_neverReturnsSecrets() throws Exception {
    StorageConfigDTO dto =
        StorageConfigDTO.builder()
            .activeProvider(StorageProviderType.S3)
            .bucket("my-bucket")
            .endpoint("https://s3.example.com")
            .region("us-east-1")
            .hasAccessKey(true)
            .hasSecretKey(true)
            .build();
    when(storageConfigService.getActiveConfigDto()).thenReturn(dto);

    mockMvc
        .perform(get("/api/admin/storage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasAccessKey").value(true))
        .andExpect(jsonPath("$.hasSecretKey").value(true))
        .andExpect(jsonPath("$.accessKey").doesNotExist())
        .andExpect(jsonPath("$.secretKey").doesNotExist())
        .andExpect(jsonPath("$.bucket").value("my-bucket"))
        .andExpect(jsonPath("$.activeProvider").value("S3"));
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName("POST /api/admin/storage/migrate — admin → 200 with counts")
  void migrate_admin_returnsResult() throws Exception {
    MigrationResultDTO result =
        MigrationResultDTO.builder().migrated(3).skipped(1).failed(0).total(4).build();
    when(storageMigrationService.migrateToActiveBackend()).thenReturn(result);

    mockMvc
        .perform(post("/api/admin/storage/migrate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.migrated").value(3))
        .andExpect(jsonPath("$.skipped").value(1))
        .andExpect(jsonPath("$.failed").value(0))
        .andExpect(jsonPath("$.total").value(4));
  }

  // ── Secret-merge tests ────────────────────────────────────────────────────

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName(
      "PUT /api/admin/storage — blank secretKey in request → stored secret preserved, bucket updated, GET still reports hasSecretKey=true")
  void updateConfig_blankSecret_preservesStoredSecret() throws Exception {
    // The service's mergeSecrets helper is exercised by updateFromRequest; here we verify
    // the service layer via its own unit and wire the controller to return the correct DTO.
    String storedSecret = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    String storedJson =
        "{\"bucket\":\"old-bucket\",\"accessKey\":\"AKIAIOSFODNN7EXAMPLE\","
            + "\"secretKey\":\""
            + storedSecret
            + "\"}";

    // Use the real mergeSecrets directly (public method, no Spring context needed)
    StorageConfigService realService =
        new StorageConfigService(null, null, objectMapper);
    ObjectNode submitted = objectMapper.createObjectNode();
    submitted.put("bucket", "new-bucket");
    submitted.put("accessKey", ""); // blank — should be preserved
    submitted.put("secretKey", ""); // blank — should be preserved

    ObjectNode merged = realService.mergeSecrets(submitted, storedJson);

    // White-box: merged node must still carry the original secret
    assertThat(merged.get("secretKey").asText()).isEqualTo(storedSecret);
    assertThat(merged.get("accessKey").asText()).isEqualTo("AKIAIOSFODNN7EXAMPLE");
    assertThat(merged.get("bucket").asText()).isEqualTo("new-bucket");

    // Controller-level: stub updateFromRequest to return DTO with hasSecretKey=true
    StorageConfigDTO dto =
        StorageConfigDTO.builder()
            .activeProvider(StorageProviderType.S3)
            .bucket("new-bucket")
            .hasAccessKey(true)
            .hasSecretKey(true)
            .build();
    when(storageConfigService.updateFromRequest(any(UpdateStorageConfigRequest.class)))
        .thenReturn(dto);

    String body =
        objectMapper.writeValueAsString(
            UpdateStorageConfigRequest.builder()
                .activeProvider(StorageProviderType.S3)
                .config(submitted)
                .build());

    mockMvc
        .perform(put("/api/admin/storage").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasSecretKey").value(true))
        .andExpect(jsonPath("$.bucket").value("new-bucket"))
        .andExpect(jsonPath("$.secretKey").doesNotExist());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName(
      "PUT /api/admin/storage — non-blank new secretKey → stored secret replaced with new value")
  void updateConfig_newSecret_replacesStoredSecret() throws Exception {
    String originalSecret = "OLD_SECRET";
    String newSecret = "NEW_SECRET_VALUE";
    String storedJson =
        "{\"bucket\":\"my-bucket\",\"accessKey\":\"OLD_AK\",\"secretKey\":\"" + originalSecret + "\"}";

    StorageConfigService realService =
        new StorageConfigService(null, null, objectMapper);
    ObjectNode submitted = objectMapper.createObjectNode();
    submitted.put("bucket", "my-bucket");
    submitted.put("accessKey", "NEW_AK");
    submitted.put("secretKey", newSecret); // non-blank → should override

    ObjectNode merged = realService.mergeSecrets(submitted, storedJson);

    // White-box: new non-blank secret must override the stored one
    assertThat(merged.get("secretKey").asText()).isEqualTo(newSecret);
    assertThat(merged.get("accessKey").asText()).isEqualTo("NEW_AK");

    // Controller-level: stub updateFromRequest to return DTO reflecting updated secret presence
    StorageConfigDTO dto =
        StorageConfigDTO.builder()
            .activeProvider(StorageProviderType.S3)
            .bucket("my-bucket")
            .hasAccessKey(true)
            .hasSecretKey(true)
            .build();
    when(storageConfigService.updateFromRequest(any(UpdateStorageConfigRequest.class)))
        .thenReturn(dto);

    String body =
        objectMapper.writeValueAsString(
            UpdateStorageConfigRequest.builder()
                .activeProvider(StorageProviderType.S3)
                .config(submitted)
                .build());

    mockMvc
        .perform(put("/api/admin/storage").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasSecretKey").value(true))
        .andExpect(jsonPath("$.secretKey").doesNotExist());
  }

  @Test
  @WithMockUser(username = "admin", roles = {"ADMIN"})
  @DisplayName(
      "POST /api/admin/storage/test — blank secret in request → service returns ConnectionTestResponse (merge helper exercised)")
  void testConnection_blankSecret_mergeHelperExercised() throws Exception {
    // White-box: verify mergeSecrets fills in blank secret before test
    String storedSecret = "STORED_SECRET";
    String storedJson =
        "{\"bucket\":\"test-bucket\",\"accessKey\":\"STORED_AK\",\"secretKey\":\"" + storedSecret + "\"}";

    StorageConfigService realService =
        new StorageConfigService(null, null, objectMapper);
    ObjectNode submitted = objectMapper.createObjectNode();
    submitted.put("bucket", "test-bucket");
    submitted.put("secretKey", ""); // blank — should be filled from stored

    ObjectNode merged = realService.mergeSecrets(submitted, storedJson);
    assertThat(merged.get("secretKey").asText()).isEqualTo(storedSecret);

    // Controller-level: stub testFromRequest to return a success response
    ConnectionTestResponse response =
        ConnectionTestResponse.builder().ok(true).message("Connection successful").build();
    when(storageConfigService.testFromRequest(any(UpdateStorageConfigRequest.class)))
        .thenReturn(response);

    String body =
        objectMapper.writeValueAsString(
            UpdateStorageConfigRequest.builder()
                .activeProvider(StorageProviderType.S3)
                .config(submitted)
                .build());

    mockMvc
        .perform(
            post("/api/admin/storage/test").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.message").value("Connection successful"));
  }
}
