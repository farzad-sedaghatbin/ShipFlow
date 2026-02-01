package com.github.farzadsedaghatbin.shipflow.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private PersonRepository personRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private Person testPerson;
  private User testUser;
  private User adminUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    personRepository.deleteAll();

    testPerson =
        Person.builder()
            .name("Test User")
            .email("test@example.com")
            .skills("Testing")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    testPerson = personRepository.save(testPerson);

    Person adminPerson =
        Person.builder()
            .name("Admin User")
            .email("admin@example.com")
            .skills("Admin")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .build();
    adminPerson = personRepository.save(adminPerson);

    testUser =
        User.builder()
            .username("user-ctrl-test-user")
            .password(passwordEncoder.encode("password123"))
            .role(UserRole.MEMBER)
            .person(testPerson)
            .isActive(true)
            .build();
    testUser = userRepository.save(testUser);

    adminUser =
        User.builder()
            .username("user-ctrl-admin")
            .password(passwordEncoder.encode("admin123"))
            .role(UserRole.ADMIN)
            .person(adminPerson)
            .isActive(true)
            .build();
    adminUser = userRepository.save(adminUser);
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-admin",
      roles = {"ADMIN"})
  void getAllUsers_AsAdmin_ShouldReturnAllUsers() throws Exception {
    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-test-user",
      roles = {"MEMBER"})
  void getAllUsers_AsNonAdmin_ShouldReturn403() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-admin",
      roles = {"ADMIN"})
  void getUserById_AsAdmin_ShouldReturnUser() throws Exception {
    mockMvc
        .perform(get("/api/users/{id}", testUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is("user-ctrl-test-user")))
        .andExpect(jsonPath("$.role", is("MEMBER")));
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-admin",
      roles = {"ADMIN"})
  void updateUserRole_AsAdmin_ShouldUpdateRole() throws Exception {
    mockMvc
        .perform(put("/api/users/{id}/role", testUser.getId()).param("role", "MANAGER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("MANAGER")));
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-test-user",
      roles = {"MEMBER"})
  void updateUserRole_AsNonAdmin_ShouldReturn403() throws Exception {
    mockMvc
        .perform(put("/api/users/{id}/role", testUser.getId()).param("role", "ADMIN"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-admin",
      roles = {"ADMIN"})
  void deactivateUser_AsAdmin_ShouldDeactivate() throws Exception {
    mockMvc
        .perform(put("/api/users/{id}/deactivate", testUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isActive", is(false)));
  }

  @Test
  @WithMockUser(
      username = "user-ctrl-admin",
      roles = {"ADMIN"})
  void activateUser_AsAdmin_ShouldActivate() throws Exception {
    // First deactivate
    testUser.setIsActive(false);
    userRepository.save(testUser);

    // Then activate
    mockMvc
        .perform(put("/api/users/{id}/activate", testUser.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isActive", is(true)));
  }
}
