package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.comment.*;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.Comment;
import com.github.farzadsedaghatbin.shipflow.entity.CommentReactionEntity;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CommentReactionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CommentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing comments and reactions on tasks and bug reports.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final BugReportRepository bugReportRepository;
    private final MessageService messageService;

    /**
     * Create a new comment.
     */
    public CommentDTO createComment(CreateCommentRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.user.not.found")));

        // Validate entity exists
        validateEntityExists(request.getEntityType(), request.getEntityId());

        Comment comment = Comment.builder()
                .content(request.getContent())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .author(author)
                .build();

        comment = commentRepository.save(comment);
        log.info("Created comment {} on {} {} by user {}", 
                comment.getId(), request.getEntityType(), request.getEntityId(), userId);

        return toDTO(comment, userId);
    }

    /**
     * Update an existing comment.
     */
    public CommentDTO updateComment(Long commentId, UpdateCommentRequest request, Long userId) {
        Comment comment = commentRepository.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.not.found")));

        // Check if user can edit
        if (!canUserEdit(comment, userId)) {
            throw new IllegalArgumentException(
                    messageService.getMessage("comment.edit.unauthorized"));
        }

        comment.setContent(request.getContent());
        comment.setIsEdited(true);
        comment = commentRepository.save(comment);

        log.info("Updated comment {} by user {}", commentId, userId);
        return toDTO(comment, userId);
    }

    /**
     * Delete a comment (soft delete).
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.not.found")));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.user.not.found")));

        // Check if user can delete
        if (!canUserDelete(comment, user)) {
            throw new IllegalArgumentException(
                    messageService.getMessage("comment.delete.unauthorized"));
        }

        comment.softDelete();
        commentRepository.save(comment);
        log.info("Deleted comment {} by user {}", commentId, userId);
    }

    /**
     * Get all comments for an entity.
     */
    @Transactional(readOnly = true)
    public List<CommentDTO> getComments(CommentEntityType entityType, Long entityId, Long userId) {
        List<Comment> comments = commentRepository.findByEntityTypeAndEntityIdNotDeleted(entityType, entityId);
        return comments.stream()
                .map(c -> toDTO(c, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get comments for an entity with pagination.
     */
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsPaginated(
            CommentEntityType entityType, Long entityId, Long userId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByEntityTypeAndEntityIdNotDeleted(
                entityType, entityId, pageable);
        return comments.map(c -> toDTO(c, userId));
    }

    /**
     * Get a single comment by ID.
     */
    @Transactional(readOnly = true)
    public CommentDTO getComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.not.found")));
        return toDTO(comment, userId);
    }

    /**
     * Add or toggle a reaction on a comment.
     */
    public CommentDTO toggleReaction(Long commentId, ReactionRequest request, Long userId) {
        Comment comment = commentRepository.findByIdNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.not.found")));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage("comment.user.not.found")));

        Optional<CommentReactionEntity> existingReaction = reactionRepository
                .findByCommentIdAndUserIdAndReactionType(commentId, userId, request.getReactionType());

        if (existingReaction.isPresent()) {
            // Remove existing reaction (toggle off)
            reactionRepository.delete(existingReaction.get());
            log.info("Removed reaction {} from comment {} by user {}", 
                    request.getReactionType(), commentId, userId);
        } else {
            // Add new reaction
            CommentReactionEntity reaction = CommentReactionEntity.builder()
                    .comment(comment)
                    .user(user)
                    .reactionType(request.getReactionType())
                    .build();
            reactionRepository.save(reaction);
            log.info("Added reaction {} to comment {} by user {}", 
                    request.getReactionType(), commentId, userId);
        }

        return toDTO(comment, userId);
    }

    /**
     * Get comment count for an entity.
     */
    @Transactional(readOnly = true)
    public long getCommentCount(CommentEntityType entityType, Long entityId) {
        return commentRepository.countByEntityTypeAndEntityIdNotDeleted(entityType, entityId);
    }

    /**
     * Get available reaction types with emojis.
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAvailableReactions() {
        return Arrays.stream(CommentReaction.values())
                .map(r -> Map.of(
                        "type", r.name(),
                        "emoji", r.getEmoji()))
                .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    private void validateEntityExists(CommentEntityType entityType, Long entityId) {
        switch (entityType) {
            case TASK:
                if (!taskRepository.existsById(entityId)) {
                    throw new IllegalArgumentException(
                            messageService.getMessage("comment.task.not.found"));
                }
                break;
            case BUG_REPORT:
                if (!bugReportRepository.existsById(entityId)) {
                    throw new IllegalArgumentException(
                            messageService.getMessage("comment.bug.not.found"));
                }
                break;
        }
    }

    private boolean canUserEdit(Comment comment, Long userId) {
        return comment.getAuthor().getId().equals(userId);
    }

    private boolean canUserDelete(Comment comment, User user) {
        // Author can delete their own comments
        if (comment.getAuthor().getId().equals(user.getId())) {
            return true;
        }
        // Admins and Managers can delete any comment
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;
    }

    private CommentDTO toDTO(Comment comment, Long currentUserId) {
        User author = comment.getAuthor();
        String authorName = author.getPerson() != null 
                ? author.getPerson().getName() 
                : author.getUsername();

        // Get current user for permission checks
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .entityType(comment.getEntityType())
                .entityId(comment.getEntityId())
                .authorId(author.getId())
                .authorName(authorName)
                .authorUsername(author.getUsername())
                .isEdited(comment.getIsEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .reactions(buildReactionSummary(comment.getId(), currentUserId))
                .canEdit(canUserEdit(comment, currentUserId))
                .canDelete(currentUser != null && canUserDelete(comment, currentUser))
                .build();
    }

    private ReactionSummaryDTO buildReactionSummary(Long commentId, Long currentUserId) {
        // Get reaction counts
        List<Object[]> countResults = reactionRepository.getReactionCountsByCommentId(commentId);
        Map<CommentReaction, Integer> reactionCounts = new EnumMap<>(CommentReaction.class);
        int totalReactions = 0;

        for (Object[] result : countResults) {
            CommentReaction type = (CommentReaction) result[0];
            int count = ((Number) result[1]).intValue();
            reactionCounts.put(type, count);
            totalReactions += count;
        }

        // Get user's own reactions
        List<CommentReaction> userReactionTypes = reactionRepository
                .findUserReactionsByCommentIdAndUserId(commentId, currentUserId);
        Map<CommentReaction, Boolean> userReactions = new EnumMap<>(CommentReaction.class);
        for (CommentReaction type : CommentReaction.values()) {
            userReactions.put(type, userReactionTypes.contains(type));
        }

        return ReactionSummaryDTO.builder()
                .reactionCounts(reactionCounts)
                .userReactions(userReactions)
                .totalReactions(totalReactions)
                .build();
    }
}
