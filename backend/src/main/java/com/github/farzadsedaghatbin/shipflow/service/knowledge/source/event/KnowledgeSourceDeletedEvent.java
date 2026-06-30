package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event;

import java.util.List;

/**
 * Published after a knowledge source is soft-deleted. Carries the ids of the cascaded
 * knowledge items so downstream consumers can purge caches or vector store entries.
 */
public record KnowledgeSourceDeletedEvent(Long sourceId, List<Long> knowledgeItemIds) {}
