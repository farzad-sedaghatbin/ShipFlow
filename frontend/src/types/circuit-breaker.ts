export interface CircuitBreaker {
  pitchId: number;
  pitchTitle: string;
  appetiteDays: number;
  daysSpent: number;
  hoursSpent: number;
  utilizationPercentage: number;
  overflowPercentage: number;
  isOverflowing: boolean;
  isCircuitBreakerTriggered: boolean;
  circuitBreakerReason?: string;
  circuitBreakerDate?: string;
  status: string;
}

export interface TriggerCircuitBreakerRequest {
  reason: string;
}

export interface KillPitchRequest {
  reason: string;
}
