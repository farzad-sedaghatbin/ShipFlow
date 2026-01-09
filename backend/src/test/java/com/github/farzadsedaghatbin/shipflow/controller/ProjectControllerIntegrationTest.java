package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CycleRepository cycleRepository;

    private Project testProject;

    @BeforeEach
    void setUp() {
        cycleRepository.deleteAll();
        projectRepository.deleteAll();

        testProject = Project.builder()
                .name("Test Project")
                .projectKey("TST")
                .description("Test Description")
                .color("#FF0000")
                .isActive(true)
                .build();
        testProject = projectRepository.save(testProject);
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    void getAllProjects_ShouldReturnProjects() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Test Project")));
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    void getActiveProjects_ShouldReturnActiveProjects() throws Exception {
        mockMvc.perform(get("/api/projects/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "QA")
    void getProjectById_WhenExists_ShouldReturnProject() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testProject.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Project")))
                .andExpect(jsonPath("$.projectKey", is("TST")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProjectById_WhenNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProjectByKey_WhenExists_ShouldReturnProject() throws Exception {
        mockMvc.perform(get("/api/projects/key/{key}", "TST"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectKey", is("TST")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProject_WithValidData_ShouldCreateProject() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .projectKey("NEW")
                .description("New project description")
                .color("#00FF00")
                .build();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Project")))
                .andExpect(jsonPath("$.projectKey", is("NEW")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProject_WithDuplicateKey_ShouldReturn400() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Another Project")
                .projectKey("TST")
                .build();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    void createProject_WithInsufficientPermissions_ShouldReturn403() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("New Project")
                .projectKey("NEW")
                .build();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProject_WhenExists_ShouldUpdateProject() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("Updated Project")
                .projectKey("TST")
                .description("Updated description")
                .color("#0000FF")
                .build();

        mockMvc.perform(put("/api/projects/{id}", testProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Project")))
                .andExpect(jsonPath("$.color", is("#0000FF")));
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void updateProject_WithProjectManager_ShouldUpdateProject() throws Exception {
        CreateProjectRequest request = CreateProjectRequest.builder()
                .name("PM Updated Project")
                .projectKey("TST")
                .build();

        mockMvc.perform(put("/api/projects/{id}", testProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PM Updated Project")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateProject_ShouldDeactivate() throws Exception {
        mockMvc.perform(post("/api/projects/{id}/deactivate", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(false)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateProject_ShouldActivate() throws Exception {
        // First deactivate
        testProject.setIsActive(false);
        projectRepository.save(testProject);

        mockMvc.perform(post("/api/projects/{id}/activate", testProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProject_WhenNoCycles_ShouldDeleteProject() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", testProject.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", testProject.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PROJECT_MANAGER")
    void deleteProject_WithInsufficientPermissions_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", testProject.getId()))
                .andExpect(status().isForbidden());
    }
}
