package com.github.farzadsedaghatbin.shipflow.dto.knowledge;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeSourceResponse {

  private Long id;
  private String name;
  private String description;
  private KnowledgeProviderType providerType;
  private KnowledgeSourceScope scope;
  private Long teamId;
  private Long projectId;
  /** Raw JSON config — frontend re-parses. */
  private String configJson;

  private KnowledgeSourceStatus status;
  private OffsetDateTime lastIngestedAt;
  private String lastError;
  private long chunkCount;
  private OffsetDateTime createdAt;

  public static KnowledgeSourceResponse from(KnowledgeSource s, long chunkCount) {
    return KnowledgeSourceResponse.builder()
        .id(s.getId())
        .name(s.getName())
        .description(s.getDescription())
        .providerType(s.getProviderType())
        .scope(s.getScope())
        .teamId(s.getTeamId())
        .projectId(s.getProjectId())
        .configJson(s.getConfig())
        .status(s.getStatus())
        .lastIngestedAt(s.getLastIngestedAt())
        .lastError(s.getLastError())
        .chunkCount(chunkCount)
        .createdAt(s.getCreatedAt())
        .build();
  }
}
