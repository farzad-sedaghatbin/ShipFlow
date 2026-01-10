import api from './api';

export interface WorkLogTimer {
  id: number;
  personId: number;
  personName: string;
  pitchId?: number;
  pitchTitle?: string;
  taskId?: number;
  taskTitle?: string;
  startTime: string;
  note?: string;
  elapsedSeconds: number;
}

export interface StartTimerRequest {
  pitchId?: number;
  taskId?: number;
  note?: string;
}

export interface StopTimerResponse {
  workLogId: number;
  hoursSpent: number;
  message: string;
}

const timerService = {
  async startTimer(request: StartTimerRequest): Promise<WorkLogTimer> {
    const response = await api.post('/timers/start', request);
    return response.data;
  },

  async stopTimer(): Promise<StopTimerResponse> {
    const response = await api.post('/timers/stop');
    return response.data;
  },

  async getActiveTimer(): Promise<WorkLogTimer | null> {
    const response = await api.get('/timers/active');
    return response.data;
  },

  async cancelTimer(): Promise<void> {
    await api.delete('/timers/cancel');
  }
};

export default timerService;
