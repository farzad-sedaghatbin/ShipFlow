package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.RegisterRequest;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full HTTP-layer coverage for the app.auth.public-registration=false gate, kept
 * in its own test class (separate Spring context via @TestPropertySource) so it
 * doesn't affect the registration-enabled assumptions the rest of
 * {@link AuthControllerIntegrationTest} relies on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.auth.public-registration=false"})
@Transactional
class AuthControllerRegistrationDisabledIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    User admin = User.builder().username("existing-admin").password(passwordEncoder.encode("password123"))
        .role(UserRole.ADMIN).isActive(true).build();
    userRepository.save(admin);
  }

  @Test
  void register_WhenPublicRegistrationDisabled_AnonymousCaller_Returns403() throws Exception {
    RegisterRequest request = new RegisterRequest("someone", "password123", UserRole.MEMBER, null, null);

    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isForbidden())
        .andExpect(jsonPath("$.messageKey", is("auth.registration.disabled")));
  }

  @Test
  @WithMockUser(username = "existing-admin")
  void register_WhenPublicRegistrationDisabled_AdminCaller_StillSucceeds() throws Exception {
    // The "Add User" flow in User Management reuses this same endpoint while
    // authenticated as an admin — it must keep working even on a production
    // instance that has public self-registration turned off.
    RegisterRequest request = new RegisterRequest("addedbyadmin", "password123", UserRole.MANAGER, null, null);

    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("addedbyadmin"))).andExpect(jsonPath("$.role", is("MANAGER")));
  }
}
