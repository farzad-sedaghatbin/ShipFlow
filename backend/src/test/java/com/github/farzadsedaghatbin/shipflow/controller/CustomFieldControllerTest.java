package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.customfield.CreateCustomFieldDefinitionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.customfield.CustomFieldDefinitionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import com.github.farzadsedaghatbin.shipflow.service.CustomFieldService;
import java.time.OffsetDateTime;
import java.util.List;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CustomFieldControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private CustomFieldService customFieldService;

  @Test
  void getDefinitions_returns200WithList() throws Exception {
    var dto =
        CustomFieldDefinitionDTO.builder()
            .id(1L)
            .name("Sprint Notes")
            .fieldType(CustomFieldType.TEXT)
            .entityType(CustomFieldEntityType.TASK)
            .required(false)
            .sortOrder(0)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    when(customFieldService.getDefinitionsForEntity(CustomFieldEntityType.TASK, null))
        .thenReturn(List.of(dto));

    mockMvc
        .perform(get("/api/custom-fields/definitions").param("entityType", "TASK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Sprint Notes"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void createDefinition_validRequest_returns201() throws Exception {
    var req = new CreateCustomFieldDefinitionRequest();
    req.setName("Priority Tier");
    req.setFieldType(CustomFieldType.SELECT);
    req.setEntityType(CustomFieldEntityType.TASK);
    req.setProjectId(1L);
    req.setOptions(List.of("Low", "Medium", "High"));

    var dto =
        CustomFieldDefinitionDTO.builder()
            .id(2L)
            .name("Priority Tier")
            .fieldType(CustomFieldType.SELECT)
            .entityType(CustomFieldEntityType.TASK)
            .projectId(1L)
            .required(false)
            .sortOrder(0)
            .options(List.of("Low", "Medium", "High"))
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    when(customFieldService.createDefinition(any())).thenReturn(dto);

    mockMvc
        .perform(
            post("/api/custom-fields/definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Priority Tier"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void deleteDefinition_returns204() throws Exception {
    doNothing().when(customFieldService).deleteDefinition(1L);

    mockMvc
        .perform(delete("/api/custom-fields/definitions/1"))
        .andExpect(status().isNoContent());

    verify(customFieldService).deleteDefinition(1L);
  }

  @Test
  void getValues_returns200() throws Exception {
    when(customFieldService.getValuesForEntity(CustomFieldEntityType.TASK, 42L))
        .thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/custom-fields/values")
                .param("entityType", "TASK")
                .param("entityId", "42"))
        .andExpect(status().isOk());
  }
}
