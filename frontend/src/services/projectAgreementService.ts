import api from './api';
import { ProjectAgreement, ProjectAgreementRequest } from '../types';

/**
 * Per-project agreement/contract-term entries — discrete, dated "we agreed X"
 * items that aren't Tasks, optionally linked to the Meeting they originated
 * from. Backed by ProjectAgreementController (mirrors TagController's
 * permission pattern: PROJECT/READ for list, PROJECT/UPDATE for writes).
 */
export const projectAgreementService = {
  list: (projectId: number) =>
    api.get<ProjectAgreement[]>(`/projects/${projectId}/agreements`),

  create: (projectId: number, request: ProjectAgreementRequest) =>
    api.post<ProjectAgreement>(`/projects/${projectId}/agreements`, request),

  update: (projectId: number, agreementId: number, request: ProjectAgreementRequest) =>
    api.put<ProjectAgreement>(`/projects/${projectId}/agreements/${agreementId}`, request),

  remove: (projectId: number, agreementId: number) =>
    api.delete(`/projects/${projectId}/agreements/${agreementId}`),
};
