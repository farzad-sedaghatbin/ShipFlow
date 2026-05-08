import api from './api';
import { EpicDependencies, EpicDependency, DependencyType } from '../types';

export async function getEpicDependencies(epicId: number): Promise<EpicDependencies> {
  const res = await api.get<EpicDependencies>(`/epics/${epicId}/dependencies`);
  return res.data;
}

export async function addEpicDependency(
  epicId: number,
  targetEpicId: number,
  dependencyType: DependencyType = 'BLOCKS'
): Promise<EpicDependency> {
  const res = await api.post<EpicDependency>(`/epics/${epicId}/dependencies`, {
    targetEpicId,
    dependencyType,
  });
  return res.data;
}

export async function removeEpicDependency(epicId: number, dependencyId: number): Promise<void> {
  await api.delete(`/epics/${epicId}/dependencies/${dependencyId}`);
}
