import { describe, it, expect, vi, beforeEach } from 'vitest';
import { retroService } from '../services/retroService';

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

describe('retroService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getByProject', () => {
    it('should fetch retrospectives by project', async () => {
      const mockRetros = [
        { id: 1, title: 'Cycle 1 Retro', status: 'CLOSED' },
        { id: 2, title: 'Cycle 2 Retro', status: 'OPEN' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockRetros });

      const result = await retroService.getByProject(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/retros/project/1');
      expect(result.data).toEqual(mockRetros);
    });
  });

  describe('getByCycle', () => {
    it('should fetch retrospectives by cycle', async () => {
      const mockRetros = [{ id: 1, title: 'Cycle 1 Retro', status: 'CLOSED' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockRetros });

      const result = await retroService.getByCycle(5);

      expect(mockedApi.get).toHaveBeenCalledWith('/retros/cycle/5');
      expect(result.data).toEqual(mockRetros);
    });
  });

  describe('getById', () => {
    it('should fetch a retro by id', async () => {
      const mockRetro = { id: 1, title: 'Test Retro', status: 'DRAFT' };
      mockedApi.get.mockResolvedValueOnce({ data: mockRetro });

      const result = await retroService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/retros/1');
      expect(result.data).toEqual(mockRetro);
    });
  });

  describe('getWithItems', () => {
    it('should fetch a retro with all items', async () => {
      const mockRetro = {
        id: 1,
        title: 'Test Retro',
        items: [
          { id: 1, content: 'Item 1', columnType: 'WENT_WELL' },
          { id: 2, content: 'Item 2', columnType: 'TRY_NEXT' },
        ],
      };
      mockedApi.get.mockResolvedValueOnce({ data: mockRetro });

      const result = await retroService.getWithItems(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/retros/1/with-items');
      expect(result.data.items).toHaveLength(2);
    });
  });

  describe('create', () => {
    it('should create a new retro', async () => {
      const newRetro = {
        title: 'New Retro',
        cycleId: 1,
        projectId: 1,
      };
      const createdRetro = { id: 1, ...newRetro, status: 'DRAFT' };
      mockedApi.post.mockResolvedValueOnce({ data: createdRetro });

      const result = await retroService.create(newRetro);

      expect(mockedApi.post).toHaveBeenCalledWith('/retros', newRetro);
      expect(result.data).toEqual(createdRetro);
    });
  });

  describe('update', () => {
    it('should update a retro', async () => {
      const updateData = { title: 'Updated Title' };
      const updatedRetro = { id: 1, title: 'Updated Title', status: 'DRAFT' };
      mockedApi.put.mockResolvedValueOnce({ data: updatedRetro });

      const result = await retroService.update(1, updateData);

      expect(mockedApi.put).toHaveBeenCalledWith('/retros/1', updateData);
      expect(result.data.title).toBe('Updated Title');
    });
  });

  describe('open', () => {
    it('should open a retro', async () => {
      const openedRetro = { id: 1, title: 'Test', status: 'OPEN' };
      mockedApi.post.mockResolvedValueOnce({ data: openedRetro });

      const result = await retroService.open(1);

      expect(mockedApi.post).toHaveBeenCalledWith('/retros/1/open');
      expect(result.data.status).toBe('OPEN');
    });
  });

  describe('close', () => {
    it('should close a retro', async () => {
      const closedRetro = {
        id: 1,
        title: 'Test',
        status: 'CLOSED',
        closedAt: '2026-01-02T10:00:00',
      };
      mockedApi.post.mockResolvedValueOnce({ data: closedRetro });

      const result = await retroService.close(1);

      expect(mockedApi.post).toHaveBeenCalledWith('/retros/1/close');
      expect(result.data.status).toBe('CLOSED');
      expect(result.data.closedAt).toBeDefined();
    });
  });

  describe('delete', () => {
    it('should delete a retro', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await retroService.delete(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/retros/1');
    });
  });

  describe('Retro Items', () => {
    describe('getItems', () => {
      it('should fetch items for a retro', async () => {
        const mockItems = [
          { id: 1, content: 'Item 1', columnType: 'WENT_WELL' },
          { id: 2, content: 'Item 2', columnType: 'DID_NOT_GO_WELL' },
        ];
        mockedApi.get.mockResolvedValueOnce({ data: mockItems });

        const result = await retroService.getItems(1);

        expect(mockedApi.get).toHaveBeenCalledWith('/retros/1/items');
        expect(result.data).toHaveLength(2);
      });
    });

    describe('createItem', () => {
      it('should create a new item', async () => {
        const newItem = {
          content: 'New item',
          columnType: 'TRY_NEXT' as const,
          retrospectiveId: 1,
        };
        const createdItem = { id: 1, ...newItem };
        mockedApi.post.mockResolvedValueOnce({ data: createdItem });

        const result = await retroService.createItem(newItem);

        expect(mockedApi.post).toHaveBeenCalledWith('/retros/items', newItem);
        expect(result.data.content).toBe('New item');
      });
    });

    describe('updateItem', () => {
      it('should update an item', async () => {
        const updatedItem = { id: 1, content: 'Updated content', columnType: 'ACTIONS' };
        mockedApi.put.mockResolvedValueOnce({ data: updatedItem });

        const result = await retroService.updateItem(1, 'Updated content');

        expect(mockedApi.put).toHaveBeenCalledWith('/retros/items/1', { content: 'Updated content' });
        expect(result.data.content).toBe('Updated content');
      });
    });

    describe('deleteItem', () => {
      it('should delete an item', async () => {
        mockedApi.delete.mockResolvedValueOnce({ data: null });

        await retroService.deleteItem(1);

        expect(mockedApi.delete).toHaveBeenCalledWith('/retros/items/1');
      });
    });
  });

  describe('Cycle Status', () => {
    describe('getCycleStatus', () => {
      it('should get cycle retro status', async () => {
        const status = {
          cycleId: 1,
          cycleName: 'Test Cycle',
          totalRetros: 1,
          closedRetros: 1,
          canCloseCycle: true,
          message: 'Cycle can be closed',
        };
        mockedApi.get.mockResolvedValueOnce({ data: status });

        const result = await retroService.getCycleStatus(1);

        expect(mockedApi.get).toHaveBeenCalledWith('/retros/cycle/1/status');
        expect(result.data.canCloseCycle).toBe(true);
      });
    });

    describe('canCloseCycle', () => {
      it('should check if cycle can be closed', async () => {
        mockedApi.get.mockResolvedValueOnce({ data: { canClose: true } });

        const result = await retroService.canCloseCycle(1);

        expect(mockedApi.get).toHaveBeenCalledWith('/retros/cycle/1/can-close');
        expect(result.data.canClose).toBe(true);
      });

      it('should return false when cycle cannot be closed', async () => {
        mockedApi.get.mockResolvedValueOnce({ data: { canClose: false } });

        const result = await retroService.canCloseCycle(1);

        expect(result.data.canClose).toBe(false);
      });
    });
  });

  describe('Project Settings', () => {
    describe('isEnabled', () => {
      it('should check if retros are enabled for project', async () => {
        mockedApi.get.mockResolvedValueOnce({ data: { enabled: true } });

        const result = await retroService.isEnabled(1);

        expect(mockedApi.get).toHaveBeenCalledWith('/retros/project/1/enabled');
        expect(result.data.enabled).toBe(true);
      });
    });

    describe('setEnabled', () => {
      it('should enable retros for project', async () => {
        mockedApi.put.mockResolvedValueOnce({ data: { enabled: true } });

        const result = await retroService.setEnabled(1, true);

        expect(mockedApi.put).toHaveBeenCalledWith('/retros/project/1/enabled', { enabled: true });
        expect(result.data.enabled).toBe(true);
      });

      it('should disable retros for project', async () => {
        mockedApi.put.mockResolvedValueOnce({ data: { enabled: false } });

        const result = await retroService.setEnabled(1, false);

        expect(mockedApi.put).toHaveBeenCalledWith('/retros/project/1/enabled', { enabled: false });
        expect(result.data.enabled).toBe(false);
      });
    });
  });
});
