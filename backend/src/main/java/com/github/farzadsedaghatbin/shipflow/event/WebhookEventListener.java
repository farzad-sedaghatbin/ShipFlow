package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.service.WebhookDeliveryService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for domain events and dispatches them to webhook subscribers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookEventListener {

  private final WebhookDeliveryService deliveryService;

  @TransactionalEventListener
  public void onTaskStatusChanged(TaskStatusChangedEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("taskId", event.getTaskId());
    data.put("pitchId", event.getPitchId());
    data.put("oldStatus", event.getOldStatus() != null ? event.getOldStatus().name() : null);
    data.put("newStatus", event.getNewStatus() != null ? event.getNewStatus().name() : null);
    data.put("isCompletionChange", event.isCompletionChange());

    deliveryService.dispatch("task.status_changed", data);
  }

  @TransactionalEventListener
  public void onTaskCompleted(TaskCompletedEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("taskId", event.getTaskId());
    data.put("pitchId", event.getPitchId());
    data.put("cycleId", event.getCycleId());
    data.put("taskTitle", event.getTaskTitle());

    deliveryService.dispatch("task.completed", data);
  }

  @TransactionalEventListener
  public void onPitchStatusChanged(PitchStatusChangedEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("pitchId", event.getPitchId());
    data.put("cycleId", event.getCycleId());
    data.put("pitchTitle", event.getPitchTitle());
    data.put("oldStatus", event.getOldStatus());
    data.put("newStatus", event.getNewStatus());
    data.put("isSignificantChange", event.isSignificantChange());

    deliveryService.dispatch("pitch.status_changed", data);
  }

  @TransactionalEventListener
  public void onCycleStatusChanged(CycleStatusChangedEvent event) {
    Map<String, Object> data = new HashMap<>();
    data.put("cycleId", event.getCycleId());
    data.put("cycleName", event.getCycleName());
    data.put("oldStatus", event.getOldStatus());
    data.put("newStatus", event.getNewStatus());

    deliveryService.dispatch("cycle.status_changed", data);
  }
}
