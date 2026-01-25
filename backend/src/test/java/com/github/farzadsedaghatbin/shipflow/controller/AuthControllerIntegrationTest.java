package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.LoginRequest;
import com.github.farzadsedaghatbin.shipflow.dto.RegisterRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Person testPerson;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        personRepository.deleteAll();

        testPerson = Person.builder()
                .name("Test User")
                .email("test@example.com")
                .skills("Testing")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        testPerson = personRepository.save(testPerson);

        testUser = User.builder()
                .username("auth-test-user")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.MEMBER)
                .person(testPerson)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() throws Exception {
        LoginRequest request = new LoginRequest("auth-test-user", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.type", is("Bearer")))
                .andExpect(jsonPath("$.username", is("auth-test-user")))
                .andExpect(jsonPath("$.role", is("MEMBER")));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithValidData_ShouldCreateUser() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "newpassword", UserRole.MEMBER, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.role", is("MEMBER")));
    }

    @Test
    void register_WithExistingUsername_ShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("auth-test-user", "somepassword", UserRole.MEMBER, null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "auth-test-user")
    void getCurrentUser_WhenAuthenticated_ShouldReturnUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("auth-test-user")))
                .andExpect(jsonPath("$.role", is("MEMBER")));
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ShouldReturnError() throws Exception {
        // When calling /api/auth/me without authentication, the endpoint tries to get the user
        // but there is no authenticated user, so it returns an error
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
