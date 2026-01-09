import api from './api';
import { Person, CreatePersonRequest, TeamAssignment, CreateTeamAssignmentRequest } from '../types';

export const personService = {
  // Person CRUD
  getAll: async (active?: boolean, search?: string): Promise<Person[]> => {
    const params = new URLSearchParams();
    if (active !== undefined) params.append('active', String(active));
    if (search) params.append('search', search);
    const queryString = params.toString();
    const response = await api.get<Person[]>(`/persons${queryString ? `?${queryString}` : ''}`);
    return response.data;
  },

  getById: async (id: number): Promise<Person> => {
    const response = await api.get<Person>(`/persons/${id}`);
    return response.data;
  },

  getByTeam: async (teamId: number): Promise<Person[]> => {
    const response = await api.get<Person[]>(`/persons/team/${teamId}`);
    return response.data;
  },

  getByCycle: async (cycleId: number): Promise<Person[]> => {
    const response = await api.get<Person[]>(`/persons/cycle/${cycleId}`);
    return response.data;
  },

  create: async (request: CreatePersonRequest): Promise<Person> => {
    const response = await api.post<Person>('/persons', request);
    return response.data;
  },

  update: async (id: number, request: CreatePersonRequest): Promise<Person> => {
    const response = await api.put<Person>(`/persons/${id}`, request);
    return response.data;
  },

  deactivate: async (id: number): Promise<Person> => {
    const response = await api.post<Person>(`/persons/${id}/deactivate`);
    return response.data;
  },

  activate: async (id: number): Promise<Person> => {
    const response = await api.post<Person>(`/persons/${id}/activate`);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/persons/${id}`);
  },

  // Team Assignments
  assignToTeam: async (request: CreateTeamAssignmentRequest): Promise<TeamAssignment> => {
    const response = await api.post<TeamAssignment>('/persons/assignments', request);
    return response.data;
  },

  getPersonAssignments: async (personId: number): Promise<TeamAssignment[]> => {
    const response = await api.get<TeamAssignment[]>(`/persons/${personId}/assignments`);
    return response.data;
  },

  getTeamAssignments: async (teamId: number): Promise<TeamAssignment[]> => {
    const response = await api.get<TeamAssignment[]>(`/persons/teams/${teamId}/assignments`);
    return response.data;
  },

  updateAssignment: async (assignmentId: number, request: CreateTeamAssignmentRequest): Promise<TeamAssignment> => {
    const response = await api.put<TeamAssignment>(`/persons/assignments/${assignmentId}`, request);
    return response.data;
  },

  endAssignment: async (assignmentId: number): Promise<TeamAssignment> => {
    const response = await api.post<TeamAssignment>(`/persons/assignments/${assignmentId}/end`);
    return response.data;
  },
};
