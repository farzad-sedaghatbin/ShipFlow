package com.github.farzadsedaghatbin.shipflow.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.PluginState;
import com.github.farzadsedaghatbin.shipflow.plugin.builtin.PluginShowcasePlugin;
import com.github.farzadsedaghatbin.shipflow.repository.PluginStateRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PluginStateServiceTest {

  @Mock private PluginStateRepository repository;

  private PluginRegistry registry;
  private PluginStateService service;

  @BeforeEach
  void setUp() {
    registry = new PluginRegistry();
    registry.registerUiPlugin(new PluginShowcasePlugin());
    registry.registerRiskPlugin(new SampleRiskCalculatorPlugin());
    service = new PluginStateService(registry, repository);
  }

  @Test
  void firstSightUsesThePluginDefaultAndSavesIt() {
    when(repository.findAll()).thenReturn(List.of());

    service.syncOnStartup();

    assertThat(registry.isEnabled(PluginShowcasePlugin.PLUGIN_ID)).isFalse();
    assertThat(registry.isEnabled("sample-risk-heuristic")).isTrue();
    ArgumentCaptor<PluginState> saved = ArgumentCaptor.forClass(PluginState.class);
    verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(PluginState::getPluginId, PluginState::isEnabled)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(PluginShowcasePlugin.PLUGIN_ID, false),
            org.assertj.core.groups.Tuple.tuple("sample-risk-heuristic", true));
  }

  @Test
  void savedChoiceBeatsTheDefault() {
    when(repository.findAll())
        .thenReturn(
            List.of(
                new PluginState(PluginShowcasePlugin.PLUGIN_ID, true, OffsetDateTime.now()),
                new PluginState("sample-risk-heuristic", false, OffsetDateTime.now())));

    service.syncOnStartup();

    assertThat(registry.isEnabled(PluginShowcasePlugin.PLUGIN_ID)).isTrue();
    assertThat(registry.isEnabled("sample-risk-heuristic")).isFalse();
    verify(repository, never()).save(any());
  }

  @Test
  void setEnabledWritesTheRowThenTheRegistry() {
    when(repository.findById(PluginShowcasePlugin.PLUGIN_ID)).thenReturn(Optional.empty());

    service.setEnabled(PluginShowcasePlugin.PLUGIN_ID, true);

    ArgumentCaptor<PluginState> saved = ArgumentCaptor.forClass(PluginState.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().isEnabled()).isTrue();
    assertThat(saved.getValue().getUpdatedAt()).isNotNull();
    assertThat(registry.isEnabled(PluginShowcasePlugin.PLUGIN_ID)).isTrue();
  }
}
