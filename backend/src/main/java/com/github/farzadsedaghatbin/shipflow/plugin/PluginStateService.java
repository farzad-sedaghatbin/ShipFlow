package com.github.farzadsedaghatbin.shipflow.plugin;

import com.github.farzadsedaghatbin.shipflow.entity.PluginState;
import com.github.farzadsedaghatbin.shipflow.repository.PluginStateRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the {@link PluginRegistry}'s in-memory enabled flags and the {@code plugin_states} table
 * in step. The table is the source of truth across restarts; the registry is the fast read path.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PluginStateService {

  private final PluginRegistry registry;
  private final PluginStateRepository repository;

  /**
   * Loads saved choices into the registry at startup. A plugin seen for the first time gets a row
   * holding its default ({@link UiContributionPlugin#isEnabledByDefault()}, otherwise enabled).
   */
  @Transactional
  public void syncOnStartup() {
    Map<String, PluginState> saved =
        repository.findAll().stream()
            .collect(Collectors.toMap(PluginState::getPluginId, Function.identity()));
    registry
        .getAllPluginDTOs()
        .forEach(
            dto -> {
              PluginState state = saved.get(dto.getPluginId());
              if (state == null) {
                boolean enabled = registry.defaultEnabled(dto.getPluginId());
                repository.save(
                    new PluginState(dto.getPluginId(), enabled, OffsetDateTime.now()));
                registry.setEnabled(dto.getPluginId(), enabled);
              } else {
                registry.setEnabled(dto.getPluginId(), state.isEnabled());
              }
            });
  }

  /** Persists the choice, then applies it. */
  @Transactional
  public void setEnabled(String pluginId, boolean enabled) {
    PluginState state =
        repository.findById(pluginId).orElseGet(() -> new PluginState(pluginId, enabled, null));
    state.setEnabled(enabled);
    state.setUpdatedAt(OffsetDateTime.now());
    repository.save(state);
    registry.setEnabled(pluginId, enabled);
  }
}
