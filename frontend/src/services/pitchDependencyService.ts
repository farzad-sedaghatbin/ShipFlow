import api from './api';
import { PitchDependencies, PitchDependency, DependencyType } from '../types';

export async function getPitchDependencies(pitchId: number): Promise<PitchDependencies> {
  const res = await api.get<PitchDependencies>(`/pitches/${pitchId}/dependencies`);
  return res.data;
}

export async function addPitchDependency(
  pitchId: number,
  targetPitchId: number,
  dependencyType: DependencyType = 'BLOCKS'
): Promise<PitchDependency> {
  const res = await api.post<PitchDependency>(`/pitches/${pitchId}/dependencies`, {
    targetPitchId,
    dependencyType,
  });
  return res.data;
}

export async function removePitchDependency(pitchId: number, dependencyId: number): Promise<void> {
  await api.delete(`/pitches/${pitchId}/dependencies/${dependencyId}`);
}
