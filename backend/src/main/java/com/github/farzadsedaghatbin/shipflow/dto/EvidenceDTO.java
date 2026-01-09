package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceDTO {
    private Long id;
    private Long pitchId;
    private String pitchTitle;
    private Long cycleId;
    private String cycleName;
    private Long projectId;
    private String projectName;
    private String projectKey;
    private Long personId;
    private String personName;
    private LocalDate date;
    private String description;
    private String fileUrl;
}
