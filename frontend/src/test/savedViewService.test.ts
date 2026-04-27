import { describe, it, expect, vi, beforeEach } from 'vitest';
import { savedViewService } from '../services/savedViewService';

vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '../services/api';
const mockedApi = vi.mocked(api, true);

const rawView = {
  id: 1,
  userId: 10,
  projectId: 5,
  name: 'My View',
  filters: JSON.stringify({ statusFilter: ['IN_PROGRESS'], priorityFilter: [], assigneeFilter: [], dependencyFilter: 'all', sortBy: 'createdAt', sortOrder: 'desc', searchQuery: '' }),
  isDefault: false,
  createdAt: '2026-04-05T10:00:00Z',
};

describe('savedViewService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getSavedViews', () => {
    it('fetches saved views and parses filters JSON', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [rawView] });

      const result = await savedViewService.getSavedViews(5);

      expect(mockedApi.get).toHaveBeenCalledWith('/projects/5/saved-views');
      expect(result.data).toHaveLength(1);
      expect(result.data[0].filters.statusFilter).toEqual(['IN_PROGRESS']);
    });

    it('returns empty array when API returns empty list', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });

      const result = await savedViewService.getSavedViews(5);

      expect(result.data).toHaveLength(0);
    });
  });

  describe('createSavedView', () => {
    it('posts serialised filters and returns parsed view', async () => {
      const filters = { statusFilter: ['DONE'], priorityFilter: ['HIGH'] };
      mockedApi.post.mockResolvedValueOnce({
        data: { ...rawView, name: 'Done + High', filters: JSON.stringify(filters) },
      });

      const result = await savedViewService.createSavedView(5, 'Done + High', filters);

      expect(mockedApi.post).toHaveBeenCalledWith(
        '/projects/5/saved-views',
        { name: 'Done + High', filters: JSON.stringify(filters) }
      );
      expect(result.data.name).toBe('Done + High');
      expect(result.data.filters.statusFilter).toEqual(['DONE']);
    });
  });

  describe('deleteSavedView', () => {
    it('sends DELETE request to correct URL', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: undefined });

      await savedViewService.deleteSavedView(5, 1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/projects/5/saved-views/1');
    });
  });

  describe('setDefault', () => {
    it('sends PATCH request and returns parsed view', async () => {
      mockedApi.patch.mockResolvedValueOnce({
        data: { ...rawView, isDefault: true },
      });

      const result = await savedViewService.setDefault(5, 1);

      expect(mockedApi.patch).toHaveBeenCalledWith('/projects/5/saved-views/1/default');
      expect(result.data.isDefault).toBe(true);
    });
  });

  describe('updateSavedView', () => {
    it('sends PUT with serialised filters', async () => {
      const filters = { sortBy: 'priority', sortOrder: 'asc' };
      mockedApi.put.mockResolvedValueOnce({
        data: { ...rawView, filters: JSON.stringify(filters) },
      });

      const result = await savedViewService.updateSavedView(5, 1, { filters });

      expect(mockedApi.put).toHaveBeenCalledWith('/projects/5/saved-views/1', {
        filters: JSON.stringify(filters),
      });
      expect(result.data.filters.sortBy).toBe('priority');
    });
  });
});
