package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.comment.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CommentService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReactionRepository reactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BugReportRepository bugReportRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private DashboardNotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private User adminUser;
    private Comment testComment;
    private Task testTask;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(UserRole.MEMBER)
                .isActive(true)
                .build();

        // Setup admin user
        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        // Setup test task
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .build();

        // Setup test comment
        testComment = Comment.builder()
                .id(1L)
                .content("Test comment content")
                .entityType(CommentEntityType.TASK)
                .entityId(1L)
                .author(testUser)
                .isEdited(false)
                .createdAt(LocalDateTime.now())
                .reactions(new ArrayList<>())
                .build();

        // Setup message service defaults
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Create Comment Tests")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create comment successfully")
        void shouldCreateCommentSuccessfully() {
            // Arrange
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("New comment")
                    .entityType(CommentEntityType.TASK)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.existsById(1L)).thenReturn(true);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.createComment(request, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("New comment");
            assertThat(result.getEntityType()).isEqualTo(CommentEntityType.TASK);
            assertThat(result.getAuthorId()).isEqualTo(1L);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            // Arrange
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("New comment")
                    .entityType(CommentEntityType.TASK)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> commentService.createComment(request, 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should fail when task not found")
        void shouldFailWhenTaskNotFound() {
            // Arrange
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("New comment")
                    .entityType(CommentEntityType.TASK)
                    .entityId(999L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> commentService.createComment(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should create comment on bug report")
        void shouldCreateCommentOnBugReport() {
            // Arrange
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("Bug comment")
                    .entityType(CommentEntityType.BUG_REPORT)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(bugReportRepository.existsById(1L)).thenReturn(true);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(2L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.createComment(request, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEntityType()).isEqualTo(CommentEntityType.BUG_REPORT);
        }
    }

    @Nested
    @DisplayName("Update Comment Tests")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should update comment successfully by author")
        void shouldUpdateCommentByAuthor() {
            // Arrange
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .build();

            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // Act
            CommentDTO result = commentService.updateComment(1L, request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should fail update when not author")
        void shouldFailUpdateWhenNotAuthor() {
            // Arrange
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .build();

            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));

            // Act & Assert
            assertThatThrownBy(() -> commentService.updateComment(1L, request, 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should fail when comment not found")
        void shouldFailWhenCommentNotFound() {
            // Arrange
            UpdateCommentRequest request = UpdateCommentRequest.builder()
                    .content("Updated content")
                    .build();

            when(commentRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> commentService.updateComment(999L, request, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Delete Comment Tests")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete comment by author")
        void shouldDeleteCommentByAuthor() {
            // Arrange
            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

            // Act
            commentService.deleteComment(1L, 1L);

            // Assert
            verify(commentRepository).save(any(Comment.class));
            assertThat(testComment.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should delete comment by admin")
        void shouldDeleteCommentByAdmin() {
            // Arrange
            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
            when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

            // Act
            commentService.deleteComment(1L, 2L);

            // Assert
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should fail delete when unauthorized")
        void shouldFailDeleteWhenUnauthorized() {
            // Arrange
            User otherUser = User.builder()
                    .id(3L)
                    .username("other")
                    .role(UserRole.MEMBER)
                    .build();

            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));

            // Act & Assert
            assertThatThrownBy(() -> commentService.deleteComment(1L, 3L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Reaction Tests")
    class ReactionTests {

        @Test
        @DisplayName("Should add reaction")
        void shouldAddReaction() {
            // Arrange
            ReactionRequest request = ReactionRequest.builder()
                    .reactionType(CommentReaction.THUMBS_UP)
                    .build();

            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByCommentIdAndUserIdAndReactionType(1L, 1L, CommentReaction.THUMBS_UP))
                    .thenReturn(Optional.empty());
            when(reactionRepository.save(any(CommentReactionEntity.class)))
                    .thenReturn(CommentReactionEntity.builder().id(1L).build());
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.toggleReaction(1L, request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(reactionRepository).save(any(CommentReactionEntity.class));
        }

        @Test
        @DisplayName("Should remove reaction when toggled")
        void shouldRemoveReactionWhenToggled() {
            // Arrange
            ReactionRequest request = ReactionRequest.builder()
                    .reactionType(CommentReaction.THUMBS_UP)
                    .build();

            CommentReactionEntity existingReaction = CommentReactionEntity.builder()
                    .id(1L)
                    .comment(testComment)
                    .user(testUser)
                    .reactionType(CommentReaction.THUMBS_UP)
                    .build();

            when(commentRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(testComment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(reactionRepository.findByCommentIdAndUserIdAndReactionType(1L, 1L, CommentReaction.THUMBS_UP))
                    .thenReturn(Optional.of(existingReaction));
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.toggleReaction(1L, request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(reactionRepository).delete(existingReaction);
        }
    }

    @Nested
    @DisplayName("Get Comments Tests")
    class GetCommentsTests {

        @Test
        @DisplayName("Should get comments for entity")
        void shouldGetCommentsForEntity() {
            // Arrange
            List<Comment> comments = Arrays.asList(testComment);
            when(commentRepository.findByEntityTypeAndEntityIdNotDeleted(CommentEntityType.TASK, 1L))
                    .thenReturn(comments);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<CommentDTO> result = commentService.getComments(CommentEntityType.TASK, 1L, 1L);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("Test comment content");
        }

        @Test
        @DisplayName("Should get comment count")
        void shouldGetCommentCount() {
            // Arrange
            when(commentRepository.countByEntityTypeAndEntityIdNotDeleted(CommentEntityType.TASK, 1L))
                    .thenReturn(5L);

            // Act
            long count = commentService.getCommentCount(CommentEntityType.TASK, 1L);

            // Assert
            assertThat(count).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Available Reactions Tests")
    class AvailableReactionsTests {

        @Test
        @DisplayName("Should return all available reactions")
        void shouldReturnAllAvailableReactions() {
            // Act
            List<Map<String, String>> reactions = commentService.getAvailableReactions();

            // Assert
            assertThat(reactions).hasSize(8);
            assertThat(reactions).anyMatch(r -> r.get("type").equals("THUMBS_UP") && r.get("emoji").equals("👍"));
            assertThat(reactions).anyMatch(r -> r.get("type").equals("HEART") && r.get("emoji").equals("❤️"));
            assertThat(reactions).anyMatch(r -> r.get("type").equals("ROCKET") && r.get("emoji").equals("🚀"));
        }
    }

    @Nested
    @DisplayName("Mention Tests")
    class MentionTests {

        @Test
        @DisplayName("Should search users for mention")
        void shouldSearchUsersForMention() {
            // Arrange
            User user1 = User.builder()
                    .id(1L)
                    .username("john_doe")
                    .isActive(true)
                    .build();
            User user2 = User.builder()
                    .id(2L)
                    .username("jane_doe")
                    .isActive(true)
                    .build();
            
            when(userRepository.searchByUsernameForMention("doe"))
                    .thenReturn(Arrays.asList(user1, user2));

            // Act
            List<MentionUserDTO> result = commentService.searchUsersForMention("doe");

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("john_doe");
            assertThat(result.get(1).getUsername()).isEqualTo("jane_doe");
        }

        @Test
        @DisplayName("Should return default users when search query is empty")
        void shouldReturnDefaultUsersWhenQueryEmpty() {
            // Arrange
            when(userRepository.findByIsActiveTrue())
                    .thenReturn(Arrays.asList(testUser, adminUser));

            // Act
            List<MentionUserDTO> result = commentService.searchUsersForMention("");

            // Assert
            assertThat(result).hasSize(2);
            verify(userRepository).findByIsActiveTrue();
            verify(userRepository, never()).searchByUsernameForMention(anyString());
        }

        @Test
        @DisplayName("Should process mentions and send notifications")
        void shouldProcessMentionsAndSendNotifications() {
            // Arrange
            User mentionedUser = User.builder()
                    .id(3L)
                    .username("mentioned_user")
                    .isActive(true)
                    .build();

            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("Hey @mentioned_user check this out!")
                    .entityType(CommentEntityType.TASK)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.existsById(1L)).thenReturn(true);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(userRepository.findByUsernameIn(Arrays.asList("mentioned_user")))
                    .thenReturn(Arrays.asList(mentionedUser));
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.createComment(request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationService).notifyCommentMention(
                    eq(mentionedUser),
                    eq(testUser),
                    eq("TASK"),
                    eq(1L),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should handle multiple mentions")
        void shouldHandleMultipleMentions() {
            // Arrange
            User user1 = User.builder().id(3L).username("user1").isActive(true).build();
            User user2 = User.builder().id(4L).username("user2").isActive(true).build();

            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("@user1 and @user2 please review")
                    .entityType(CommentEntityType.TASK)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.existsById(1L)).thenReturn(true);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(userRepository.findByUsernameIn(anyList()))
                    .thenReturn(Arrays.asList(user1, user2));
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.createComment(request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationService, times(2)).notifyCommentMention(
                    any(User.class),
                    eq(testUser),
                    eq("TASK"),
                    eq(1L),
                    anyString()
            );
        }

        @Test
        @DisplayName("Should not send notification when no mentions")
        void shouldNotSendNotificationWhenNoMentions() {
            // Arrange
            CreateCommentRequest request = CreateCommentRequest.builder()
                    .content("Just a regular comment without mentions")
                    .entityType(CommentEntityType.TASK)
                    .entityId(1L)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(taskRepository.existsById(1L)).thenReturn(true);
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });
            when(reactionRepository.getReactionCountsByCommentId(anyLong())).thenReturn(Collections.emptyList());
            when(reactionRepository.findUserReactionsByCommentIdAndUserId(anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());

            // Act
            CommentDTO result = commentService.createComment(request, 1L);

            // Assert
            assertThat(result).isNotNull();
            verify(notificationService, never()).notifyCommentMention(
                    any(User.class),
                    any(User.class),
                    anyString(),
                    anyLong(),
                    anyString()
            );
        }
    }
}
