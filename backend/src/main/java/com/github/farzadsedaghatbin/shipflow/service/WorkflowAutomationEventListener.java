package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import com.github.farzadsedaghatbin.shipflow.event.CycleStatusChangedEvent;
import com.github.farzadsedaghatbin.shipflow.event.PitchStatusChangedEvent;
import com.github.farzadsedaghatbin.shipflow.event.TaskStatusChangedEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges existing domain events to the workflow automation engine.
 * Each listener method translates an event into trigger context and calls the engine.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowAutomationEventListener {

  private final WorkflowAutomationEngine engine;

  @EventListener
  public void onTaskStatusChanged(TaskStatusChangedEvent event) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("taskId", event.getTaskId());
    ctx.put("pitchId", event.getPitchId());
    ctx.put("oldStatus", event.getOldStatus() != null ? event.getOldStatus().name() : "");
    ctx.put("newStatus", event.getNewStatus() != null ? event.getNewStatus().name() : "");

    engine.fire(TriggerType.TASK_STATUS_CHANGED, null, ctx);

    if (event.isCompletionChange()) {
      engine.fire(TriggerType.TASK_COMPLETED, null, ctx);
    }
  }

  @EventListener
  public void onPitchStatusChanged(PitchStatusChangedEvent event) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("pitchId", event.getPitchId());
    ctx.put("pitchTitle", event.getPitchTitle());
    ctx.put("oldStatus", event.getOldStatus());
    ctx.put("newStatus", event.getNewStatus());
    engine.fire(TriggerType.PITCH_STATUS_CHANGED, null, ctx);
  }

  @EventListener
  public void onCycleStatusChanged(CycleStatusChangedEvent event) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("cycleId", event.getCycleId());
    ctx.put("cycleName", event.getCycleName());
    ctx.put("oldStatus", event.getOldStatus());
    ctx.put("newStatus", event.getNewStatus());

    engine.fire(TriggerType.CYCLE_STATUS_CHANGED, null, ctx);

    if ("IN_PROGRESS".equals(event.getNewStatus())) {
      engine.fire(TriggerType.CYCLE_STARTED, null, ctx);
    }
    if ("COMPLETED".equals(event.getNewStatus()) || "CANCELLED".equals(event.getNewStatus())) {
      engine.fire(TriggerType.CYCLE_ENDED, null, ctx);
    }
  }
}
