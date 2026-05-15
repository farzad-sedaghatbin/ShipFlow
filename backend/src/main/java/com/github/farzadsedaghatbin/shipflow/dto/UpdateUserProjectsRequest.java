package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProjectsRequest {

  @NotNull
  @Valid
  private List<ProjectAssignment> assignments;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectAssignment {

    @NotNull
    private Long projectId;

    @NotNull
    private ProjectRole projectRole;
  }
}
