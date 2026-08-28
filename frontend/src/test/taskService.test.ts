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
    it('should fetch all tasks with pagination', async () => {
      const mockResponse = {
        content: [
          { id: 1, title: 'Task 1', status: 'TODO' },
          { id: 2, title: 'Task 2', status: 'IN_PROGRESS' },
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 10,
      };
      mockedApi.get.mockResolvedValueOnce({ data: mockResponse });

      const result = await taskService.getAll(0, 10, 'createdAt', 'desc');

      // getAll also forwards the Backlog filter params (statuses/priorities/
      // assigneeIds/creatorIds/category/exclude); they are undefined when unused.
      expect(mockedApi.get).toHaveBeenCalledWith('/tasks', {
        params: expect.objectContaining({
          page: 0,
          size: 10,
          sortBy: 'createdAt',
          sortOrder: 'desc',
        }),
      });
      expect(result.data).toEqual(mockResponse);
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

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1/all');
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
    it('should fetch current user tasks with pagination', async () => {
      const mockResponse = {
        content: [{ id: 1, title: 'My Task' }],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 10,
      };
      mockedApi.get.mockResolvedValueOnce({ data: mockResponse });

      const result = await taskService.getMy(0, 10, 'createdAt', 'desc');

      // As with getAll, the filter params ride along as undefined when unused.
      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/my', {
        params: expect.objectContaining({
          page: 0,
          size: 10,
          sortBy: 'createdAt',
          sortOrder: 'desc',
        }),
      });
      expect(result.data).toEqual(mockResponse);
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

  describe('getSubTasks', () => {
    it('should fetch subtasks for a parent task', async () => {
      const mockSubTasks = [
        { id: 2, title: 'Subtask 1', parentTaskId: 1 },
        { id: 3, title: 'Subtask 2', parentTaskId: 1 },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockSubTasks });

      const result = await taskService.getSubTasks(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/1/subtasks');
      expect(result.data).toEqual(mockSubTasks);
    });
  });

  describe('getRootTasks', () => {
    it('should fetch root tasks for a cycle', async () => {
      const mockRootTasks = [
        { id: 1, title: 'Root Task 1', parentTaskId: null },
        { id: 4, title: 'Root Task 2', parentTaskId: null },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockRootTasks });

      const result = await taskService.getRootTasks(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1/roots');
      expect(result.data).toEqual(mockRootTasks);
    });
  });

  describe('getTaskTree', () => {
    it('should fetch task tree for a cycle', async () => {
      const mockTaskTree = [
        { 
          id: 1, 
          title: 'Parent Task', 
          parentTaskId: null,
          children: [
            { id: 2, title: 'Subtask 1', parentTaskId: 1 },
            { id: 3, title: 'Subtask 2', parentTaskId: 1 },
          ]
        },
      ];
      mockedApi.get.mockResolvedValueOnce({ data: mockTaskTree });

      const result = await taskService.getTaskTree(1);

      expect(mockedApi.get).toHaveBeenCalledWith('/tasks/cycle/1/tree');
      expect(result.data).toEqual(mockTaskTree);
    });
  });
});
