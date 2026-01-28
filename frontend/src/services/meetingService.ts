import api from './api';
import { Meeting, CreateMeetingRequest, MeetingType } from '../types';

export interface MeetingFilterParams {
  cycleId?: number;
  projectId?: number;
  pitchId?: number;
  types?: MeetingType[];
  startDate?: string;
  endDate?: string;
  dorReady?: boolean;
  dodReady?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const meetingService = {
  getAll: () => api.get<Meeting[]>('/meetings'),
  getPaginated: (params: MeetingFilterParams = {}) => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortOrder) queryParams.append('sortOrder', params.sortOrder);
    return api.get<PageResponse<Meeting>>(`/meetings/paginated?${queryParams.toString()}`);
  },
  getWithFilters: (params: MeetingFilterParams = {}) => {
    const queryParams = new URLSearchParams();
    if (params.cycleId !== undefined) queryParams.append('cycleId', params.cycleId.toString());
    if (params.projectId !== undefined) queryParams.append('projectId', params.projectId.toString());
    if (params.pitchId !== undefined) queryParams.append('pitchId', params.pitchId.toString());
    if (params.types && params.types.length > 0) {
      params.types.forEach(type => queryParams.append('types', type));
    }
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    if (params.dorReady !== undefined) queryParams.append('dorReady', params.dorReady.toString());
    if (params.dodReady !== undefined) queryParams.append('dodReady', params.dodReady.toString());
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.sortBy) queryParams.append('sortBy', params.sortBy);
    if (params.sortOrder) queryParams.append('sortOrder', params.sortOrder);
    return api.get<PageResponse<Meeting>>(`/meetings/filter?${queryParams.toString()}`);
  },
  getByPitchId: (pitchId: number) => api.get<Meeting[]>(`/meetings/pitch/${pitchId}`),
  getByType: (type: MeetingType) => api.get<Meeting[]>(`/meetings/type/${type}`),
  getById: (id: number) => api.get<Meeting>(`/meetings/${id}`),
  create: (data: CreateMeetingRequest) => api.post<Meeting>('/meetings', data),
  update: (id: number, data: CreateMeetingRequest) => api.put<Meeting>(`/meetings/${id}`, data),
  delete: (id: number) => api.delete(`/meetings/${id}`),
};
