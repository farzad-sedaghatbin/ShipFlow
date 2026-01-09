package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {

    private Long id;
    private String name;
    private String projectKey;
    private String description;
    private String color;
    private String logoUrl;
    private Long ownerId;
    private String ownerName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer cycleCount;
    private Integer activeCycleCount;
}
