package com.github.farzadsedaghatbin.shipflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.CreateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardWidgetDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.UpdateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidget;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardWidgetRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
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

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardWidgetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DashboardWidgetRepository dashboardWidgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonRepository personRepository;

    private User testUser;
    private DashboardWidget testWidget;

    @BeforeEach
    void setUp() {
        dashboardWidgetRepository.deleteAll();
        userRepository.deleteAll();
        personRepository.deleteAll();
        
        // Create test person and user
        Person testPerson = Person.builder()
                .name("Test User")
                .email("test@example.com")
                .isActive(true)
                .build();
        testPerson = personRepository.save(testPerson);

        testUser = User.builder()
                .username("testuser")
                .password("password")
                .role(UserRole.DEVELOPER)
                .person(testPerson)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);

        // Create test widget
        testWidget = DashboardWidget.builder()
                .user(testUser)
                .widgetType("STATS_CARDS")
                .isVisible(true)
                .displayOrder(0)
                .build();
        testWidget = dashboardWidgetRepository.save(testWidget);
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void getUserWidgets_ShouldReturnWidgets() throws Exception {
        mockMvc.perform(get("/api/dashboard/widgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].widgetType").value("STATS_CARDS"))
                .andExpect(jsonPath("$[0].isVisible").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void getVisibleWidgets_ShouldReturnOnlyVisibleWidgets() throws Exception {
        // Create hidden widget
        DashboardWidget hiddenWidget = DashboardWidget.builder()
                .user(testUser)
                .widgetType("HIDDEN_WIDGET")
                .isVisible(false)
                .displayOrder(1)
                .build();
        dashboardWidgetRepository.save(hiddenWidget);

        mockMvc.perform(get("/api/dashboard/widgets/visible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isVisible").value(true));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void createWidget_WithValidData_ShouldCreateWidget() throws Exception {
        CreateDashboardWidgetRequest request = CreateDashboardWidgetRequest.builder()
                .widgetType("NEW_WIDGET")
                .isVisible(true)
                .displayOrder(5)
                .build();

        mockMvc.perform(post("/api/dashboard/widgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.widgetType").value("NEW_WIDGET"))
                .andExpect(jsonPath("$.isVisible").value(true))
                .andExpect(jsonPath("$.displayOrder").value(5));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void updateWidget_WhenExists_ShouldUpdateWidget() throws Exception {
        UpdateDashboardWidgetRequest request = UpdateDashboardWidgetRequest.builder()
                .isVisible(false)
                .displayOrder(10)
                .build();

        mockMvc.perform(put("/api/dashboard/widgets/" + testWidget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVisible").value(false))
                .andExpect(jsonPath("$.displayOrder").value(10));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void bulkUpdateWidgets_ShouldUpdateMultipleWidgets() throws Exception {
        DashboardWidget widget2 = DashboardWidget.builder()
                .user(testUser)
                .widgetType("WIDGET_2")
                .isVisible(true)
                .displayOrder(1)
                .build();
        widget2 = dashboardWidgetRepository.save(widget2);

        DashboardWidgetDTO dto1 = DashboardWidgetDTO.builder()
                .id(testWidget.getId())
                .userId(testUser.getId())
                .widgetType("STATS_CARDS")
                .isVisible(false)
                .displayOrder(5)
                .build();

        DashboardWidgetDTO dto2 = DashboardWidgetDTO.builder()
                .id(widget2.getId())
                .userId(testUser.getId())
                .widgetType("WIDGET_2")
                .isVisible(true)
                .displayOrder(1)
                .build();

        List<DashboardWidgetDTO> updates = Arrays.asList(dto1, dto2);

        mockMvc.perform(put("/api/dashboard/widgets/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void deleteWidget_WhenExists_ShouldDeleteWidget() throws Exception {
        mockMvc.perform(delete("/api/dashboard/widgets/" + testWidget.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "DEVELOPER")
    void resetToDefaults_ShouldResetWidgets() throws Exception {
        // First delete existing widgets to avoid conflict
        dashboardWidgetRepository.deleteByUserId(testUser.getId());
        
        mockMvc.perform(post("/api/dashboard/widgets/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getUserWidgets_WithoutAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/dashboard/widgets"))
                .andExpect(status().is3xxRedirection()); // Spring Security redirects to login
    }
}
