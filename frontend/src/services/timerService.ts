import axios from 'axios';

const API_BASE_URL = '/api'; // Use relative API path

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
    const response = await axios.post(`${API_BASE_URL}/api/timers/start`, request);
    return response.data;
  },

  async stopTimer(): Promise<StopTimerResponse> {
    const response = await axios.post(`${API_BASE_URL}/api/timers/stop`);
    return response.data;
  },

  async getActiveTimer(): Promise<WorkLogTimer | null> {
    const response = await axios.get(`${API_BASE_URL}/api/timers/active`);
    return response.data;
  },

  async cancelTimer(): Promise<void> {
    await axios.delete(`${API_BASE_URL}/api/timers/cancel`);
  }
};

export default timerService;
