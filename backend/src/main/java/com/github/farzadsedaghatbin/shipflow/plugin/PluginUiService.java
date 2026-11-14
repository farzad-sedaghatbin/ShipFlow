package com.github.farzadsedaghatbin.shipflow.plugin;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginMenuItemDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginPageDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.PluginPageException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Serves the menu and pages of {@link UiContributionPlugin}s. Enforcement lives here, not in the
 * frontend: disabled plugins are invisible, a caller below a menu item's required role gets 403,
 * a project id the caller cannot access is refused, and a plugin that throws is isolated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginUiService {

  private final PluginRegistry registry;
  private final PluginUiSanitizer sanitizer;
  private final UserRepository userRepository;
  private final UserService userService;

  public List<PluginMenuItemDTO> menuFor(String username) {
    UserRole role = userRepository.findByUsername(username).map(User::getRole).orElse(null);
    if (role == null) {
      return List.of();
    }
    List<PluginMenuItemDTO> result = new ArrayList<>();
    for (UiContributionPlugin plugin : registry.getUiPlugins()) {
      if (!registry.isEnabled(plugin.getPluginId())) {
        continue;
      }
      for (PluginMenuItem item : safeMenuItems(plugin)) {
        if (item != null && allowed(role, item.requiredRole())) {
          PluginMenuItemDTO dto = sanitizer.menuItem(plugin.getPluginId(), item);
          if (dto != null) {
            result.add(dto);
          }
        }
      }
    }
    return result;
  }

  public PluginPageDTO pageFor(String username, String pluginId, String pageKey, Long projectId) {
    if (!PluginUiSanitizer.isValidId(pluginId) || !PluginUiSanitizer.isValidId(pageKey)) {
      throw new ResourceNotFoundException("Plugin page not found");
    }
    UiContributionPlugin plugin =
        registry
            .findUiPlugin(pluginId)
            .filter(p -> registry.isEnabled(p.getPluginId()))
            .orElseThrow(() -> new ResourceNotFoundException("Plugin page not found"));
    PluginMenuItem item =
        safeMenuItems(plugin).stream()
            .filter(i -> i != null && pageKey.equals(i.key()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Plugin page not found"));

    User user =
        userRepository.findByUsername(username).orElseThrow(() -> new AccessDeniedException("denied"));
    if (!allowed(user.getRole(), item.requiredRole())) {
      throw new AccessDeniedException("Role not allowed for this plugin page");
    }
    if (projectId != null) {
      Set<Long> accessible = userService.getAccessibleProjectIdsForUser(user.getId());
      if (accessible != null && !accessible.contains(projectId)) {
        throw new AccessDeniedException("No access to the requested project");
      }
    }

    PluginPageContext context =
        new PluginPageContext(
            user.getUsername(),
            user.getRole().name(),
            projectId,
            LocaleContextHolder.getLocale().getLanguage());
    PluginPage page;
    try {
      page = plugin.getPage(pageKey, context);
    } catch (RuntimeException e) {
      log.warn("UI plugin {} failed building page {}: {}", pluginId, pageKey, e.toString());
      throw new PluginPageException("Plugin page failed", e);
    }
    if (page == null) {
      throw new ResourceNotFoundException("Plugin page not found");
    }
    return sanitizer.page(pluginId, pageKey, page);
  }

  private List<PluginMenuItem> safeMenuItems(UiContributionPlugin plugin) {
    try {
      List<PluginMenuItem> items = plugin.getMenuItems();
      return items == null ? List.of() : items;
    } catch (RuntimeException e) {
      log.warn("UI plugin {} failed listing menu items: {}", plugin.getPluginId(), e.toString());
      return List.of();
    }
  }

  static boolean allowed(UserRole role, PluginMenuItem.RequiredRole required) {
    if (role == null) {
      return false;
    }
    return switch (required) {
      case ANY -> true;
      case MANAGER -> role == UserRole.ADMIN || role == UserRole.MANAGER;
      case ADMIN -> role == UserRole.ADMIN;
    };
  }
}
