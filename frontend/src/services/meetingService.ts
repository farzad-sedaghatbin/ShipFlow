import api from './api';
import { Meeting, CreateMeetingRequest, MeetingType } from '../types';

export const meetingService = {
  getAll: () => api.get<Meeting[]>('/meetings'),
  getByPitchId: (pitchId: number) => api.get<Meeting[]>(`/meetings/pitch/${pitchId}`),
  getByType: (type: MeetingType) => api.get<Meeting[]>(`/meetings/type/${type}`),
  getById: (id: number) => api.get<Meeting>(`/meetings/${id}`),
  create: (data: CreateMeetingRequest) => api.post<Meeting>('/meetings', data),
  update: (id: number, data: CreateMeetingRequest) => api.put<Meeting>(`/meetings/${id}`, data),
  delete: (id: number) => api.delete(`/meetings/${id}`),
};
