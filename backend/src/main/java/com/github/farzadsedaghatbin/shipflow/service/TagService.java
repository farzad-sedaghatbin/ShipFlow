package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.TagDTO;
import com.github.farzadsedaghatbin.shipflow.dto.TagRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.Tag;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TagRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing tags that correlate retro items and pitches.
 * Part of v0.5 "Insight, Not Metrics" - enables linking retrospective items to future work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagService {

  private final TagRepository tagRepository;
  private final ProjectRepository projectRepository;
  private final RetroItemRepository retroItemRepository;
  private final PitchRepository pitchRepository;
  private final UserRepository userRepository;

  @Cacheable(value = "tags", key = "'byProject:' + #projectId")
  @Transactional(readOnly = true)
  public List<TagDTO> getTagsByProject(Long projectId) {
    return tagRepository.findByProjectIdOrderByNameAsc(projectId)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TagDTO> searchTags(Long projectId, String query) {
    return tagRepository.searchByNameInProjectOrdered(projectId, query)
        .stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @CacheEvict(value = "tags", allEntries = true)
  @Transactional
  public TagDTO createTag(TagRequest request) {
    if (tagRepository.existsByNameAndProjectId(request.getName(), request.getProjectId())) {
      throw new BadRequestException("Tag with name '" + request.getName() + "' already exists in this project");
    }

    Project project = projectRepository.findById(request.getProjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + request.getProjectId()));

    Tag tag = Tag.builder()
        .name(request.getName())
        .project(project)
        .color(request.getColor() != null ? request.getColor() : "#6366f1")
        .description(request.getDescription())
        .createdBy(getCurrentUser())
        .build();

    return toDTO(tagRepository.save(tag));
  }

  @CacheEvict(value = "tags", allEntries = true)
  @Transactional
  public TagDTO updateTag(Long tagId, TagRequest request) {
    Tag tag = tagRepository.findById(tagId)
        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));

    // Check for duplicate name if changing
    if (!tag.getName().equals(request.getName()) && 
        tagRepository.existsByNameAndProjectId(request.getName(), tag.getProject().getId())) {
      throw new BadRequestException("Tag with name '" + request.getName() + "' already exists in this project");
    }

    tag.setName(request.getName());
    if (request.getColor() != null) {
      tag.setColor(request.getColor());
    }
    if (request.getDescription() != null) {
      tag.setDescription(request.getDescription());
    }

    return toDTO(tagRepository.save(tag));
  }

  @CacheEvict(value = "tags", allEntries = true)
  @Transactional
  public void deleteTag(Long tagId) {
    tagRepository.deleteById(tagId);
  }

  @Transactional
  public void addTagToRetroItem(Long retroItemId, Long tagId) {
    RetroItem item = retroItemRepository.findById(retroItemId)
        .orElseThrow(() -> new ResourceNotFoundException("RetroItem not found: " + retroItemId));
    Tag tag = tagRepository.findById(tagId)
        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));

    // Enforce project boundary: tag and retro item must belong to same project
    Long retroProjectId = item.getRetrospective().getCycle().getProject().getId();
    if (!tag.getProject().getId().equals(retroProjectId)) {
      throw new BadRequestException("Tag from project " + tag.getProject().getId() + " cannot be linked to retro item from project " + retroProjectId);
    }

    item.getTags().add(tag);
    retroItemRepository.save(item);
  }

  @Transactional
  public void removeTagFromRetroItem(Long retroItemId, Long tagId) {
    RetroItem item = retroItemRepository.findById(retroItemId)
        .orElseThrow(() -> new ResourceNotFoundException("RetroItem not found: " + retroItemId));
    
    item.getTags().removeIf(t -> t.getId().equals(tagId));
    retroItemRepository.save(item);
  }

  @Transactional
  public void addTagToPitch(Long pitchId, Long tagId) {
    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + pitchId));
    Tag tag = tagRepository.findById(tagId)
        .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));

    // Enforce project boundary: tag and pitch must belong to same project
    Long pitchProjectId = pitch.getCycle().getProject().getId();
    if (!tag.getProject().getId().equals(pitchProjectId)) {
      throw new BadRequestException("Tag from project " + tag.getProject().getId() + " cannot be linked to pitch from project " + pitchProjectId);
    }

    pitch.getTags().add(tag);
    pitchRepository.save(pitch);
  }

  @Transactional
  public void removeTagFromPitch(Long pitchId, Long tagId) {
    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + pitchId));
    
    pitch.getTags().removeIf(t -> t.getId().equals(tagId));
    pitchRepository.save(pitch);
  }

  @Transactional(readOnly = true)
  public List<TagDTO> getTagsForRetroItem(Long retroItemId) {
    RetroItem item = retroItemRepository.findById(retroItemId)
        .orElseThrow(() -> new ResourceNotFoundException("RetroItem not found: " + retroItemId));
    return item.getTags().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TagDTO> getTagsForPitch(Long pitchId) {
    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + pitchId));
    return pitch.getTags().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * Find pitches that share tags with a retro item.
   * Used for "link to future bet" suggestions.
   */
  @Transactional(readOnly = true)
  public List<Pitch> findRelatedPitches(Long retroItemId) {
    RetroItem item = retroItemRepository.findById(retroItemId)
        .orElseThrow(() -> new ResourceNotFoundException("RetroItem not found: " + retroItemId));

    if (item.getTags().isEmpty()) {
      return List.of();
    }

    // Get all pitches that share any tag with this retro item
    return item.getTags().stream()
        .flatMap(tag -> tag.getPitches().stream())
        .filter(pitch -> pitch.getDeletedAt() == null)
        .distinct()
        .collect(Collectors.toList());
  }

  private TagDTO toDTO(Tag tag) {
    return TagDTO.builder()
        .id(tag.getId())
        .name(tag.getName())
        .projectId(tag.getProject().getId())
        .color(tag.getColor())
        .description(tag.getDescription())
        .createdAt(tag.getCreatedAt())
        .createdByUsername(tag.getCreatedBy() != null ? tag.getCreatedBy().getUsername() : null)
        .retroItemCount(tagRepository.countRetroItemsByTagId(tag.getId()))
        .pitchCount(tagRepository.countPitchesByTagId(tag.getId()))
        .build();
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByUsername(username).orElse(null);
  }
}
