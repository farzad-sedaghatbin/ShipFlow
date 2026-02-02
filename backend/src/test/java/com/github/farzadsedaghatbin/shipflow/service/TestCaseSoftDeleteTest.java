package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.TestCase;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.repository.TestCaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestCase Soft Delete Tests")
class TestCaseSoftDeleteTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TestCaseService testCaseService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .role(UserRole.MEMBER)
            .isActive(true)
            .build();

        // Setup test case
        testCase = TestCase.builder()
            .id(1L)
            .testCaseKey("TC-001")
            .title("Test Case Title")
            .description("Test Case Description")
            .type(TestCaseType.FUNCTIONAL)
            .priority(TestCasePriority.HIGH)
            .status(TestCaseStatus.READY)
            .aiGenerated(false)
            .createdBy(testUser)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Mock security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Enable feature by default (simulate @Value injection)
        try {
            var field = TestCaseService.class.getDeclaredField("testManagementEnabled");
            field.setAccessible(true);
            field.set(testCaseService, true);
        } catch (Exception e) {
            // Handle reflection issues in test
        }
    }

    @Test
    @DisplayName("Should soft delete test case successfully")
    void shouldSoftDeleteTestCaseSuccessfully() {
        // Given
        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenReturn(testCase);

        // When
        testCaseService.deleteTestCase(1L);

        // Then
        verify(testCaseRepository).save(testCase);
        assertThat(testCase.getDeletedAt()).isNotNull();
        assertThat(testCase.getDeletedBy()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should throw exception when test case not found")
    void shouldThrowExceptionWhenTestCaseNotFound() {
        // Given
        when(testCaseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> testCaseService.deleteTestCase(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test case not found: 999");
    }

    @Test
    @DisplayName("Should throw exception when test case already deleted")
    void shouldThrowExceptionWhenTestCaseAlreadyDeleted() {
        // Given
        testCase.setDeletedAt(LocalDateTime.now());
        testCase.setDeletedBy(testUser);
        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));

        // When & Then
        assertThatThrownBy(() -> testCaseService.deleteTestCase(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Test case is already deleted");
    }

    @Test
    @DisplayName("Should not return deleted test cases in getAllTestCases")
    void shouldNotReturnDeletedTestCasesInGetAllTestCases() {
        // Given
        when(testCaseRepository.findAllNotDeleted()).thenReturn(java.util.List.of());

        // When
        var result = testCaseService.getAllTestCases();

        // Then
        assertThat(result).isEmpty();
        verify(testCaseRepository).findAllNotDeleted();
        verify(testCaseRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should not return deleted test case in getTestCaseById")
    void shouldNotReturnDeletedTestCaseInGetTestCaseById() {
        // Given
        when(testCaseRepository.findByIdNotDeleted(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> testCaseService.getTestCaseById(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test case not found: 1");
    }
}