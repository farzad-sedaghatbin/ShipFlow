package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RetroService {

    private final RetroRepository retroRepository;
    private final RetroItemRepository retroItemRepository;
    private final RetroItemVoteRepository retroItemVoteRepository;
    private final CycleRepository cycleRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ==================== RETRO CRUD ====================

    public List<RetroDTO> getAllRetrosByProject(Long projectId) {
        validateRetrospectivesEnabled(projectId);
        return retroRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<RetroDTO> getRetrosByCycle(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
        
        if (cycle.getProject() != null) {
            validateRetrospectivesEnabled(cycle.getProject().getId());
        }
        
        return retroRepository.findByCycleIdOrderByCreatedAtDesc(cycleId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RetroDTO getRetroById(Long id) {
        Retrospective retro = retroRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        return toDTO(retro);
    }

    public RetroDTO getRetroWithItems(Long id) {
        Retrospective retro = retroRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        return toDTOWithItems(retro);
    }

    public RetroDTO createRetro(CreateRetroRequest request) {
        validateRetrospectivesEnabled(request.getProjectId());
        
        Cycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + request.getCycleId()));
        
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
        
        User currentUser = getCurrentUser();
        
        Retrospective retro = Retrospective.builder()
                .title(request.getTitle())
                .notes(request.getNotes())
                .status(RetroStatus.DRAFT)
                .cycle(cycle)
                .project(project)
                .createdBy(currentUser)
                .build();
        
        Retrospective saved = retroRepository.save(retro);
        return toDTO(saved);
    }

    public RetroDTO updateRetro(Long id, UpdateRetroRequest request) {
        Retrospective retro = retroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot update a closed retrospective");
        }
        
        if (request.getTitle() != null) {
            retro.setTitle(request.getTitle());
        }
        if (request.getNotes() != null) {
            retro.setNotes(request.getNotes());
        }
        
        Retrospective saved = retroRepository.save(retro);
        return toDTO(saved);
    }

    public RetroDTO openRetro(Long id) {
        Retrospective retro = retroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot open a closed retrospective");
        }
        
        retro.setStatus(RetroStatus.OPEN);
        Retrospective saved = retroRepository.save(retro);
        return toDTO(saved);
    }

    public RetroDTO closeRetro(Long id) {
        Retrospective retro = retroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + id));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        retro.setStatus(RetroStatus.CLOSED);
        retro.setClosedAt(LocalDateTime.now());
        Retrospective saved = retroRepository.save(retro);
        return toDTO(saved);
    }

    public void deleteRetro(Long id) {
        if (!retroRepository.existsById(id)) {
            throw new ResourceNotFoundException("Retrospective not found with id: " + id);
        }
        retroRepository.deleteById(id);
    }

    // ==================== RETRO ITEMS ====================

    public List<RetroItemDTO> getItemsByRetro(Long retroId) {
        Retrospective retro = retroRepository.findById(retroId)
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + retroId));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        return retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retroId)
                .stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    public RetroItemDTO createRetroItem(CreateRetroItemRequest request) {
        Retrospective retro = retroRepository.findById(request.getRetrospectiveId())
                .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + request.getRetrospectiveId()));
        
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot add items to a closed retrospective");
        }
        
        User currentUser = getCurrentUser();
        
        RetroItem item = RetroItem.builder()
                .content(request.getContent())
                .columnType(request.getColumnType())
                .retrospective(retro)
                .author(currentUser)
                .build();
        
        RetroItem saved = retroItemRepository.save(item);
        return toItemDTO(saved);
    }

    public RetroItemDTO updateRetroItem(Long itemId, String content) {
        RetroItem item = retroItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));
        
        Retrospective retro = item.getRetrospective();
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot update items in a closed retrospective");
        }
        
        item.setContent(content);
        RetroItem saved = retroItemRepository.save(item);
        return toItemDTO(saved);
    }

    public void deleteRetroItem(Long itemId) {
        RetroItem item = retroItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));
        
        Retrospective retro = item.getRetrospective();
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot delete items from a closed retrospective");
        }
        
        retroItemRepository.deleteById(itemId);
    }

    // ==================== VOTING ====================

    public RetroItemDTO toggleVote(Long itemId) {
        RetroItem item = retroItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));
        
        Retrospective retro = item.getRetrospective();
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot vote on items in a closed retrospective");
        }
        
        // Cannot vote on merged items
        if (item.getMergedInto() != null) {
            throw new IllegalStateException("Cannot vote on a merged item");
        }
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User not found");
        }
        
        boolean hasVoted = retroItemVoteRepository.existsByRetroItemIdAndUserId(itemId, currentUser.getId());
        
        if (hasVoted) {
            // Remove vote
            retroItemVoteRepository.deleteByRetroItemIdAndUserId(itemId, currentUser.getId());
            item.setVoteCount(Math.max(0, item.getVoteCount() - 1));
        } else {
            // Add vote
            RetroItemVote vote = RetroItemVote.builder()
                    .retroItem(item)
                    .user(currentUser)
                    .build();
            retroItemVoteRepository.save(vote);
            item.setVoteCount(item.getVoteCount() + 1);
        }
        
        RetroItem saved = retroItemRepository.save(item);
        return toItemDTO(saved);
    }

    // ==================== MERGING ====================

    public RetroItemDTO mergeItems(Long targetItemId, Long sourceItemId) {
        RetroItem targetItem = retroItemRepository.findById(targetItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Target item not found with id: " + targetItemId));
        
        RetroItem sourceItem = retroItemRepository.findById(sourceItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Source item not found with id: " + sourceItemId));
        
        // Validate same retrospective
        if (!targetItem.getRetrospective().getId().equals(sourceItem.getRetrospective().getId())) {
            throw new IllegalStateException("Cannot merge items from different retrospectives");
        }
        
        // Validate same column
        if (targetItem.getColumnType() != sourceItem.getColumnType()) {
            throw new IllegalStateException("Cannot merge items from different columns");
        }
        
        Retrospective retro = targetItem.getRetrospective();
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot merge items in a closed retrospective");
        }
        
        // Cannot merge into an already merged item
        if (targetItem.getMergedInto() != null) {
            throw new IllegalStateException("Cannot merge into an already merged item");
        }
        
        // Cannot merge an item that is already merged
        if (sourceItem.getMergedInto() != null) {
            throw new IllegalStateException("Source item is already merged");
        }
        
        // Transfer votes from source to target
        List<RetroItemVote> sourceVotes = retroItemVoteRepository.findByRetroItemId(sourceItemId);
        for (RetroItemVote vote : sourceVotes) {
            // Only add if user hasn't already voted on target
            if (!retroItemVoteRepository.existsByRetroItemIdAndUserId(targetItemId, vote.getUser().getId())) {
                RetroItemVote newVote = RetroItemVote.builder()
                        .retroItem(targetItem)
                        .user(vote.getUser())
                        .build();
                retroItemVoteRepository.save(newVote);
                targetItem.setVoteCount(targetItem.getVoteCount() + 1);
            }
        }
        
        // Mark source as merged
        sourceItem.setMergedInto(targetItem);
        sourceItem.setVoteCount(0); // Reset vote count on merged item
        retroItemRepository.save(sourceItem);
        
        // Save target with updated vote count
        RetroItem saved = retroItemRepository.save(targetItem);
        return toItemDTO(saved);
    }

    public RetroItemDTO unmergeItem(Long itemId) {
        RetroItem item = retroItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));
        
        if (item.getMergedInto() == null) {
            throw new IllegalStateException("Item is not merged");
        }
        
        Retrospective retro = item.getRetrospective();
        if (retro.getProject() != null) {
            validateRetrospectivesEnabled(retro.getProject().getId());
        }
        
        if (retro.getStatus() == RetroStatus.CLOSED) {
            throw new IllegalStateException("Cannot unmerge items in a closed retrospective");
        }
        
        item.setMergedInto(null);
        RetroItem saved = retroItemRepository.save(item);
        return toItemDTO(saved);
    }

    // ==================== CYCLE RETRO STATUS ====================

    public CycleRetroStatusDTO getCycleRetroStatus(Long cycleId) {
        Cycle cycle = cycleRepository.findByIdWithProject(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
        
        boolean retroEnabled = cycle.getProject() != null && 
                               Boolean.TRUE.equals(cycle.getProject().getEnableRetrospectives());
        
        long totalRetros = retroRepository.countByCycleId(cycleId);
        long closedRetros = retroRepository.countByCycleIdAndStatus(cycleId, RetroStatus.CLOSED);
        
        boolean canClose;
        String message;
        
        if (!retroEnabled) {
            // If retrospectives are disabled, cycle can be closed without retro requirement
            canClose = true;
            message = "Retrospectives are disabled for this project";
        } else if (closedRetros >= 1) {
            canClose = true;
            message = "Cycle can be closed - retrospective completed";
        } else {
            canClose = false;
            message = "You can't close this cycle yet — create and close at least one Retro for this cycle.";
        }
        
        return CycleRetroStatusDTO.builder()
                .cycleId(cycleId)
                .cycleName(cycle.getName())
                .totalRetros((int) totalRetros)
                .closedRetros((int) closedRetros)
                .canCloseCycle(canClose)
                .message(message)
                .build();
    }

    public boolean canCloseCycle(Long cycleId) {
        CycleRetroStatusDTO status = getCycleRetroStatus(cycleId);
        return status.getCanCloseCycle();
    }

    // ==================== PROJECT SETTING ====================

    public boolean isRetrospectivesEnabled(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        return Boolean.TRUE.equals(project.getEnableRetrospectives());
    }

    public void setRetrospectivesEnabled(Long projectId, boolean enabled) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        project.setEnableRetrospectives(enabled);
        projectRepository.save(project);
    }

    // ==================== HELPERS ====================

    private void validateRetrospectivesEnabled(Long projectId) {
        if (!isRetrospectivesEnabled(projectId)) {
            throw new IllegalStateException("Retrospectives feature is disabled for this project");
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private RetroDTO toDTO(Retrospective retro) {
        RetroDTO.RetroDTOBuilder builder = RetroDTO.builder()
                .id(retro.getId())
                .title(retro.getTitle())
                .notes(retro.getNotes())
                .status(retro.getStatus())
                .createdAt(retro.getCreatedAt())
                .updatedAt(retro.getUpdatedAt())
                .closedAt(retro.getClosedAt());
        
        if (retro.getCycle() != null) {
            builder.cycleId(retro.getCycle().getId())
                   .cycleName(retro.getCycle().getName());
        }
        
        if (retro.getProject() != null) {
            builder.projectId(retro.getProject().getId())
                   .projectName(retro.getProject().getName())
                   .projectKey(retro.getProject().getProjectKey());
        }
        
        if (retro.getCreatedBy() != null) {
            builder.createdById(retro.getCreatedBy().getId())
                   .createdByName(retro.getCreatedBy().getUsername());
        }
        
        // Count items
        long itemCount = retroItemRepository.countByRetrospectiveId(retro.getId());
        builder.itemCount((int) itemCount);
        
        return builder.build();
    }

    private RetroDTO toDTOWithItems(Retrospective retro) {
        RetroDTO dto = toDTO(retro);
        
        List<RetroItemDTO> items = retro.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        
        dto.setItems(items);
        return dto;
    }

    private RetroItemDTO toItemDTO(RetroItem item) {
        User currentUser = getCurrentUser();
        
        RetroItemDTO.RetroItemDTOBuilder builder = RetroItemDTO.builder()
                .id(item.getId())
                .content(item.getContent())
                .columnType(item.getColumnType())
                .retrospectiveId(item.getRetrospective().getId())
                .voteCount(item.getVoteCount() != null ? item.getVoteCount() : 0)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt());
        
        if (item.getAuthor() != null) {
            builder.authorId(item.getAuthor().getId())
                   .authorName(item.getAuthor().getUsername());
        }
        
        // Check if current user has voted
        if (currentUser != null) {
            boolean hasVoted = retroItemVoteRepository.existsByRetroItemIdAndUserId(item.getId(), currentUser.getId());
            builder.hasVoted(hasVoted);
        } else {
            builder.hasVoted(false);
        }
        
        // Merged into
        if (item.getMergedInto() != null) {
            builder.mergedIntoId(item.getMergedInto().getId());
        }
        
        // Get merged item IDs
        List<Long> mergedItemIds = retroItemRepository.findByMergedIntoId(item.getId())
                .stream()
                .map(RetroItem::getId)
                .collect(Collectors.toList());
        builder.mergedItemIds(mergedItemIds);
        
        return builder.build();
    }
}
