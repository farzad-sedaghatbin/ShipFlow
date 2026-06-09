import { describe, it, expect, vi, beforeEach } from 'vitest';
import { teamService } from '../services/teamService';

// Mock the api module
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

import api from '../services/api';
const mockedApi = vi.mocked(api, true);

describe('teamService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all teams', async () => {
      const mockTeams = [
        { id: 1, name: 'Team 1' },
        { id: 2, name: 'Team 2' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockTeams });

      const result = await teamService.getAll();

      expect(mockedApi.get).toHaveBeenCalledWith('/teams');
      expect(result.data).toEqual(mockTeams);
    });
  });

  describe('getById', () => {
    it('should fetch a team by id', async () => {
      const mockTeam = { id: 1, name: 'Team 1' };
      mockedApi.get.mockResolvedValueOnce({ data: mockTeam });

      const result = await teamService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/teams/1');
      expect(result.data).toEqual(mockTeam);
    });
  });

  describe('getByCycleId', () => {
    it('should fetch teams by cycle id', async () => {
      const mockTeams = [{ id: 1, name: 'Team 1' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTeams });

      const result = await teamService.getByCycleId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/teams/cycle/1');
      expect(result.data).toEqual(mockTeams);
    });
  });

  describe('create', () => {
    it('should create a new team', async () => {
      const newTeam = { name: 'New Team', cycleId: 1 };
      const createdTeam = { id: 1, ...newTeam };
      mockedApi.post.mockResolvedValueOnce({ data: createdTeam });

      const result = await teamService.create(newTeam as any);

      expect(mockedApi.post).toHaveBeenCalledWith('/teams', newTeam);
      expect(result.data).toEqual(createdTeam);
    });
  });

  describe('update', () => {
    it('should update a team', async () => {
      const updatedTeam = { name: 'Updated Team', cycleId: 1 };
      const returnedTeam = { id: 1, ...updatedTeam };
      mockedApi.put.mockResolvedValueOnce({ data: returnedTeam });

      const result = await teamService.update(1, updatedTeam as any);

      expect(mockedApi.put).toHaveBeenCalledWith('/teams/1', updatedTeam);
      expect(result.data).toEqual(returnedTeam);
    });
  });

  describe('archive', () => {
    it('should archive a team', async () => {
      mockedApi.patch.mockResolvedValueOnce({ data: null });

      await teamService.archive(1);

      expect(mockedApi.patch).toHaveBeenCalledWith('/teams/1/archive');
    });
  });

  describe('unarchive', () => {
    it('should unarchive a team', async () => {
      mockedApi.patch.mockResolvedValueOnce({ data: null });

      await teamService.unarchive(1);

      expect(mockedApi.patch).toHaveBeenCalledWith('/teams/1/unarchive');
    });
  });
});
