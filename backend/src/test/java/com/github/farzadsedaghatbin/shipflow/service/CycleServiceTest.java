package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CycleDTO;
import com.github.farzadsedaghatbin.shipflow.dto.CreateCycleRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CycleServiceTest {

    @Mock
    private CycleRepository cycleRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private CycleService cycleService;

    private Cycle testCycle;
    private Project testProject;
    private CreateCycleRequest testRequest;

    @BeforeEach
    void setUp() {
        testProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .projectKey("TST")
                .isActive(true)
                .build();

        testCycle = Cycle.builder()
                .id(1L)
                .name("Test Cycle")
                .project(testProject)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(6))
                .phase(CyclePhase.BUILD)
                .isActive(true)
                .build();

        testRequest = new CreateCycleRequest();
        testRequest.setProjectId(1L);
        testRequest.setName("Test Cycle");
        testRequest.setStartDate(LocalDate.now());
        testRequest.setEndDate(LocalDate.now().plusWeeks(6));
        testRequest.setPhase(CyclePhase.BUILD);
    }

    @Test
    void getAllCycles_ShouldReturnAllCycles() {
        when(cycleRepository.findAllByOrderByStartDateDesc())
                .thenReturn(Arrays.asList(testCycle));

        List<CycleDTO> result = cycleService.getAllCycles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Cycle");
        verify(cycleRepository).findAllByOrderByStartDateDesc();
    }

    @Test
    void getCycleById_WhenExists_ShouldReturnCycle() {
        when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));

        CycleDTO result = cycleService.getCycleById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Cycle");
        assertThat(result.getProjectId()).isEqualTo(1L);
    }

    @Test
    void getCycleById_WhenNotExists_ShouldThrowException() {
        when(cycleRepository.findByIdWithProject(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cycleService.getCycleById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cycle not found");
    }

    @Test
    void createCycle_ShouldSaveCycle() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

        CycleDTO result = cycleService.createCycle(testRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Cycle");
        verify(cycleRepository).save(any(Cycle.class));
    }

    @Test
    void updateCycle_WhenExists_ShouldUpdateCycle() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
        when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

        testRequest.setName("Updated Cycle");
        CycleDTO result = cycleService.updateCycle(1L, testRequest);

        assertThat(result).isNotNull();
        verify(cycleRepository).save(any(Cycle.class));
    }

    @Test
    void deleteCycle_ShouldCallRepository() {
        when(cycleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(cycleRepository).deleteById(1L);

        cycleService.deleteCycle(1L);

        verify(cycleRepository).deleteById(1L);
    }

    @Test
    void getActiveCycles_ShouldReturnActiveCycles() {
        when(cycleRepository.findByIsActiveTrue())
                .thenReturn(Arrays.asList(testCycle));

        List<CycleDTO> result = cycleService.getActiveCycles();

        assertThat(result).hasSize(1);
        verify(cycleRepository).findByIsActiveTrue();
    }

    @Test
    void updatePhase_ShouldUpdateCyclePhase() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
        when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

        CycleDTO result = cycleService.updatePhase(1L, CyclePhase.COOLDOWN);

        assertThat(result).isNotNull();
        verify(cycleRepository).save(any(Cycle.class));
    }

    @Test
    void toggleActive_ShouldToggleCycleActiveStatus() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
        when(cycleRepository.save(any(Cycle.class))).thenReturn(testCycle);

        CycleDTO result = cycleService.toggleActive(1L);

        assertThat(result).isNotNull();
        verify(cycleRepository).save(any(Cycle.class));
    }

    @Test
    void getCyclesByProject_ShouldReturnCyclesForProject() {
        when(cycleRepository.findByProjectIdOrderByStartDateDesc(1L))
                .thenReturn(Arrays.asList(testCycle));

        List<CycleDTO> result = cycleService.getCyclesByProject(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        verify(cycleRepository).findByProjectIdOrderByStartDateDesc(1L);
    }
}
