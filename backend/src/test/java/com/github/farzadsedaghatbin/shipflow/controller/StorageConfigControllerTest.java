package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.storage.MigrationResultDTO;
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
    String configJson =
        "{\"bucket\":\"my-bucket\",\"endpoint\":\"https://s3.example.com\","
            + "\"region\":\"us-east-1\",\"accessKey\":\"AKIAIOSFODNN7EXAMPLE\","
            + "\"secretKey\":\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\"}";
    when(storageConfigService.getActiveConfig()).thenReturn(makeConfig(configJson));

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
}
