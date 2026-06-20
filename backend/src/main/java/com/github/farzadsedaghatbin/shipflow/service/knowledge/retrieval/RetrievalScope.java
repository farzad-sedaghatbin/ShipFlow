package com.github.farzadsedaghatbin.shipflow.service.knowledge.retrieval;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Value type describing the visibility scope of a retrieval call.
 *
 * <p>Single-org deployment: there is no organization id. A retrieval request is
 * scoped by the caller's team memberships and, optionally, the active project.
 */
@Value
@Builder
public class RetrievalScope {
  Set<Long> teamIds;
  Long projectId;
}
