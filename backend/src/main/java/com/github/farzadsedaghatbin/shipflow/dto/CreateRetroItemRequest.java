package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRetroItemRequest {
    
    @NotBlank(message = "Content is required")
    private String content;
    
    @NotNull(message = "Column type is required")
    private RetroColumnType columnType;
    
    @NotNull(message = "Retrospective ID is required")
    private Long retrospectiveId;
}
