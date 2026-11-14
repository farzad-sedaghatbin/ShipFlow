package com.github.farzadsedaghatbin.shipflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginRegistry;
import com.github.farzadsedaghatbin.shipflow.plugin.builtin.PluginShowcasePlugin;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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

/** Contract tests for the UI-plugin endpoints and the admin toggle that drives them. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PluginUiControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PluginRegistry registry;

  @BeforeEach
  void createUsers() {
    userRepository.save(
        User.builder().username("plugin-admin").password("x").role(UserRole.ADMIN).isActive(true).build());
    userRepository.save(
        User.builder().username("plugin-member").password("x").role(UserRole.MEMBER).isActive(true).build());
  }

  @AfterEach
  void restoreDefault() {
    registry.setEnabled(PluginShowcasePlugin.PLUGIN_ID, false);
  }

  @Test
  void unauthenticatedRequestIsRejected() throws Exception {
    // Depending on the security profile this is a 401/403 or a redirect to sign-in; never a 200.
    mockMvc
        .perform(get("/api/v1/plugins/ui/menu"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
  }

  @Test
  @WithMockUser(username = "plugin-member", roles = {"MEMBER"})
  void showcaseIsHiddenUntilAnAdminEnablesIt() throws Exception {
    mockMvc.perform(get("/api/v1/plugins/ui/menu")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    mockMvc.perform(get("/api/v1/plugins/ui/{p}/pages/overview", PluginShowcasePlugin.PLUGIN_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "plugin-admin", roles = {"ADMIN"})
  void enablingShowcaseExposesMenuAndPage() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/admin/plugins/{p}/enabled", PluginShowcasePlugin.PLUGIN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("ui"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.version").value("1.0.0"));

    mockMvc
        .perform(get("/api/v1/plugins/ui/menu"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].pluginId", hasItem(PluginShowcasePlugin.PLUGIN_ID)));

    mockMvc
        .perform(get("/api/v1/plugins/ui/{p}/pages/overview", PluginShowcasePlugin.PLUGIN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Plugin Showcase"))
        .andExpect(jsonPath("$.blocks[*].type", hasItem("STAT_CARDS")));
  }

  @Test
  @WithMockUser(username = "plugin-member", roles = {"MEMBER"})
  void memberCannotToggleAPlugin() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/admin/plugins/{p}/enabled", PluginShowcasePlugin.PLUGIN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true}"))
        .andExpect(status().isForbidden());
  }
}
