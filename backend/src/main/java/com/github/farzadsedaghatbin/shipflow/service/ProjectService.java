package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateProjectRequest;
import com.github.farzadsedaghatbin.shipflow.dto.ProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;
    private final LocalizationService localizationService;

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAll() {
        return projectRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAllActive() {
        return projectRepository.findAllActiveOrderByName().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO findById(Long id) {
        Project project = projectRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return toDTO(project);
    }

    @Transactional(readOnly = true)
    public ProjectDTO findByKey(String projectKey) {
        Project project = projectRepository.findByProjectKey(projectKey.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with key: " + projectKey));
        return toDTO(project);
    }

    @Transactional
    public ProjectDTO create(CreateProjectRequest request) {
        if (projectRepository.existsByProjectKey(request.getProjectKey().toUpperCase())) {
            throw new IllegalArgumentException(localizationService.getMessage("project.key.exists", request.getProjectKey()));
        }

        Project project = Project.builder()
                .name(request.getName())
                .projectKey(request.getProjectKey().toUpperCase())
                .description(request.getDescription())
                .color(request.getColor())
                .logoUrl(request.getLogoUrl())
                .projectType(request.getProjectType() != null ? request.getProjectType() : ProjectType.SHAPE_UP)
                .isActive(true)
                .build();

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
            project.setOwner(owner);
        }

        project = projectRepository.save(project);
        
        // For Kanban projects, create a default "Continuous Flow" cycle
        if (project.getProjectType() == ProjectType.KANBAN) {
            createDefaultKanbanCycle(project);
        }
        
        return toDTO(project);
    }

    /**
     * Creates a default continuous cycle for Kanban projects.
     * This cycle has no end date concept (set to far future) and is always active.
     */
    private void createDefaultKanbanCycle(Project project) {
        Cycle defaultCycle = Cycle.builder()
                .name("Continuous Flow")
                .project(project)
                .startDate(LocalDate.now())
                .endDate(LocalDate.of(2099, 12, 31)) // Far future end date
                .phase(CyclePhase.BUILD)
                .isActive(true)
                .build();
        cycleRepository.save(defaultCycle);
    }

    @Transactional
    public ProjectDTO update(Long id, CreateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        // Check if key is being changed and if new key exists
        if (!project.getProjectKey().equals(request.getProjectKey().toUpperCase())) {
            if (projectRepository.existsByProjectKey(request.getProjectKey().toUpperCase())) {
                throw new IllegalArgumentException(localizationService.getMessage("project.key.exists", request.getProjectKey()));
            }
            project.setProjectKey(request.getProjectKey().toUpperCase());
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setColor(request.getColor());
        project.setLogoUrl(request.getLogoUrl());
        
        // Update project type if provided
        if (request.getProjectType() != null) {
            project.setProjectType(request.getProjectType());
        }

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getOwnerId()));
            project.setOwner(owner);
        } else {
            project.setOwner(null);
        }

        project = projectRepository.save(project);
        return toDTO(project);
    }

    @Transactional
    public ProjectDTO deactivate(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        project.setIsActive(false);
        project = projectRepository.save(project);
        return toDTO(project);
    }

    @Transactional
    public ProjectDTO activate(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        project.setIsActive(true);
        project = projectRepository.save(project);
        return toDTO(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        
        // Check if project has cycles
        long cycleCount = cycleRepository.countByProjectId(id);
        if (cycleCount > 0) {
            throw new IllegalArgumentException(localizationService.getMessage("project.cannot.delete.with.cycles"));
        }
        
        projectRepository.delete(project);
    }

    private ProjectDTO toDTO(Project project) {
        long cycleCount = cycleRepository.countByProjectId(project.getId());
        long activeCycleCount = cycleRepository.countActiveByProjectId(project.getId());

        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .projectKey(project.getProjectKey())
                .description(project.getDescription())
                .color(project.getColor())
                .logoUrl(project.getLogoUrl())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .ownerName(project.getOwner() != null ? project.getOwner().getUsername() : null)
                .isActive(project.getIsActive())
                .projectType(project.getProjectType())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .cycleCount((int) cycleCount)
                .activeCycleCount((int) activeCycleCount)
                .build();
    }
}
