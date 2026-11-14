package com.github.farzadsedaghatbin.shipflow.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.PluginPageException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PluginUiServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserService userService;

  private PluginRegistry registry;
  private PluginUiService service;

  @BeforeEach
  void setUp() {
    registry = new PluginRegistry();
    service = new PluginUiService(registry, new PluginUiSanitizer(), userRepository, userService);
  }

  private void signedInAs(String username, UserRole role) {
    User user = User.builder().id(7L).username(username).role(role).build();
    lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
  }

  private static UiContributionPlugin plugin(
      String id, PluginMenuItem.RequiredRole role, PluginPage page) {
    return new UiContributionPlugin() {
      public String getPluginId() { return id; }
      public String getDisplayName() { return id; }
      public List<PluginMenuItem> getMenuItems() {
        return List.of(new PluginMenuItem("home", java.util.Map.of("en", "Home"), "chart", role));
      }
      public PluginPage getPage(String key, PluginPageContext ctx) {
        if (page == null) { throw new IllegalStateException("boom"); }
        return page;
      }
    };
  }

  @Test
  void menuHidesPluginsAboveTheCallersRole() {
    registry.registerUiPlugin(plugin("open", PluginMenuItem.RequiredRole.ANY, new PluginPage("o", List.of())));
    registry.registerUiPlugin(plugin("admin-only", PluginMenuItem.RequiredRole.ADMIN, new PluginPage("a", List.of())));
    signedInAs("mia", UserRole.MEMBER);
    signedInAs("root", UserRole.ADMIN);

    assertThat(service.menuFor("mia")).extracting(m -> m.getPluginId()).containsExactly("open");
    assertThat(service.menuFor("root")).extracting(m -> m.getPluginId()).containsExactly("open", "admin-only");
  }

  @Test
  void disabledPluginIsNeitherListedNorServed() {
    registry.registerUiPlugin(plugin("open", PluginMenuItem.RequiredRole.ANY, new PluginPage("o", List.of())));
    registry.setEnabled("open", false);
    signedInAs("mia", UserRole.MEMBER);

    assertThat(service.menuFor("mia")).isEmpty();
    assertThatThrownBy(() -> service.pageFor("mia", "open", "home", null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void pageIsForbiddenBelowRequiredRoleEvenWhenCalledDirectly() {
    registry.registerUiPlugin(plugin("admin-only", PluginMenuItem.RequiredRole.ADMIN, new PluginPage("a", List.of())));
    signedInAs("mia", UserRole.MANAGER);

    assertThatThrownBy(() -> service.pageFor("mia", "admin-only", "home", null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void unknownPluginOrKeyIsNotFound() {
    registry.registerUiPlugin(plugin("open", PluginMenuItem.RequiredRole.ANY, new PluginPage("o", List.of())));
    signedInAs("mia", UserRole.MEMBER);

    assertThatThrownBy(() -> service.pageFor("mia", "nope", "home", null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.pageFor("mia", "open", "missing", null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.pageFor("mia", "../open", "home", null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void projectTheCallerCannotAccessIsRefused() {
    registry.registerUiPlugin(plugin("open", PluginMenuItem.RequiredRole.ANY, new PluginPage("o", List.of())));
    signedInAs("mia", UserRole.MEMBER);
    when(userService.getAccessibleProjectIdsForUser(anyLong())).thenReturn(Set.of(1L));

    assertThatThrownBy(() -> service.pageFor("mia", "open", "home", 99L))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(service.pageFor("mia", "open", "home", 1L).getTitle()).isEqualTo("o");
  }

  @Test
  void adminMayUseAnyProject() {
    registry.registerUiPlugin(plugin("open", PluginMenuItem.RequiredRole.ANY, new PluginPage("o", List.of())));
    signedInAs("root", UserRole.ADMIN);
    when(userService.getAccessibleProjectIdsForUser(anyLong())).thenReturn(null);

    assertThat(service.pageFor("root", "open", "home", 99L).getPageKey()).isEqualTo("home");
  }

  @Test
  void throwingPluginIsIsolated() {
    registry.registerUiPlugin(plugin("broken", PluginMenuItem.RequiredRole.ANY, null));
    registry.registerUiPlugin(plugin("fine", PluginMenuItem.RequiredRole.ANY, new PluginPage("f", List.of())));
    signedInAs("mia", UserRole.MEMBER);

    assertThatThrownBy(() -> service.pageFor("mia", "broken", "home", null))
        .isInstanceOf(PluginPageException.class);
    assertThat(service.pageFor("mia", "fine", "home", null).getTitle()).isEqualTo("f");
    assertThat(service.menuFor("mia")).hasSize(2);
  }
}
