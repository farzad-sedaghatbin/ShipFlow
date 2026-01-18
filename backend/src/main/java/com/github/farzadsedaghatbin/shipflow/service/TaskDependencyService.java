package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateTaskDependencyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.TaskDependencyDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.TaskDependency;
import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.TaskDependencyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing task dependencies.
 * Handles creating, deleting, and querying task dependencies with circular dependency detection.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TaskDependencyService {

    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskRepository taskRepository;
    private final LocalizationService localizationService;
    private final MessageService messageService;

    /**
     * Add a dependency between tasks.
     * Validates that the dependency doesn't create a circular reference.
     */
    public TaskDependencyDTO addDependency(Long sourceTaskId, CreateTaskDependencyRequest request) {
        // Validate both tasks exist
        Task sourceTask = taskRepository.findById(sourceTaskId)
                .orElseThrow(() -> new IllegalArgumentException("Source task not found with id: " + sourceTaskId));
        
        Task targetTask = taskRepository.findById(request.getTargetTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Target task not found with id: " + request.getTargetTaskId()));

        // Validate tasks are in the same cycle
        if (!sourceTask.getCycle().getId().equals(targetTask.getCycle().getId())) {
            throw new BadRequestException(localizationService.getMessage("dependency.same.cycle.required"));
        }

        // Prevent self-dependencies
        if (sourceTaskId.equals(request.getTargetTaskId())) {
            throw new BadRequestException(localizationService.getMessage("dependency.self.reference"));
        }

        // Check if dependency already exists
        if (taskDependencyRepository.findBySourceTaskIdAndTargetTaskId(sourceTaskId, request.getTargetTaskId()).isPresent()) {
            throw new BadRequestException(localizationService.getMessage("dependency.already.exists"));
        }

        // Check for circular dependencies
        if (wouldCreateCircularDependency(sourceTaskId, request.getTargetTaskId())) {
            throw new BadRequestException(localizationService.getMessage("dependency.circular.reference"));
        }

        // Create the dependency
        TaskDependency dependency = TaskDependency.builder()
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .dependencyType(request.getDependencyType() != null ? request.getDependencyType() : DependencyType.BLOCKS)
                .build();

        TaskDependency saved = taskDependencyRepository.save(dependency);
        return toDTO(saved);
    }

    /**
     * Remove a dependency between tasks.
     */
    public void removeDependency(Long dependencyId) {
        if (!taskDependencyRepository.existsById(dependencyId)) {
            throw new IllegalArgumentException(localizationService.getMessage("dependency.not.found", dependencyId));
        }
        taskDependencyRepository.deleteById(dependencyId);
    }

    /**
     * Get all dependencies for a task (both incoming and outgoing).
     */
    public Map<String, List<TaskDependencyDTO>> getDependenciesForTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException(messageService.getMessage("error.task.not.found", taskId));
        }

        List<TaskDependency> outgoing = taskDependencyRepository.findBySourceTaskId(taskId);
        List<TaskDependency> incoming = taskDependencyRepository.findByTargetTaskId(taskId);

        Map<String, List<TaskDependencyDTO>> result = new HashMap<>();
        result.put("blocking", outgoing.stream().map(this::toDTO).collect(Collectors.toList()));
        result.put("blockedBy", incoming.stream().map(this::toDTO).collect(Collectors.toList()));

        return result;
    }

    /**
     * Get all tasks that are blocking the given task.
     */
    public List<TaskDependencyDTO> getBlockingDependencies(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException(messageService.getMessage("error.task.not.found", taskId));
        }

        return taskDependencyRepository.findBlockingDependenciesByTaskId(taskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all tasks that are blocked by the given task.
     */
    public List<TaskDependencyDTO> getBlockedByDependencies(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new IllegalArgumentException(messageService.getMessage("error.task.not.found", taskId));
        }

        return taskDependencyRepository.findBlockedByDependenciesByTaskId(taskId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if adding a dependency would create a circular reference.
     * Uses depth-first search to detect cycles.
     */
    private boolean wouldCreateCircularDependency(Long sourceTaskId, Long targetTaskId) {
        // If target already depends on source (directly or indirectly), adding source->target would create a cycle
        return hasPathBetweenTasks(targetTaskId, sourceTaskId, new HashSet<>());
    }

    /**
     * Check if there's a dependency path from start to end task using DFS.
     */
    private boolean hasPathBetweenTasks(Long startTaskId, Long endTaskId, Set<Long> visited) {
        if (startTaskId.equals(endTaskId)) {
            return true;
        }

        if (visited.contains(startTaskId)) {
            return false;
        }

        visited.add(startTaskId);

        // Get all tasks that startTask depends on (outgoing dependencies)
        List<TaskDependency> dependencies = taskDependencyRepository.findBySourceTaskId(startTaskId);
        for (TaskDependency dep : dependencies) {
            if (hasPathBetweenTasks(dep.getTargetTask().getId(), endTaskId, visited)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convert entity to DTO.
     */
    private TaskDependencyDTO toDTO(TaskDependency dependency) {
        return TaskDependencyDTO.builder()
                .id(dependency.getId())
                .sourceTaskId(dependency.getSourceTask().getId())
                .sourceTaskTitle(dependency.getSourceTask().getTitle())
                .targetTaskId(dependency.getTargetTask().getId())
                .targetTaskTitle(dependency.getTargetTask().getTitle())
                .dependencyType(dependency.getDependencyType())
                .createdAt(dependency.getCreatedAt())
                .build();
    }
}
