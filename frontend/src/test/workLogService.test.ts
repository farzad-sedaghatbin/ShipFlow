import { describe, it, expect, vi, beforeEach } from 'vitest';
import { workLogService } from '../services/workLogService';

// Mock the api module
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '../services/api';
const mockedApi = vi.mocked(api, true);

describe('workLogService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all work logs', async () => {
      const mockLogs = [
        { id: 1, note: 'Work 1' },
        { id: 2, note: 'Work 2' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getAll();

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('getById', () => {
    it('should fetch a work log by id', async () => {
      const mockLog = { id: 1, note: 'Work 1' };
      mockedApi.get.mockResolvedValueOnce({ data: mockLog });

      const result = await workLogService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/1');
      expect(result.data).toEqual(mockLog);
    });
  });

  describe('getByPitchId', () => {
    it('should fetch work logs by pitch id', async () => {
      const mockLogs = [{ id: 1, note: 'Work 1' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getByPitchId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/pitch/1');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('getByPersonId', () => {
    it('should fetch work logs by person id', async () => {
      const mockLogs = [{ id: 1, note: 'Work 1' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getByPersonId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/person/1');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('create', () => {
    it('should create a new work log', async () => {
      const newLog = { pitchId: 1, personId: 1, date: '2024-01-15', hoursSpent: 8 };
      const createdLog = { id: 1, ...newLog };
      mockedApi.post.mockResolvedValueOnce({ data: createdLog });

      const result = await workLogService.create(newLog as any);

      expect(mockedApi.post).toHaveBeenCalledWith('/worklogs', newLog);
      expect(result.data).toEqual(createdLog);
    });
  });

  describe('update', () => {
    it('should update a work log', async () => {
      const updatedLog = { pitchId: 1, personId: 1, date: '2024-01-15', hoursSpent: 6 };
      const returnedLog = { id: 1, ...updatedLog };
      mockedApi.put.mockResolvedValueOnce({ data: returnedLog });

      const result = await workLogService.update(1, updatedLog as any);

      expect(mockedApi.put).toHaveBeenCalledWith('/worklogs/1', updatedLog);
      expect(result.data).toEqual(returnedLog);
    });
  });

  describe('delete', () => {
    it('should delete a work log', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await workLogService.delete(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/worklogs/1');
    });
  });

  // ========== My Work Logs Tests ==========

  describe('getMy', () => {
    it('should fetch current user work logs', async () => {
      const mockLogs = [{ id: 1, note: 'My Work' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getMy();

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/my');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('getMyByCycle', () => {
    it('should fetch current user work logs by cycle', async () => {
      const mockLogs = [{ id: 1, note: 'My Work' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getMyByCycle(5);

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/my/cycle/5');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('getMyByDate', () => {
    it('should fetch current user work logs by date', async () => {
      const mockLogs = [{ id: 1, note: 'My Work' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockLogs });

      const result = await workLogService.getMyByDate('2024-01-15');

      expect(mockedApi.get).toHaveBeenCalledWith('/worklogs/my/date/2024-01-15');
      expect(result.data).toEqual(mockLogs);
    });
  });

  describe('createMy', () => {
    it('should create a work log for current user', async () => {
      const newLog = { pitchId: 1, date: '2024-01-15', hoursSpent: 4, note: 'My work' };
      const createdLog = { id: 1, personId: 5, personName: 'Test User', ...newLog };
      mockedApi.post.mockResolvedValueOnce({ data: createdLog });

      const result = await workLogService.createMy(newLog);

      expect(mockedApi.post).toHaveBeenCalledWith('/worklogs/my', newLog);
      expect(result.data).toEqual(createdLog);
    });
  });

  describe('updateMy', () => {
    it('should update current user own work log', async () => {
      const updatedLog = { pitchId: 1, date: '2024-01-15', hoursSpent: 6, note: 'Updated' };
      const returnedLog = { id: 1, personId: 5, ...updatedLog };
      mockedApi.put.mockResolvedValueOnce({ data: returnedLog });

      const result = await workLogService.updateMy(1, updatedLog);

      expect(mockedApi.put).toHaveBeenCalledWith('/worklogs/my/1', updatedLog);
      expect(result.data).toEqual(returnedLog);
    });
  });

  describe('deleteMy', () => {
    it('should delete current user own work log', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await workLogService.deleteMy(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/worklogs/my/1');
    });
  });
});
