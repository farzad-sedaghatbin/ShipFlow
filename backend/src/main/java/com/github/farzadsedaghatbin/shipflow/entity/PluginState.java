package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The administrator's saved enabled/disabled choice for one plugin. */
@Entity
@Table(name = "plugin_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PluginState {

  @Id
  @Column(name = "plugin_id", length = 100)
  private String pluginId;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
