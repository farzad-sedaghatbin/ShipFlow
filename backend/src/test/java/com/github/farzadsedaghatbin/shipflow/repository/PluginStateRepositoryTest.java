package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.PluginState;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PluginStateRepositoryTest {

  @Autowired private PluginStateRepository repository;

  @Test
  void choiceRoundTripsAndCanBeFlipped() {
    repository.saveAndFlush(new PluginState("demo-plugin", false, OffsetDateTime.now()));

    PluginState loaded = repository.findById("demo-plugin").orElseThrow();
    assertThat(loaded.isEnabled()).isFalse();

    loaded.setEnabled(true);
    repository.saveAndFlush(loaded);
    assertThat(repository.findById("demo-plugin").orElseThrow().isEnabled()).isTrue();
  }
}
