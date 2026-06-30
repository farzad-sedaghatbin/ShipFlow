package com.github.farzadsedaghatbin.shipflow.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeSourceMigrationTest {
  @Autowired JdbcTemplate jdbc;

  @Test
  void table_exists_with_expected_columns() {
    Integer cols = jdbc.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='KNOWLEDGE_SOURCES'",
        Integer.class);
    assertThat(cols).isGreaterThanOrEqualTo(14); // 14 because we dropped organization_id
  }

  @Test
  void knowledge_items_has_source_fk() {
    Integer present = jdbc.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE TABLE_NAME='KNOWLEDGE_ITEMS' AND COLUMN_NAME='KNOWLEDGE_SOURCE_ID'",
        Integer.class);
    assertThat(present).isEqualTo(1);
  }
}
