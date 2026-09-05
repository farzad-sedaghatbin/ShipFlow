import { describe, it, expect, vi, beforeEach } from 'vitest';
import { pitchService } from '../services/pitchService';

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

describe('pitchService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all pitches', async () => {
      const mockPitches = [
        { id: 1, title: 'Pitch 1' },
        { id: 2, title: 'Pitch 2' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockPitches });

      const result = await pitchService.getAll();

      expect(mockedApi.get).toHaveBeenCalledWith('/pitches');
      expect(result.data).toEqual(mockPitches);
    });
  });

  describe('getById', () => {
    it('should fetch a pitch by id', async () => {
      const mockPitch = { id: 1, title: 'Pitch 1' };
      mockedApi.get.mockResolvedValueOnce({ data: mockPitch });

      const result = await pitchService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/pitches/1');
      expect(result.data).toEqual(mockPitch);
    });
  });

  describe('getByCycleId', () => {
    it('should fetch pitches by cycle id', async () => {
      const mockPitches = [{ id: 1, title: 'Pitch 1' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockPitches });

      const result = await pitchService.getByCycleId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/pitches/cycle/1');
      expect(result.data).toEqual(mockPitches);
    });
  });

  describe('create', () => {
    it('should create a new pitch', async () => {
      const newPitch = { title: 'New Pitch', description: 'Description', cycleId: 1 };
      const createdPitch = { id: 1, ...newPitch };
      mockedApi.post.mockResolvedValueOnce({ data: createdPitch });

      const result = await pitchService.create(newPitch as any);

      expect(mockedApi.post).toHaveBeenCalledWith('/pitches', newPitch);
      expect(result.data).toEqual(createdPitch);
    });
  });

  describe('update', () => {
    it('should update a pitch', async () => {
      const updatedPitch = { title: 'Updated Pitch', description: 'Updated', cycleId: 1 };
      const returnedPitch = { id: 1, ...updatedPitch };
      mockedApi.put.mockResolvedValueOnce({ data: returnedPitch });

      const result = await pitchService.update(1, updatedPitch as any);

      expect(mockedApi.put).toHaveBeenCalledWith('/pitches/1', updatedPitch);
      expect(result.data).toEqual(returnedPitch);
    });

    it('threads expectedVersion through for the optimistic-lock check (v1.13.0 S64)', async () => {
      const updatedPitch = { title: 'Updated Pitch', description: 'Updated', cycleId: 1, expectedVersion: 3 };
      const returnedPitch = { id: 1, ...updatedPitch, version: 4 };
      mockedApi.put.mockResolvedValueOnce({ data: returnedPitch });

      const result = await pitchService.update(1, updatedPitch as any);

      expect(mockedApi.put).toHaveBeenCalledWith('/pitches/1', expect.objectContaining({ expectedVersion: 3 }));
      expect(result.data).toEqual(returnedPitch);
    });
  });

  describe('delete', () => {
    it('should delete a pitch', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await pitchService.delete(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/pitches/1');
    });
  });
});
