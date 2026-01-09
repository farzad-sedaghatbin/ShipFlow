import { describe, it, expect, vi, beforeEach } from 'vitest';
import { taskService } from '../services/taskService';

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

describe('taskService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAll', () => {
    it('should fetch all tasks', async () => {
      const mockTasks = [
        { id: 1, title: 'Task 1', status: 'TODO' },
        { id: 2, title: 'Task 2', status: 'IN_PROGRESS' },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getAll();

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getById', () => {
    it('should fetch a task by id', async () => {
      const mockTask = { id: 1, title: 'Task 1', status: 'TODO' };
      mockedApi.get.mockResolvedValueOnce({ data: mockTask });

      const result = await taskService.getById(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/1');
      expect(result.data).toEqual(mockTask);
    });
  });

  describe('getByCycleId', () => {
    it('should fetch tasks by cycle id', async () => {
      const mockTasks = [{ id: 1, title: 'Task 1', cycleId: 1 }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getByCycleId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getByCycleIdAndStatus', () => {
    it('should fetch tasks by cycle id and status', async () => {
      const mockTasks = [{ id: 1, title: 'Task 1', cycleId: 1, status: 'TODO' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getByCycleIdAndStatus(1, 'TODO');

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1/status/TODO');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getByAssigneeId', () => {
    it('should fetch tasks by assignee id', async () => {
      const mockTasks = [{ id: 1, title: 'Task 1', assigneeId: 1 }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getByAssigneeId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/assignee/1');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getByPersonId', () => {
    it('should fetch tasks by person id (assignee or pair)', async () => {
      const mockTasks = [{ id: 1, title: 'Task 1' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getByPersonId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/person/1');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getStatisticsByCycleId', () => {
    it('should fetch task statistics for a cycle', async () => {
      const mockStats = {
        cycleId: 1,
        totalTasks: 10,
        doneTasks: 3,
        inProgressTasks: 4,
        completionPercentage: 30.0,
      };
      mockedApi.get.mockResolvedValueOnce({ data: mockStats });

      const result = await taskService.getStatisticsByCycleId(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1/statistics');
      expect(result.data).toEqual(mockStats);
    });
  });

  describe('getMy', () => {
    it('should fetch current user tasks', async () => {
      const mockTasks = [{ id: 1, title: 'My Task' }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getMy();

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/my');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('getMyByCycle', () => {
    it('should fetch current user tasks by cycle', async () => {
      const mockTasks = [{ id: 1, title: 'My Task', cycleId: 1 }];
      mockedApi.get.mockResolvedValueOnce({ data: mockTasks });

      const result = await taskService.getMyByCycle(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/my/cycle/1');
      expect(result.data).toEqual(mockTasks);
    });
  });

  describe('create', () => {
    it('should create a new task', async () => {
      const newTask = {
        title: 'New Task',
        description: 'Description',
        cycleId: 1,
        status: 'TODO' as const,
        priority: 'MEDIUM' as const,
        estimateHours: 8,
        assigneeId: 1,
      };
      const createdTask = { id: 1, ...newTask };
      mockedApi.post.mockResolvedValueOnce({ data: createdTask });

      const result = await taskService.create(newTask);

      expect(mockedApi.post).toHaveBeenCalledWith('/tasks', newTask);
      expect(result.data).toEqual(createdTask);
    });

    it('should create a task with pair assignee', async () => {
      const newTask = {
        title: 'Pair Task',
        cycleId: 1,
        assigneeId: 1,
        pairAssigneeId: 2,
      };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 1, ...newTask } });

      await taskService.create(newTask);

      expect(mockedApi.post).toHaveBeenCalledWith('/tasks', newTask);
    });

    it('should create a task with due date', async () => {
      const newTask = {
        title: 'Task with Due Date',
        cycleId: 1,
        dueDate: '2025-01-15',
      };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 1, ...newTask } });

      await taskService.create(newTask);

      expect(mockedApi.post).toHaveBeenCalledWith('/tasks', newTask);
    });
  });

  describe('update', () => {
    it('should update an existing task', async () => {
      const updateData = {
        title: 'Updated Task',
        description: 'Updated description',
        cycleId: 1,
        status: 'IN_PROGRESS' as const,
        priority: 'HIGH' as const,
      };
      const updatedTask = { id: 1, ...updateData };
      mockedApi.put.mockResolvedValueOnce({ data: updatedTask });

      const result = await taskService.update(1, updateData);

      expect(mockedApi.put).toHaveBeenCalledWith('/tasks/1', updateData);
      expect(result.data).toEqual(updatedTask);
    });
  });

  describe('updateStatus', () => {
    it('should update task status only', async () => {
      const updatedTask = { id: 1, title: 'Task', status: 'DONE' };
      mockedApi.patch.mockResolvedValueOnce({ data: updatedTask });

      const result = await taskService.updateStatus(1, 'DONE');

      expect(mockedApi.patch).toHaveBeenCalledWith('/tasks/1/status', { status: 'DONE' });
      expect(result.data).toEqual(updatedTask);
    });

    it('should update status to BLOCKED', async () => {
      mockedApi.patch.mockResolvedValueOnce({ data: { id: 1, status: 'BLOCKED' } });

      await taskService.updateStatus(1, 'BLOCKED');

      expect(mockedApi.patch).toHaveBeenCalledWith('/tasks/1/status', { status: 'BLOCKED' });
    });
  });

  describe('delete', () => {
    it('should delete a task', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });

      await taskService.delete(1);

      expect(mockedApi.delete).toHaveBeenCalledWith('/tasks/1');
    });
  });
});
