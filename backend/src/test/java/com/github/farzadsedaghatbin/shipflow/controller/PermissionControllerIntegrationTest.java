package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.controller.PermissionController.CreatePermissionRequest;
import com.github.farzadsedaghatbin.shipflow.controller.PermissionController.UpdatePermissionRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.PermissionRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.security.rbac.enabled=true"
})
@Transactional
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Permission testPermission;
    private User adminUser;
    private User qaUser;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        userRepository.deleteAll();
        personRepository.deleteAll();

        // Create test persons
        Person adminPerson = Person.builder()
                .name("Admin User")
                .email("admin@example.com")
                .skills("Admin")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        adminPerson = personRepository.save(adminPerson);

        Person qaPerson = Person.builder()
                .name("QA User")
                .email("qa@example.com")
                .skills("Testing")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        qaPerson = personRepository.save(qaPerson);

        // Create test users
        adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .person(adminPerson)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        adminUser = userRepository.save(adminUser);

        qaUser = User.builder()
                .username("qa")
                .password(passwordEncoder.encode("qa123"))
                .role(UserRole.MEMBER)
                .person(qaPerson)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        qaUser = userRepository.save(qaUser);

        // Create test permission
        testPermission = Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.BUG)
                .permissionType(PermissionType.CREATE)
                .description("QA can create bugs")
                .build();
        testPermission = permissionRepository.save(testPermission);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllPermissions_AsAdmin_ShouldReturnAllPermissions() throws Exception {
        mockMvc.perform(get("/api/permissions"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].role", is("MEMBER")))
                .andExpect(jsonPath("$[0].resourceType", is("BUG")))
                .andExpect(jsonPath("$[0].permissionType", is("CREATE")));
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void getAllPermissions_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPermissionsByRole_AsAdmin_ShouldReturnRolePermissions() throws Exception {
        mockMvc.perform(get("/api/permissions/role/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].role", is("MEMBER")));
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void getCurrentUserPermissions_AsQA_ShouldReturnOwnPermissions() throws Exception {
        mockMvc.perform(get("/api/permissions/my-permissions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is("qa")))
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andExpect(jsonPath("$.permissions", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void hasPermission_WithValidPermission_ShouldReturnTrue() throws Exception {
        mockMvc.perform(get("/api/permissions/has-permission")
                        .param("resourceType", "BUG")
                        .param("permissionType", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void hasPermission_WithInvalidPermission_ShouldReturnFalse() throws Exception {
        mockMvc.perform(get("/api/permissions/has-permission")
                        .param("resourceType", "SYSTEM")
                        .param("permissionType", "MANAGE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(false)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createPermission_AsAdmin_ShouldCreateNewPermission() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setRole(UserRole.MEMBER);
        request.setResourceType(ResourceType.PITCH);
        request.setPermissionType(PermissionType.APPROVE);
        request.setDescription("Developer can approve pitches");

        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("MEMBER")))
                .andExpect(jsonPath("$.resourceType", is("PITCH")))
                .andExpect(jsonPath("$.permissionType", is("APPROVE")))
                .andExpect(jsonPath("$.description", is("Developer can approve pitches")));

        // Verify in database
        List<Permission> permissions = permissionRepository.findByRole(UserRole.MEMBER);
        assertThat(permissions).anyMatch(p ->
                p.getResourceType() == ResourceType.PITCH &&
                        p.getPermissionType() == PermissionType.APPROVE
        );
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void createPermission_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setRole(UserRole.MEMBER);
        request.setResourceType(ResourceType.PITCH);
        request.setPermissionType(PermissionType.CREATE);

        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createPermission_WithDuplicate_ShouldReturnBadRequest() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setRole(UserRole.MEMBER);
        request.setResourceType(ResourceType.BUG);
        request.setPermissionType(PermissionType.CREATE);
        request.setDescription("Duplicate permission");

        mockMvc.perform(post("/api/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updatePermission_AsAdmin_ShouldUpdateDescription() throws Exception {
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setDescription("Updated: QA can create bugs for testing");

        mockMvc.perform(put("/api/permissions/" + testPermission.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated: QA can create bugs for testing")));

        // Verify in database
        Permission updated = permissionRepository.findById(testPermission.getId()).orElseThrow();
        assertThat(updated.getDescription()).isEqualTo("Updated: QA can create bugs for testing");
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void updatePermission_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setDescription("Should fail");

        mockMvc.perform(put("/api/permissions/" + testPermission.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updatePermission_WithNonExistentId_ShouldReturnBadRequest() throws Exception {
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setDescription("Updated description");

        mockMvc.perform(put("/api/permissions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deletePermission_AsAdmin_ShouldDeletePermission() throws Exception {
        Long permissionId = testPermission.getId();

        mockMvc.perform(delete("/api/permissions/" + permissionId))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(permissionRepository.findById(permissionId)).isEmpty();
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void deletePermission_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/permissions/" + testPermission.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deletePermission_WithNonExistentId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/api/permissions/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createBulkPermissions_AsAdmin_ShouldCreateMultiplePermissions() throws Exception {
        CreatePermissionRequest req1 = new CreatePermissionRequest();
        req1.setRole(UserRole.MEMBER);
        req1.setResourceType(ResourceType.CYCLE);
        req1.setPermissionType(PermissionType.READ);
        req1.setDescription("Dev can read cycles");

        CreatePermissionRequest req2 = new CreatePermissionRequest();
        req2.setRole(UserRole.MEMBER);
        req2.setResourceType(ResourceType.CYCLE);
        req2.setPermissionType(PermissionType.CREATE);
        req2.setDescription("Dev can create cycles");

        List<CreatePermissionRequest> requests = Arrays.asList(req1, req2);

        mockMvc.perform(post("/api/permissions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));

        // Verify in database - should have at least 2 new permissions created
        List<Permission> permissions = permissionRepository.findByRole(UserRole.MEMBER);
        assertThat(permissions.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void createBulkPermissions_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setRole(UserRole.MEMBER);
        request.setResourceType(ResourceType.BUG);
        request.setPermissionType(PermissionType.READ);

        mockMvc.perform(post("/api/permissions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteBulkPermissions_AsAdmin_ShouldDeleteMultiplePermissions() throws Exception {
        // Create additional permission
        Permission permission2 = Permission.builder()
                .role(UserRole.MEMBER)
                .resourceType(ResourceType.BUG)
                .permissionType(PermissionType.UPDATE)
                .description("QA can update bugs")
                .build();
        permission2 = permissionRepository.save(permission2);

        List<Long> ids = Arrays.asList(testPermission.getId(), permission2.getId());

        mockMvc.perform(delete("/api/permissions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isNoContent());

        // Verify deletions
        assertThat(permissionRepository.findById(testPermission.getId())).isEmpty();
        assertThat(permissionRepository.findById(permission2.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "qa", roles = {"MEMBER"})
    void deleteBulkPermissions_AsNonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/permissions/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(testPermission.getId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isFound());  // 302 redirect to login
    }
}
