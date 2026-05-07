import api from './api';

export interface SavedViewFilters {
  statusFilter?: string[];
  priorityFilter?: string[];
  assigneeFilter?: number[];
  activeCategory?: string;
  dependencyFilter?: string;
  sortBy?: string;
  sortOrder?: string;
  searchQuery?: string;
}

export interface SavedView {
  id: number;
  userId: number;
  projectId: number;
  name: string;
  filters: SavedViewFilters;
  isDefault: boolean;
  createdAt: string;
}

interface SavedViewRaw {
  id: number;
  userId: number;
  projectId: number;
  name: string;
  filters: string;
  isDefault: boolean;
  createdAt: string;
}

function parseView(raw: SavedViewRaw): SavedView {
  let filters: SavedViewFilters = {};
  try {
    filters = JSON.parse(raw.filters) as SavedViewFilters;
  } catch {
    filters = {};
  }
  return { ...raw, filters };
}

export const savedViewService = {
  getSavedViews: (projectId: number) =>
    api
      .get<SavedViewRaw[]>(`/projects/${projectId}/saved-views`)
      .then((r) => ({ ...r, data: r.data.map(parseView) })),

  createSavedView: (projectId: number, name: string, filters: SavedViewFilters) =>
    api
      .post<SavedViewRaw>(`/projects/${projectId}/saved-views`, {
        name,
        filters: JSON.stringify(filters),
      })
      .then((r) => ({ ...r, data: parseView(r.data) })),

  updateSavedView: (
    projectId: number,
    id: number,
    data: { name?: string; filters?: SavedViewFilters }
  ) => {
    const payload: Record<string, unknown> = {};
    if (data.name !== undefined) payload.name = data.name;
    if (data.filters !== undefined) payload.filters = JSON.stringify(data.filters);
    return api
      .put<SavedViewRaw>(`/projects/${projectId}/saved-views/${id}`, payload)
      .then((r) => ({ ...r, data: parseView(r.data) }));
  },

  deleteSavedView: (projectId: number, id: number) =>
    api.delete<void>(`/projects/${projectId}/saved-views/${id}`),

  setDefault: (projectId: number, id: number) =>
    api
      .patch<SavedViewRaw>(`/projects/${projectId}/saved-views/${id}/default`)
      .then((r) => ({ ...r, data: parseView(r.data) })),
};
