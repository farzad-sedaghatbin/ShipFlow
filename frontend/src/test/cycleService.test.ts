import { describe, it, expect, vi, beforeEach } from 'vitest';
import { cycleService } from '../services/cycleService';

// Mock the api module
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

describe('cycleService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all cycles', async () => {
      const mockCycles = [
        { id: 1, name: 'Cycle 1' },
        { id: 2, name: 'Cycle 2' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockCycles });

      const result = await cycleService.getAll();

      expect(mockedApi.get).toHaveBeenCalledWith('/cycles');
      expect(result.data).toEqual(mockCycles);
    });
  });

  describe('getById', () => {
    it('should fetch a cycle by id', async () => {
      const mockCycle = { id: 1, name: 'Cycle 1' };
      mockedApi.get.mockResolvedValueOnce({ data: mockCycle });

      const result = await cycleService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/cycles/1');
      expect(result.data).toEqual(mockCycle);
    });
  });

  describe('getActive', () => {
    it('should fetch active cycles', async () => {
      const mockCycles = [{ id: 1, name: 'Active Cycle' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockCycles });

      const result = await cycleService.getActive();

      expect(mockedApi.get).toHaveBeenCalledWith('/cycles/active');
      expect(result.data).toEqual(mockCycles);
    });
  });

  describe('create', () => {
    it('should create a new cycle', async () => {
      const newCycle = { name: 'New Cycle', startDate: '2024-01-01', endDate: '2024-06-30' };
      const createdCycle = { id: 1, ...newCycle };
      mockedApi.post.mockResolvedValueOnce({ data: createdCycle });

      const result = await cycleService.create(newCycle as any);

      expect(mockedApi.post).toHaveBeenCalledWith('/cycles', newCycle);
      expect(result.data).toEqual(createdCycle);
    });
  });

  describe('update', () => {
    it('should update a cycle', async () => {
      const updatedCycle = { name: 'Updated Cycle', startDate: '2024-01-01', endDate: '2024-06-30' };
      const returnedCycle = { id: 1, ...updatedCycle };
      mockedApi.put.mockResolvedValueOnce({ data: returnedCycle });

      const result = await cycleService.update(1, updatedCycle as any);

      expect(mockedApi.put).toHaveBeenCalledWith('/cycles/1', updatedCycle);
      expect(result.data).toEqual(returnedCycle);
    });
  });

  describe('delete', () => {
    it('should delete a cycle', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await cycleService.delete(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/cycles/1');
    });
  });
});
