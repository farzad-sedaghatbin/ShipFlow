import { describe, it, expect, vi, beforeEach } from 'vitest';
import { wikiPermissionService } from '../../../services/wikiPermissionService';

// Mock the api module (path relative to the service file).
vi.mock('../../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '../../../services/api';
const mockedApi = vi.mocked(api, true);

describe('wikiPermissionService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('list', () => {
    it('calls GET /wiki/spaces/:id/permissions', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiPermissionService.list(6);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/spaces/6/permissions');
    });
  });

  describe('grant', () => {
    it('POSTs a USER grant with granteeType/granteeRef/level', async () => {
      const req = {
        granteeType: 'USER' as const,
        granteeRef: '10',
        level: 'WRITE' as const,
      };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 1, spaceId: 6, ...req } });
      await wikiPermissionService.grant(6, req);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/spaces/6/permissions', req);
    });

    it('POSTs a ROLE grant with the role name as granteeRef', async () => {
      const req = {
        granteeType: 'ROLE' as const,
        granteeRef: 'MEMBER',
        level: 'READ' as const,
      };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 2, spaceId: 6, ...req } });
      await wikiPermissionService.grant(6, req);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/spaces/6/permissions', req);
    });
  });

  describe('revoke', () => {
    it('calls DELETE /wiki/permissions/:permId', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });
      await wikiPermissionService.revoke(99);
      expect(mockedApi.delete).toHaveBeenCalledWith('/wiki/permissions/99');
    });
  });
});
