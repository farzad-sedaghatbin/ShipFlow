package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.CreateKnowledgeSourceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.KnowledgeSourceResponse;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.KnowledgeSourceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class KnowledgeSourceControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private KnowledgeSourceService svc;
  @MockBean private UserService userService;

  @BeforeEach
  void setUp() {
    UserDTO u = UserDTO.builder().id(1L).username("user").build();
    when(userService.findByUsername("user")).thenReturn(u);
  }

  private KnowledgeSourceResponse sampleResponse(long id) {
    return KnowledgeSourceResponse.builder()
        .id(id)
        .name("Engineering Wiki")
        .providerType(KnowledgeProviderType.URL)
        .scope(KnowledgeSourceScope.ORG)
        .status(KnowledgeSourceStatus.PENDING)
        .chunkCount(0L)
        .build();
  }

  @Test
  @WithMockUser(username = "user", roles = {"ADMIN"})
  void post_returns_202_with_response() throws Exception {
    CreateKnowledgeSourceRequest req = new CreateKnowledgeSourceRequest();
    req.setName("Engineering Wiki");
    req.setProviderType(KnowledgeProviderType.URL);
    req.setScope(KnowledgeSourceScope.ORG);
    req.setConfig(objectMapper.readTree("{\"url\":\"https://example.com\"}"));

    when(svc.create(any(CreateKnowledgeSourceRequest.class), eq(1L)))
        .thenReturn(sampleResponse(99L));

    mockMvc
        .perform(
            post("/api/knowledge/sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.name").value("Engineering Wiki"));

    verify(svc).create(any(CreateKnowledgeSourceRequest.class), eq(1L));
  }

  @Test
  @WithMockUser(username = "user", roles = {"ADMIN"})
  void list_org_returns_200() throws Exception {
    when(svc.listOrg(1L))
        .thenReturn(List.of(sampleResponse(11L), sampleResponse(12L)));

    mockMvc
        .perform(get("/api/knowledge/sources").param("scope", "org"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].id").value(11))
        .andExpect(jsonPath("$[1].id").value(12));

    verify(svc).listOrg(1L);
  }

  @Test
  @WithMockUser(username = "user", roles = {"ADMIN"})
  void delete_returns_204() throws Exception {
    mockMvc
        .perform(delete("/api/knowledge/sources/{id}", 7L))
        .andExpect(status().isNoContent());

    verify(svc).delete(7L, 1L);
  }
}
