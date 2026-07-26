package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.UserDTO;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscribeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushSubscriptionResponse;
import com.github.farzadsedaghatbin.shipflow.dto.push.PushUnsubscribeRequest;
import com.github.farzadsedaghatbin.shipflow.dto.push.VapidPublicKeyResponse;
import com.github.farzadsedaghatbin.shipflow.service.PushSubscriptionService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class PushSubscriptionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private PushSubscriptionService pushSubscriptionService;
  @MockBean private UserService userService;

  private UserDTO mockCurrentUser(Long id, String username) {
    UserDTO dto = UserDTO.builder().id(id).username(username).build();
    when(userService.findByUsername(username)).thenReturn(dto);
    return dto;
  }

  @Test
  @DisplayName("GET /api/push/vapid-public-key — unauthenticated → 401/403")
  void getVapidPublicKey_unauthenticated_isRejected() throws Exception {
    mockMvc.perform(get("/api/push/vapid-public-key")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "alice", roles = {"DEVELOPER"})
  @DisplayName("GET /api/push/vapid-public-key — authenticated → returns key + enabled flag")
  void getVapidPublicKey_authenticated_returnsKey() throws Exception {
    when(pushSubscriptionService.getVapidPublicKey())
        .thenReturn(VapidPublicKeyResponse.builder().publicKey("test-public-key").enabled(true).build());

    mockMvc
        .perform(get("/api/push/vapid-public-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicKey").value("test-public-key"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  @WithMockUser(username = "alice", roles = {"DEVELOPER"})
  @DisplayName("POST /api/push/subscribe — registers subscription for the current user")
  void subscribe_registersForCurrentUser() throws Exception {
    mockCurrentUser(7L, "alice");

    PushSubscribeRequest request =
        PushSubscribeRequest.builder()
            .endpoint("https://push.example.com/xyz")
            .p256dhKey("p256dh-key")
            .authKey("auth-key")
            .userAgent("Mozilla/5.0")
            .build();

    PushSubscriptionResponse response =
        PushSubscriptionResponse.builder()
            .id(1L)
            .endpoint("https://push.example.com/xyz")
            .createdAt(OffsetDateTime.now())
            .build();

    when(pushSubscriptionService.subscribe(eq(7L), any(PushSubscribeRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/push/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.endpoint").value("https://push.example.com/xyz"))
        .andExpect(jsonPath("$.id").value(1));

    verify(pushSubscriptionService).subscribe(eq(7L), any(PushSubscribeRequest.class));
  }

  @Test
  @WithMockUser(username = "alice", roles = {"DEVELOPER"})
  @DisplayName("POST /api/push/subscribe — missing required field → 400")
  void subscribe_missingEndpoint_returns400() throws Exception {
    mockCurrentUser(7L, "alice");

    String invalidBody = "{\"p256dhKey\":\"p\",\"authKey\":\"a\"}";

    mockMvc
        .perform(
            post("/api/push/subscribe").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "alice", roles = {"DEVELOPER"})
  @DisplayName("DELETE /api/push/unsubscribe — removes subscription for the current user")
  void unsubscribe_removesForCurrentUser() throws Exception {
    mockCurrentUser(7L, "alice");

    PushUnsubscribeRequest request =
        PushUnsubscribeRequest.builder().endpoint("https://push.example.com/xyz").build();

    mockMvc
        .perform(
            delete("/api/push/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    verify(pushSubscriptionService).unsubscribe(7L, "https://push.example.com/xyz");
  }
}
