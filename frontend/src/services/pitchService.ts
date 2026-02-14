import api from './api';
import { Pitch, CreatePitchRequest, CreateIdeaRequest, PitchStatus, Page, EntityHistory } from '../types';

export const pitchService = {
  getAll: () => api.get<Pitch[]>('/pitches'),
  getMyPitches: () => api.get<Pitch[]>('/pitches/my-pitches'),
  getByCycleId: (cycleId: number) => api.get<Pitch[]>(`/pitches/cycle/${cycleId}`),
  getByTeamId: (teamId: number) => api.get<Pitch[]>(`/pitches/team/${teamId}`),
  getByEpicId: (epicId: number) => api.get<Pitch[]>(`/pitches/epic/${epicId}`),
  getByReleaseId: (releaseId: number) => api.get<Pitch[]>(`/pitches/release/${releaseId}`),
  getById: (id: number) => api.get<Pitch>(`/pitches/${id}`),
  create: (data: CreatePitchRequest) => api.post<Pitch>('/pitches', data),
  update: (id: number, data: CreatePitchRequest) => api.put<Pitch>(`/pitches/${id}`, data),
  updateStatus: (id: number, status: PitchStatus) => api.patch<Pitch>(`/pitches/${id}/status?status=${status}`),
  assignTeam: (id: number, teamId: number) => api.patch<Pitch>(`/pitches/${id}/assign-team/${teamId}`),
  linkToEpic: (id: number, epicId: number) => api.patch<Pitch>(`/pitches/${id}/epic/${epicId}`),
  unlinkFromEpic: (id: number) => api.patch<Pitch>(`/pitches/${id}/unlink-epic`),
  setTargetRelease: (id: number, releaseId: number) => api.patch<Pitch>(`/pitches/${id}/release/${releaseId}`),
  clearTargetRelease: (id: number) => api.patch<Pitch>(`/pitches/${id}/unlink-release`),
  delete: (id: number) => api.delete(`/pitches/${id}`),
  
  // Pitch History (Audit Trail)
  getHistory: (pitchId: number, page?: number, size?: number) =>
    api.get<Page<EntityHistory>>(`/pitches/${pitchId}/history`, {
      params: {
        page: page ?? 0,
        size: size ?? 20,
      },
    }),

  // ===== Shape Up Workflow: Ideas Pool =====
  /** Get all ideas (raw concepts not yet being shaped) */
  getIdeas: () => api.get<Pitch[]>('/pitches/ideas'),
  /** Get ideas by project ID */
  getIdeasByProjectId: (projectId: number) => api.get<Pitch[]>(`/pitches/project/${projectId}/ideas`),
  /** Get ideas by epic ID */
  getIdeasByEpicId: (epicId: number) => api.get<Pitch[]>(`/pitches/epic/${epicId}/ideas`),
  /** Create a lightweight idea (just title + optional description) */
  createIdea: (data: CreateIdeaRequest) => api.post<Pitch>('/pitches/ideas', data),

  // ===== Shape Up Workflow: Shaping Pool =====
  /** Get all drafts (pitches being shaped) */
  getDrafts: () => api.get<Pitch[]>('/pitches/drafts'),
  /** Get drafts by project ID */
  getDraftsByProjectId: (projectId: number) => api.get<Pitch[]>(`/pitches/project/${projectId}/drafts`),

  // ===== Shape Up Workflow: Betting Table =====
  /** Get shaped pitches ready for betting (cycle assignment) */
  getBettingCandidates: () => api.get<Pitch[]>('/pitches/betting-candidates'),
  /** Get betting candidates by project ID */
  getBettingCandidatesByProjectId: (projectId: number) => api.get<Pitch[]>(`/pitches/project/${projectId}/betting-candidates`),
  /** Get unassigned pitches (IDEA, DRAFT, SHAPED) by epic ID */
  getUnassignedByEpicId: (epicId: number) => api.get<Pitch[]>(`/pitches/epic/${epicId}/unassigned`),

  // ===== Shape Up Workflow: Status Transitions =====
  /** Start shaping an idea (IDEA → DRAFT) */
  startShaping: (id: number) => api.patch<Pitch>(`/pitches/${id}/start-shaping`),
  /** Mark draft as shaped and ready for betting (DRAFT → SHAPED) */
  markAsShaped: (id: number) => api.patch<Pitch>(`/pitches/${id}/mark-shaped`),
  /** Assign shaped pitch to cycle (betting decision) (SHAPED → PENDING) */
  assignToCycle: (id: number, cycleId: number) => api.patch<Pitch>(`/pitches/${id}/assign-cycle/${cycleId}`),
  /** Unassign pitch from cycle (PENDING → SHAPED) */
  unassignFromCycle: (id: number) => api.patch<Pitch>(`/pitches/${id}/unassign-cycle`),
};
