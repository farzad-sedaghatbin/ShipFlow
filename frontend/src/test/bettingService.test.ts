import { describe, it, expect, vi, beforeEach } from 'vitest';
import { bettingService } from '../services/bettingService';

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

describe('bettingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getBettingTable', () => {
    it('should fetch betting table for a cycle', async () => {
      const mockBettingTable = {
        cycleId: 1,
        cycleName: 'Q1 2024',
        shapedPitches: [{ id: 1, title: 'Feature A', status: 'SHAPED' }],
        teamTracks: [{ teamId: 1, teamName: 'Alpha', slots: [] }],
      };
      mockedApi.get.mockResolvedValueOnce({ data: mockBettingTable });

      const result = await bettingService.getBettingTable(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/betting/cycle/1');
      expect(result.data).toEqual(mockBettingTable);
    });
  });

  describe('getShapedPitches', () => {
    it('should fetch shaped pitches without project filter', async () => {
      const mockPitches = [
        { id: 1, title: 'Pitch 1', status: 'SHAPED' },
        { id: 2, title: 'Pitch 2', status: 'SHAPED' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockPitches });

      const result = await bettingService.getShapedPitches();

      expect(mockedApi.get).toHaveBeenCalledWith('/betting/shaped-pitches');
      expect(result.data).toEqual(mockPitches);
    });

    it('should fetch shaped pitches with project filter', async () => {
      const mockPitches = [{ id: 1, title: 'Pitch 1', status: 'SHAPED' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockPitches });

      const result = await bettingService.getShapedPitches(5);

      expect(mockedApi.get).toHaveBeenCalledWith('/betting/shaped-pitches?projectId=5');
      expect(result.data).toEqual(mockPitches);
    });
  });

  describe('createSlot', () => {
    it('should create a new betting slot', async () => {
      const newSlot = {
        cycleId: 1,
        teamId: 1,
        position: 0,
        startDate: '2024-01-01',
        endDate: '2024-02-15',
      };
      const createdSlot = { id: 1, ...newSlot, teamName: 'Alpha' };
      mockedApi.post.mockResolvedValueOnce({ data: createdSlot });

      const result = await bettingService.createSlot(newSlot);

      expect(mockedApi.post).toHaveBeenCalledWith('/betting/slots', newSlot);
      expect(result.data).toEqual(createdSlot);
    });
  });

  describe('generateSlots', () => {
    it('should generate slots for a cycle', async () => {
      const mockSlots = [
        { id: 1, teamName: 'Alpha', position: 0 },
        { id: 2, teamName: 'Beta', position: 0 },
      ];
      mockedApi.post.mockResolvedValueOnce({ data: mockSlots });

      const result = await bettingService.generateSlots(1);

      expect(mockedApi.post).toHaveBeenCalledWith('/betting/cycle/1/generate-slots');
      expect(result.data).toEqual(mockSlots);
    });
  });

  describe('assignPitchToSlot', () => {
    it('should assign a pitch to a slot', async () => {
      const mockSlot = { id: 1, pitchId: 5, pitchTitle: 'User Auth' };
      mockedApi.patch.mockResolvedValueOnce({ data: mockSlot });

      const result = await bettingService.assignPitchToSlot(1, 5);

      expect(mockedApi.patch).toHaveBeenCalledWith('/betting/slots/1/assign/5');
      expect(result.data).toEqual(mockSlot);
    });
  });

  describe('removePitchFromSlot', () => {
    it('should remove pitch from slot', async () => {
      const mockSlot = { id: 1, pitchId: null };
      mockedApi.patch.mockResolvedValueOnce({ data: mockSlot });

      const result = await bettingService.removePitchFromSlot(1);

      expect(mockedApi.patch).toHaveBeenCalledWith('/betting/slots/1/unassign');
      expect(result.data.pitchId).toBeNull();
    });
  });

  describe('updateSlotDates', () => {
    it('should update slot dates', async () => {
      const mockSlot = { id: 1, startDate: '2024-02-01', endDate: '2024-03-15' };
      mockedApi.patch.mockResolvedValueOnce({ data: mockSlot });

      const result = await bettingService.updateSlotDates(1, '2024-02-01', '2024-03-15');

      expect(mockedApi.patch).toHaveBeenCalledWith(
        '/betting/slots/1/dates?startDate=2024-02-01&endDate=2024-03-15'
      );
      expect(result.data).toEqual(mockSlot);
    });
  });

  describe('deleteSlot', () => {
    it('should delete a slot', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: undefined });

      await bettingService.deleteSlot(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/betting/slots/1');
    });
  });

  describe('canPitchFitInSlot', () => {
    it('should check if pitch fits in slot', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: true });

      const result = await bettingService.canPitchFitInSlot(1, 5);

      expect(mockedApi.get).toHaveBeenCalledWith('/betting/slots/1/can-fit/5');
      expect(result.data).toBe(true);
    });

    it('should return false when pitch does not fit', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: false });

      const result = await bettingService.canPitchFitInSlot(1, 10);

      expect(result.data).toBe(false);
    });
  });
});
