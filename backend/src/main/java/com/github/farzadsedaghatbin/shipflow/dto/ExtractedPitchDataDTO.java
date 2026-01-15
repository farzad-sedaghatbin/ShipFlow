package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for pitch shaping data extracted from documents.
 * Contains Shape Up methodology elements extracted via AI analysis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedPitchDataDTO {
    private String title;
    private String problemStatement;
    private String solution;
    private String rabbitHoles;
    private String risks;
    private String noGos;
    private Integer appetiteDays;
    private String wireframeLinks;
    private boolean extractionSuccessful;
    private String errorMessage;
    private Long documentId; // ID of saved document (if saveDocument was true)
}
