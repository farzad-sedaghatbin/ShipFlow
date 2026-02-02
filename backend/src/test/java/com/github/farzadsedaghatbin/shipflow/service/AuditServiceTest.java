package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO.RevisionType;
import com.github.farzadsedaghatbin.shipflow.dto.audit.FieldChangeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.audit.AuditRevisionEntity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType as EnversRevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private MessageService messageService;

    @Mock
    private AuditReader auditReader;

    @Mock
    private AuditQuery auditQuery;

    @InjectMocks
    private AuditService auditService;

    private Task testTask1;
    private Task testTask2;
    private Person testAssignee;
    private AuditRevisionEntity revisionEntity1;
    private AuditRevisionEntity revisionEntity2;

    @BeforeEach
    void setUp() {
        lenient().when(messageService.getMessage(anyString())).thenReturn("test message");

        testAssignee = Person.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        // First revision - task creation
        testTask1 = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.BACKLOG)
                .priority(TaskPriority.MEDIUM)
                .category(TaskCategory.PITCH_SCOPE)
                .build();

        // Second revision - task updated (status changed)
        testTask2 = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .category(TaskCategory.PITCH_SCOPE)
                .assignee(testAssignee)
                .build();

        revisionEntity1 = new AuditRevisionEntity();
        revisionEntity1.setId(1);
        revisionEntity1.setTimestamp(System.currentTimeMillis() - 10000);
        revisionEntity1.setModifiedBy("admin");

        revisionEntity2 = new AuditRevisionEntity();
        revisionEntity2.setId(2);
        revisionEntity2.setTimestamp(System.currentTimeMillis());
        revisionEntity2.setModifiedBy("user1");
    }

    @Nested
    @DisplayName("getTaskHistory")
    class GetTaskHistoryTests {

        @Test
        @DisplayName("should return empty page when no revisions exist")
        void shouldReturnEmptyPageWhenNoRevisionsExist() {
            try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
                mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.createQuery()).thenReturn(auditQuery);
                when(auditQuery.forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
                when(auditQuery.add(any())).thenReturn(auditQuery);
                when(auditQuery.addOrder(any())).thenReturn(auditQuery);
                when(auditQuery.getResultList()).thenReturn(new ArrayList<>());

                Pageable pageable = PageRequest.of(0, 20);
                Page<EntityHistoryDTO> result = auditService.getTaskHistory(999L, pageable);

                assertThat(result).isEmpty();
                assertThat(result.getTotalElements()).isEqualTo(0);
            }
        }

        @Test
        @DisplayName("should return history with creation revision")
        void shouldReturnHistoryWithCreationRevision() {
            try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
                mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.createQuery()).thenReturn(auditQuery);
                when(auditQuery.forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
                when(auditQuery.add(any())).thenReturn(auditQuery);
                when(auditQuery.addOrder(any())).thenReturn(auditQuery);

                List<Object[]> revisions = new ArrayList<>();
                revisions.add(new Object[]{testTask1, revisionEntity1, EnversRevisionType.ADD});
                when(auditQuery.getResultList()).thenReturn(revisions);

                Pageable pageable = PageRequest.of(0, 20);
                Page<EntityHistoryDTO> result = auditService.getTaskHistory(1L, pageable);

                assertThat(result).isNotEmpty();
                assertThat(result.getTotalElements()).isEqualTo(1);

                EntityHistoryDTO historyEntry = result.getContent().get(0);
                assertThat(historyEntry.getRevisionNumber()).isEqualTo(1L);
                assertThat(historyEntry.getModifiedBy()).isEqualTo("admin");
                assertThat(historyEntry.getRevisionType()).isEqualTo(RevisionType.CREATED);
                assertThat(historyEntry.getChanges()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should compute field diffs between revisions")
        void shouldComputeFieldDiffsBetweenRevisions() {
            try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
                mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.createQuery()).thenReturn(auditQuery);
                when(auditQuery.forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
                when(auditQuery.add(any())).thenReturn(auditQuery);
                when(auditQuery.addOrder(any())).thenReturn(auditQuery);

                // Two revisions - creation and modification
                List<Object[]> revisions = new ArrayList<>();
                revisions.add(new Object[]{testTask2, revisionEntity2, EnversRevisionType.MOD});
                revisions.add(new Object[]{testTask1, revisionEntity1, EnversRevisionType.ADD});
                when(auditQuery.getResultList()).thenReturn(revisions);

                Pageable pageable = PageRequest.of(0, 20);
                Page<EntityHistoryDTO> result = auditService.getTaskHistory(1L, pageable);

                assertThat(result).hasSize(2);

                // First entry (newest) should be the modification
                EntityHistoryDTO modEntry = result.getContent().get(0);
                assertThat(modEntry.getRevisionType()).isEqualTo(RevisionType.MODIFIED);
                assertThat(modEntry.getModifiedBy()).isEqualTo("user1");

                // Should show status change from BACKLOG to IN_PROGRESS
                List<FieldChangeDTO> changes = modEntry.getChanges();
                assertThat(changes).isNotEmpty();
                
                FieldChangeDTO statusChange = changes.stream()
                        .filter(c -> "status".equals(c.getFieldName()))
                        .findFirst()
                        .orElse(null);
                assertThat(statusChange).isNotNull();
                assertThat(statusChange.getOldValue()).isEqualTo("BACKLOG");
                assertThat(statusChange.getNewValue()).isEqualTo("IN_PROGRESS");

                // Should show priority change from MEDIUM to HIGH
                FieldChangeDTO priorityChange = changes.stream()
                        .filter(c -> "priority".equals(c.getFieldName()))
                        .findFirst()
                        .orElse(null);
                assertThat(priorityChange).isNotNull();
                assertThat(priorityChange.getOldValue()).isEqualTo("MEDIUM");
                assertThat(priorityChange.getNewValue()).isEqualTo("HIGH");

                // Should show assignee change from null to John Doe
                FieldChangeDTO assigneeChange = changes.stream()
                        .filter(c -> "assignee".equals(c.getFieldName()))
                        .findFirst()
                        .orElse(null);
                assertThat(assigneeChange).isNotNull();
                assertThat(assigneeChange.getOldValue()).isNull();
                assertThat(assigneeChange.getNewValue()).isEqualTo("John Doe");
            }
        }

        @Test
        @DisplayName("should respect pagination")
        void shouldRespectPagination() {
            try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
                mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.createQuery()).thenReturn(auditQuery);
                when(auditQuery.forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
                when(auditQuery.add(any())).thenReturn(auditQuery);
                when(auditQuery.addOrder(any())).thenReturn(auditQuery);

                // Create multiple revisions
                List<Object[]> revisions = new ArrayList<>();
                for (int i = 5; i >= 1; i--) {
                    AuditRevisionEntity rev = new AuditRevisionEntity();
                    rev.setId(i);
                    rev.setTimestamp(System.currentTimeMillis() - (i * 1000));
                    rev.setModifiedBy("user" + i);
                    revisions.add(new Object[]{testTask1, rev, i == 1 ? EnversRevisionType.ADD : EnversRevisionType.MOD});
                }
                when(auditQuery.getResultList()).thenReturn(revisions);

                // Request second page with size 2
                Pageable pageable = PageRequest.of(1, 2);
                Page<EntityHistoryDTO> result = auditService.getTaskHistory(1L, pageable);

                assertThat(result.getTotalElements()).isEqualTo(5);
                assertThat(result.getTotalPages()).isEqualTo(3);
                assertThat(result.getNumber()).isEqualTo(1);
                assertThat(result.getContent()).hasSize(2);
            }
        }

        @Test
        @DisplayName("should handle system user for unauthenticated changes")
        void shouldHandleSystemUserForUnauthenticatedChanges() {
            try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
                mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
                when(auditReader.createQuery()).thenReturn(auditQuery);
                when(auditQuery.forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
                when(auditQuery.add(any())).thenReturn(auditQuery);
                when(auditQuery.addOrder(any())).thenReturn(auditQuery);

                AuditRevisionEntity revWithNullUser = new AuditRevisionEntity();
                revWithNullUser.setId(1);
                revWithNullUser.setTimestamp(System.currentTimeMillis());
                revWithNullUser.setModifiedBy(null);

                List<Object[]> revisions = new ArrayList<>();
                revisions.add(new Object[]{testTask1, revWithNullUser, EnversRevisionType.ADD});
                when(auditQuery.getResultList()).thenReturn(revisions);

                Pageable pageable = PageRequest.of(0, 20);
                Page<EntityHistoryDTO> result = auditService.getTaskHistory(1L, pageable);

                assertThat(result.getContent().get(0).getModifiedBy()).isEqualTo("system");
            }
        }
    }
}
