package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.GrantPermissionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.MovePageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiSpaceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageSearchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiRevisionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiSpaceDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiSpacePermissionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiTreeNodeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpacePermission;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.event.WikiPageChangedEvent;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.wiki.WikiHistoryReader;
import lombok.extern.slf4j.Slf4j;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WikiService {

  private final WikiSpaceRepository spaceRepository;
  private final WikiPageRepository pageRepository;
  private final WikiSpacePermissionRepository permissionRepository;
  private final WikiPermissionService permissionService;
  private final WikiHistoryReader historyReader;
  private final ApplicationEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  private final KnowledgeSourceRepository knowledgeSourceRepository;

  public WikiService(
      WikiSpaceRepository spaceRepository,
      WikiPageRepository pageRepository,
      WikiSpacePermissionRepository permissionRepository,
      WikiPermissionService permissionService,
      WikiHistoryReader historyReader,
      ApplicationEventPublisher eventPublisher,
      ObjectMapper objectMapper,
      KnowledgeSourceRepository knowledgeSourceRepository) {
    this.spaceRepository = spaceRepository;
    this.pageRepository = pageRepository;
    this.permissionRepository = permissionRepository;
    this.permissionService = permissionService;
    this.historyReader = historyReader;
    this.eventPublisher = eventPublisher;
    this.objectMapper = objectMapper;
    this.knowledgeSourceRepository = knowledgeSourceRepository;
  }

  // --- Space operations ---

  public WikiSpaceDTO createSpace(CreateWikiSpaceRequest req, Long userId) {
    permissionService.requireCreateSpace(userId);
    WikiSpace space = new WikiSpace();
    space.setName(req.name());
    space.setSpaceKey(req.spaceKey() != null ? req.spaceKey() : toSlug(req.name()));
    space.setDescription(req.description());
    space.setCreatedBy(userId);
    space = spaceRepository.save(space);

    // Auto-register the wiki space as a Knowledge Center source
    try {
      KnowledgeSource ks =
          KnowledgeSource.builder()
              .name("Wiki: " + space.getName())
              .description("Auto-indexed wiki space")
              .providerType(KnowledgeProviderType.WIKI)
              .scope(KnowledgeSourceScope.ORG)
              .projectId(space.getProjectId())
              .config("{\"spaceId\":" + space.getId() + "}")
              .status(KnowledgeSourceStatus.PENDING)
              .createdBy(userId)
              .build();
      knowledgeSourceRepository.save(ks);
    } catch (Exception e) {
      log.warn(
          "Could not auto-create KnowledgeSource for wiki space {}: {}",
          space.getId(),
          e.getMessage());
    }

    return toSpaceDTO(space);
  }

  @Transactional(readOnly = true)
  public List<WikiSpaceDTO> listSpaces(Long userId) {
    List<WikiSpace> all = spaceRepository.findByDeletedAtIsNullOrderByNameAsc();
    return all.stream()
        .filter(s -> permissionService.canRead(userId, s))
        .map(this::toSpaceDTO)
        .collect(Collectors.toList());
  }

  // --- Tree ---

  @Transactional(readOnly = true)
  public List<WikiTreeNodeDTO> getTree(Long spaceId, Long userId) {
    WikiSpace space =
        spaceRepository
            .findById(spaceId)
            .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
    permissionService.requireRead(userId, space);

    List<WikiPage> allPages = pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId);
    return buildTree(allPages, null);
  }

  private List<WikiTreeNodeDTO> buildTree(List<WikiPage> allPages, Long parentId) {
    return allPages.stream()
        .filter(p -> Objects.equals(p.getParentId(), parentId))
        .sorted(Comparator.comparingInt(p -> (p.getPosition() != null ? p.getPosition() : 0)))
        .map(
            p ->
                new WikiTreeNodeDTO(
                    p.getId(),
                    p.getTitle(),
                    p.getSlug(),
                    p.getPosition() != null ? p.getPosition() : 0,
                    buildTree(allPages, p.getId())))
        .collect(Collectors.toList());
  }

  // --- Page operations ---

  @Transactional(readOnly = true)
  public WikiPageDTO getPage(Long pageId, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireRead(userId, space);
    return toPageDTO(page);
  }

  public WikiPageDTO createPage(CreateWikiPageRequest req, Long userId) {
    WikiSpace space = requireSpace(req.spaceId());
    permissionService.requireWrite(userId, space);

    WikiPage page = new WikiPage();
    page.setSpaceId(req.spaceId());
    page.setParentId(req.parentId());
    page.setTitle(req.title());
    page.setContent(req.content());
    page.setContentText(extractText(req.content()));
    page.setSlug(toSlug(req.title()));
    page.setPosition(nextPosition(req.spaceId(), req.parentId()));
    page.setCreatedBy(userId);
    page = pageRepository.save(page);

    eventPublisher.publishEvent(
        new WikiPageChangedEvent(
            page.getId(), page.getSpaceId(), WikiPageChangedEvent.ChangeType.CREATED));
    return toPageDTO(page);
  }

  public WikiPageDTO updatePage(Long pageId, UpdateWikiPageRequest req, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireWrite(userId, space);

    if (req.title() != null) {
      page.setTitle(req.title());
      page.setSlug(toSlug(req.title()));
    }
    if (req.content() != null) {
      page.setContent(req.content());
      page.setContentText(extractText(req.content()));
    }
    page = pageRepository.save(page);

    eventPublisher.publishEvent(
        new WikiPageChangedEvent(
            page.getId(), page.getSpaceId(), WikiPageChangedEvent.ChangeType.UPDATED));
    return toPageDTO(page);
  }

  public void movePage(Long pageId, MovePageRequest req, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireWrite(userId, space);

    Long newParentId = req.newParentId();

    // Cycle prevention: newParentId must not be the page itself or any descendant
    if (newParentId != null) {
      if (newParentId.equals(pageId)) {
        throw new IllegalArgumentException("Cannot move a page under itself");
      }
      Set<Long> descendants = collectDescendantIds(page.getSpaceId(), pageId);
      if (descendants.contains(newParentId)) {
        throw new IllegalArgumentException(
            "Cannot move page under one of its own descendants (cycle detected)");
      }
    }

    Long oldParentId = page.getParentId();

    if (Objects.equals(oldParentId, newParentId)) {
      // Same-parent reorder: operate on a single list to avoid double-processing.
      List<WikiPage> siblings = getSiblings(page.getSpaceId(), oldParentId, pageId);
      int insertAt = Math.min(req.newIndex(), siblings.size());
      siblings.add(insertAt, page);
      for (int i = 0; i < siblings.size(); i++) {
        siblings.get(i).setPosition(i);
      }
      page.setParentId(newParentId);
      pageRepository.saveAll(siblings);
    } else {
      // Cross-parent move: compute both lists in memory, then persist in one pass each.
      List<WikiPage> sourceSiblings = getSiblings(page.getSpaceId(), oldParentId, pageId);
      for (int i = 0; i < sourceSiblings.size(); i++) {
        sourceSiblings.get(i).setPosition(i);
      }

      List<WikiPage> destSiblings = getSiblings(page.getSpaceId(), newParentId, null);
      int insertAt = Math.min(req.newIndex(), destSiblings.size());
      destSiblings.add(insertAt, page);
      for (int i = 0; i < destSiblings.size(); i++) {
        destSiblings.get(i).setPosition(i);
      }

      page.setParentId(newParentId);
      pageRepository.saveAll(sourceSiblings);
      pageRepository.saveAll(destSiblings);
    }
  }

  public void deletePage(Long pageId, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireWrite(userId, space);

    page.setDeletedAt(OffsetDateTime.now());
    pageRepository.save(page);

    eventPublisher.publishEvent(
        new WikiPageChangedEvent(
            page.getId(), page.getSpaceId(), WikiPageChangedEvent.ChangeType.DELETED));
  }

  @Transactional(readOnly = true)
  public List<WikiRevisionDTO> getHistory(Long pageId, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireRead(userId, space);
    return historyReader.history(pageId);
  }

  public WikiPageDTO restoreRevision(Long pageId, int revision, Long userId) {
    WikiPage page = requirePage(pageId);
    WikiSpace space = requireSpace(page.getSpaceId());
    permissionService.requireWrite(userId, space);

    WikiPage historicPage =
        historyReader
            .revision(pageId, revision)
            .orElseThrow(() -> new ResourceNotFoundException("Revision not found: " + revision));

    page.setTitle(historicPage.getTitle());
    page.setContent(historicPage.getContent());
    page.setContentText(extractText(historicPage.getContent()));
    page.setSlug(toSlug(historicPage.getTitle()));
    page = pageRepository.save(page);

    eventPublisher.publishEvent(
        new WikiPageChangedEvent(
            page.getId(), page.getSpaceId(), WikiPageChangedEvent.ChangeType.RESTORED));
    return toPageDTO(page);
  }

  // --- Additional space operations ---

  @Transactional(readOnly = true)
  public WikiSpaceDTO getSpace(Long spaceId, Long userId) {
    WikiSpace space = requireSpace(spaceId);
    permissionService.requireRead(userId, space);
    return toSpaceDTO(space);
  }

  public WikiSpaceDTO updateSpace(Long spaceId, UpdateWikiSpaceRequest req, Long userId) {
    WikiSpace space = requireSpace(spaceId);
    permissionService.requireWrite(userId, space);
    if (req.name() != null) space.setName(req.name());
    if (req.description() != null) space.setDescription(req.description());
    space = spaceRepository.save(space);
    return toSpaceDTO(space);
  }

  public void deleteSpace(Long spaceId, Long userId) {
    WikiSpace space = requireSpace(spaceId);
    permissionService.requireWrite(userId, space);

    OffsetDateTime now = OffsetDateTime.now();

    // Cascade soft-delete to all non-deleted pages and remove their KC chunks via events
    List<WikiPage> activePages = pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId);
    for (WikiPage page : activePages) {
      page.setDeletedAt(now);
      pageRepository.save(page);
      eventPublisher.publishEvent(
          new WikiPageChangedEvent(
              page.getId(), spaceId, WikiPageChangedEvent.ChangeType.DELETED));
    }

    // Soft-delete the backing KnowledgeSource for this space (if it exists)
    List<KnowledgeSource> sources = knowledgeSourceRepository.findActiveByWikiSpaceId(spaceId);
    for (KnowledgeSource ks : sources) {
      ks.setDeletedAt(now);
      knowledgeSourceRepository.save(ks);
    }

    space.setDeletedAt(now);
    spaceRepository.save(space);
  }

  // --- Space permission management ---

  public WikiSpacePermissionDTO grantPermission(
      Long spaceId, GrantPermissionRequest req, Long userId) {
    WikiSpace space = requireSpace(spaceId);
    permissionService.requireWrite(userId, space);
    WikiSpacePermission perm = new WikiSpacePermission();
    perm.setSpaceId(spaceId);
    perm.setGranteeType(req.granteeType());
    perm.setGranteeRef(req.granteeRef());
    perm.setLevel(req.level());
    perm = permissionRepository.save(perm);
    return toPermissionDTO(perm);
  }

  @Transactional(readOnly = true)
  public List<WikiSpacePermissionDTO> listPermissions(Long spaceId, Long userId) {
    WikiSpace space = requireSpace(spaceId);
    permissionService.requireRead(userId, space);
    return permissionRepository.findBySpaceId(spaceId).stream()
        .map(this::toPermissionDTO)
        .collect(Collectors.toList());
  }

  public void revokePermission(Long permId, Long userId) {
    WikiSpacePermission perm =
        permissionRepository
            .findById(permId)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permId));
    WikiSpace space = requireSpace(perm.getSpaceId());
    permissionService.requireWrite(userId, space);
    permissionRepository.delete(perm);
  }

  // --- Search ---

  @Transactional(readOnly = true)
  public List<WikiPageSearchDTO> searchPages(String q, Long spaceId, Long userId) {
    List<WikiSpace> accessibleSpaces =
        spaceRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
            .filter(s -> permissionService.canRead(userId, s))
            .collect(Collectors.toList());

    if (accessibleSpaces.isEmpty()) return List.of();

    String lowerQ = q == null ? "" : q.toLowerCase();

    return accessibleSpaces.stream()
        .filter(s -> spaceId == null || s.getId().equals(spaceId))
        .flatMap(s -> pageRepository.findBySpaceIdAndDeletedAtIsNull(s.getId()).stream())
        .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(lowerQ))
        .map(p -> new WikiPageSearchDTO(p.getId(), p.getTitle(), p.getSlug()))
        .collect(Collectors.toList());
  }

  // --- Helpers ---

  private WikiPage requirePage(Long pageId) {
    return pageRepository
        .findById(pageId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("Page not found: " + pageId));
  }

  private WikiSpace requireSpace(Long spaceId) {
    return spaceRepository
        .findById(spaceId)
        .orElseThrow(() -> new ResourceNotFoundException("Space not found: " + spaceId));
  }

  private int nextPosition(Long spaceId, Long parentId) {
    List<WikiPage> siblings;
    if (parentId == null) {
      siblings =
          pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(
              spaceId);
    } else {
      siblings =
          pageRepository.findBySpaceIdAndParentIdAndDeletedAtIsNullOrderByPositionAsc(
              spaceId, parentId);
    }
    if (siblings.isEmpty()) return 0;
    return siblings.stream()
            .mapToInt(p -> p.getPosition() != null ? p.getPosition() : 0)
            .max()
            .orElse(-1)
        + 1;
  }

  private List<WikiPage> getSiblings(Long spaceId, Long parentId, Long excludeId) {
    List<WikiPage> siblings;
    if (parentId == null) {
      siblings =
          pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(
              spaceId);
    } else {
      siblings =
          pageRepository.findBySpaceIdAndParentIdAndDeletedAtIsNullOrderByPositionAsc(
              spaceId, parentId);
    }
    if (excludeId != null) {
      siblings = new ArrayList<>(siblings);
      siblings.removeIf(p -> p.getId().equals(excludeId));
    }
    return new ArrayList<>(siblings);
  }

  private Set<Long> collectDescendantIds(Long spaceId, Long rootId) {
    List<WikiPage> all = pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId);
    Set<Long> result = new HashSet<>();
    collectDescendants(all, rootId, result);
    return result;
  }

  private void collectDescendants(List<WikiPage> all, Long parentId, Set<Long> result) {
    for (WikiPage p : all) {
      if (parentId.equals(p.getParentId())) {
        result.add(p.getId());
        collectDescendants(all, p.getId(), result);
      }
    }
  }

  /**
   * Extracts plain text from a BlockNote/ProseMirror JSON document by recursively collecting all
   * {@code "text"} string nodes. Returns an empty string for null, blank, or unparseable input.
   */
  String extractText(String contentJson) {
    if (contentJson == null || contentJson.isBlank()) return "";
    try {
      JsonNode root = objectMapper.readTree(contentJson);
      StringBuilder sb = new StringBuilder();
      extractTextFromNode(root, sb);
      return sb.toString().trim();
    } catch (Exception e) {
      return "";
    }
  }

  private void extractTextFromNode(JsonNode node, StringBuilder sb) {
    if (node.isObject()) {
      JsonNode textNode = node.get("text");
      if (textNode != null && textNode.isTextual()) {
        String t = textNode.asText();
        if (!t.isBlank()) {
          if (sb.length() > 0) sb.append(" ");
          sb.append(t);
        }
      }
      node
          .fields()
          .forEachRemaining(
              entry -> {
                if (!"text".equals(entry.getKey())) {
                  extractTextFromNode(entry.getValue(), sb);
                }
              });
    } else if (node.isArray()) {
      node.forEach(child -> extractTextFromNode(child, sb));
    }
  }

  private String toSlug(String text) {
    if (text == null) return "";
    return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
  }

  // --- DTO mappers ---

  private WikiSpaceDTO toSpaceDTO(WikiSpace space) {
    return new WikiSpaceDTO(
        space.getId(),
        space.getName(),
        space.getSpaceKey(),
        space.getCreatedBy(),
        space.getCreatedAt(),
        space.getUpdatedAt());
  }

  private WikiSpacePermissionDTO toPermissionDTO(WikiSpacePermission perm) {
    return new WikiSpacePermissionDTO(
        perm.getId(),
        perm.getSpaceId(),
        perm.getGranteeType(),
        perm.getGranteeRef(),
        perm.getLevel());
  }

  private WikiPageDTO toPageDTO(WikiPage page) {
    return new WikiPageDTO(
        page.getId(),
        page.getSpaceId(),
        page.getParentId(),
        page.getTitle(),
        page.getSlug(),
        page.getContent(),
        page.getContentText(),
        page.getPosition() != null ? page.getPosition() : 0,
        page.getCreatedBy(),
        page.getCreatedAt(),
        page.getUpdatedAt());
  }
}
