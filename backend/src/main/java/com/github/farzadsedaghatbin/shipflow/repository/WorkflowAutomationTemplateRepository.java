package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkflowAutomationTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowAutomationTemplateRepository
    extends JpaRepository<WorkflowAutomationTemplate, Long> {

  List<WorkflowAutomationTemplate> findAllByOrderBySortOrderAsc();

  List<WorkflowAutomationTemplate> findByCategoryOrderBySortOrderAsc(String category);
}
