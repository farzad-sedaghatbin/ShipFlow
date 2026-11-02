package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The lightweight "what is this link?" lookup behind social/chat link previews.
 *
 * <p>Link previews are fetched by crawlers, which carry no login, so whatever this returns is
 * readable by anyone who has — or can guess — the URL. It therefore returns <strong>general,
 * non-sensitive facts only</strong>: the item's title, what kind of item it is, and a couple of
 * status-style labels. It deliberately never returns free text a person wrote (a task or bug
 * description, a pitch's problem statement, comments), which is where credentials, customer
 * details and internal links end up.
 *
 * <p>Soft-deleted items resolve to empty, exactly as if they did not exist, so deleting something
 * also withdraws its preview. Bug reports have no soft-delete, so a deleted bug is simply absent.
 *
 * <p>Wiki spaces with per-space grants are private, so for those this returns only "Wiki space" or
 * "Wiki page", never the space name or page title. That is the same test the Knowledge Center uses
 * to keep restricted spaces out of AI answers.
 *
 * <p>To preview a new kind of item: add a method here that maps it to a {@link LinkPreview}, an
 * endpoint in {@code LinkPreviewController}, and a path in {@code SocialCrawlerFilter}. Keep the
 * summary to type/status-style labels.
 */
@Service
@RequiredArgsConstructor
public class LinkPreviewService {

  /** What a preview card shows: a title, and one line of general context under it. */
  public record LinkPreview(String title, String summary) {
  }

  private final TaskRepository taskRepository;
  private final PitchRepository pitchRepository;
  private final BugReportRepository bugReportRepository;
  private final CycleRepository cycleRepository;
  private final InitiativeRepository initiativeRepository;
  private final WikiSpaceRepository wikiSpaceRepository;
  private final WikiPageRepository wikiPageRepository;
  private final WikiSpacePermissionRepository wikiSpacePermissionRepository;

  @Transactional(readOnly = true)
  public Optional<LinkPreview> forTask(Long id) {
    return taskRepository.findById(id).filter(task -> task.getDeletedAt() == null).map(this::toPreview);
  }

  /**
   * Bugs are linked by key ({@code BUG-150}, the URL the bug page settles on) as well as by
   * numeric id (older links), so this accepts either — the same rule the bug page itself uses.
   */
  @Transactional(readOnly = true)
  public Optional<LinkPreview> forBug(String idOrKey) {
    // 18 digits always fits a long; anything longer is treated as a key (and simply not found).
    boolean numeric = idOrKey.length() <= 18 && idOrKey.chars().allMatch(Character::isDigit);
    Optional<BugReport> bug = numeric
        ? bugReportRepository.findById(Long.valueOf(idOrKey))
        : bugReportRepository.findByBugKey(idOrKey);
    return bug.map(this::toPreview);
  }

  @Transactional(readOnly = true)
  public Optional<LinkPreview> forPitch(Long id) {
    // The NotDeleted query, not findById: findById would still resolve a soft-deleted pitch.
    return pitchRepository.findByIdNotDeleted(id).map(this::toPreview);
  }

  @Transactional(readOnly = true)
  public Optional<LinkPreview> forCycle(Long id) {
    return cycleRepository.findById(id).map(this::toPreview);
  }

  /**
   * The roadmap URL carries no id (the project comes from the viewer's session), so there is
   * nothing specific to look up — but naming the page still beats the generic site card.
   */
  public LinkPreview forRoadmap() {
    return new LinkPreview("Roadmap", "Roadmap · Initiatives and timeline");
  }

  @Transactional(readOnly = true)
  public Optional<LinkPreview> forInitiative(Long id) {
    return initiativeRepository.findByIdNotDeleted(id).map(this::toPreview);
  }

  @Transactional(readOnly = true)
  public Optional<LinkPreview> forWikiSpace(Long spaceId) {
    return liveSpace(spaceId).map(space -> {
      if (isRestricted(space)) {
        return new LinkPreview("Wiki space", "Wiki space · Private");
      }
      long pages = wikiPageRepository.countBySpaceIdAndDeletedAtIsNull(space.getId());
      return new LinkPreview(space.getName(), "Wiki space · %d page%s".formatted(pages, pages == 1 ? "" : "s"));
    });
  }

  /** The page must belong to the space in the URL, so an id from another space is not found. */
  @Transactional(readOnly = true)
  public Optional<LinkPreview> forWikiPage(Long spaceId, Long pageId) {
    return liveSpace(spaceId).flatMap(space -> wikiPageRepository.findById(pageId)
        .filter(page -> page.getDeletedAt() == null && space.getId().equals(page.getSpaceId()))
        .map(page -> isRestricted(space)
            ? new LinkPreview("Wiki page", "Wiki page · Private")
            : new LinkPreview(page.getTitle(), "Wiki page · " + space.getName())));
  }

  private Optional<WikiSpace> liveSpace(Long spaceId) {
    return wikiSpaceRepository.findById(spaceId).filter(space -> space.getDeletedAt() == null);
  }

  private boolean isRestricted(WikiSpace space) {
    return wikiSpacePermissionRepository.existsBySpaceId(space.getId());
  }

  private LinkPreview toPreview(Initiative initiative) {
    return new LinkPreview(initiative.getName(), join("Initiative", label(initiative.getStatus())));
  }

  private LinkPreview toPreview(Task task) {
    return new LinkPreview(task.getTitle(), join("Task", label(task.getStatus()), label(task.getPriority())));
  }

  private LinkPreview toPreview(BugReport bug) {
    return new LinkPreview(bug.getTitle(), join("Bug report", label(bug.getSeverity()), label(bug.getStatus())));
  }

  private LinkPreview toPreview(Pitch pitch) {
    return new LinkPreview(pitch.getTitle(), join("Pitch", label(pitch.getStatus())));
  }

  private LinkPreview toPreview(Cycle cycle) {
    // Counted with a query, NOT via cycle.getPitches().size(): that collection is lazy, and touching
    // it outside a session threw LazyInitializationException, which made every cycle preview a 500.
    // The count query also excludes soft-deleted pitches, which the collection did not.
    long pitchCount = pitchRepository.countByCycleIdNotDeleted(cycle.getId());
    String summary = String.format("Cycle: %d pitch%s · %s – %s", pitchCount, pitchCount == 1 ? "" : "es",
        cycle.getStartDate(), cycle.getEndDate());
    return new LinkPreview(cycle.getName(), summary);
  }

  /** "Task · In Progress · High" — skips any part that is not set. */
  private static String join(String type, String... labels) {
    return Stream.concat(Stream.of(type), Stream.of(labels)).filter(s -> s != null && !s.isBlank())
        .collect(Collectors.joining(" · "));
  }

  /** {@code IN_PROGRESS} → {@code In Progress}; null stays null so it is skipped. */
  static String label(Enum<?> value) {
    if (value == null) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    for (String word : value.name().split("_")) {
      if (word.isEmpty()) {
        continue;
      }
      if (out.length() > 0) {
        out.append(' ');
      }
      out.append(word.charAt(0)).append(word.substring(1).toLowerCase());
    }
    return out.toString();
  }
}
