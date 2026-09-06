import { describe, it, expect, vi, beforeEach } from 'vitest';
import { wikiService } from '../../../services/wikiService';

// Mock the api module (path relative to the service file)
vi.mock('../../../services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '../../../services/api';
const mockedApi = vi.mocked(api, true);

describe('wikiService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ── Spaces ──────────────────────────────────────────────────────────────────

  describe('getSpaces', () => {
    it('calls GET /wiki/spaces', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.getSpaces();
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/spaces');
    });
  });

  describe('createSpace', () => {
    it('calls POST /wiki/spaces with the request body', async () => {
      const req = { name: 'Engineering', spaceKey: 'ENG' };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 1, ...req } });
      await wikiService.createSpace(req);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/spaces', req);
    });
  });

  describe('getSpace', () => {
    it('calls GET /wiki/spaces/:id', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: { id: 5 } });
      await wikiService.getSpace(5);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/spaces/5');
    });
  });

  describe('updateSpace', () => {
    it('calls PUT /wiki/spaces/:id with the request body', async () => {
      const req = { name: 'Updated' };
      mockedApi.put.mockResolvedValueOnce({ data: { id: 2, ...req } });
      await wikiService.updateSpace(2, req);
      expect(mockedApi.put).toHaveBeenCalledWith('/wiki/spaces/2', req);
    });
  });

  describe('deleteSpace', () => {
    it('calls DELETE /wiki/spaces/:id', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });
      await wikiService.deleteSpace(3);
      expect(mockedApi.delete).toHaveBeenCalledWith('/wiki/spaces/3');
    });
  });

  describe('getSpaceTree', () => {
    it('calls GET /wiki/spaces/:id/tree', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.getSpaceTree(4);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/spaces/4/tree');
    });
  });

  // Space permission (sharing) calls now live in wikiPermissionService — see
  // wikiPermissionService.test.ts.

  // ── Pages ───────────────────────────────────────────────────────────────────

  describe('createPage', () => {
    it('calls POST /wiki/pages with the request body', async () => {
      const req = { spaceId: 1, title: 'Getting Started' };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 7, ...req } });
      await wikiService.createPage(req);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/pages', req);
    });
  });

  describe('getPage', () => {
    it('calls GET /wiki/pages/:id', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: { id: 7 } });
      await wikiService.getPage(7);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/pages/7');
    });
  });

  describe('updatePage', () => {
    it('calls PUT /wiki/pages/:id with the request body', async () => {
      const req = { title: 'Updated', content: '[]' };
      mockedApi.put.mockResolvedValueOnce({ data: { id: 8, ...req } });
      await wikiService.updatePage(8, req);
      expect(mockedApi.put).toHaveBeenCalledWith('/wiki/pages/8', req);
    });

    it('includes expectedVersion in the body for the optimistic-lock check (v1.13.0 S64)', async () => {
      const req = { title: 'Updated', content: '[]', expectedVersion: 5 };
      mockedApi.put.mockResolvedValueOnce({ data: { id: 8, ...req, version: 6 } });
      await wikiService.updatePage(8, req);
      expect(mockedApi.put).toHaveBeenCalledWith('/wiki/pages/8', expect.objectContaining({ expectedVersion: 5 }));
    });
  });

  describe('deletePage', () => {
    it('calls DELETE /wiki/pages/:id', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });
      await wikiService.deletePage(9);
      expect(mockedApi.delete).toHaveBeenCalledWith('/wiki/pages/9');
    });
  });

  describe('movePage', () => {
    it('calls POST /wiki/pages/:id/move with the request body', async () => {
      const req = { newParentId: null, newIndex: 2 };
      mockedApi.post.mockResolvedValueOnce({ data: { id: 10 } });
      await wikiService.movePage(10, req);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/pages/10/move', req);
    });
  });

  describe('getPageHistory', () => {
    it('calls GET /wiki/pages/:id/history', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.getPageHistory(11);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/pages/11/history');
    });
  });

  describe('restorePage', () => {
    it('calls POST /wiki/pages/:id/restore/:revision', async () => {
      mockedApi.post.mockResolvedValueOnce({ data: { id: 11 } });
      await wikiService.restorePage(11, 3);
      expect(mockedApi.post).toHaveBeenCalledWith('/wiki/pages/11/restore/3');
    });
  });

  describe('searchPages', () => {
    it('calls GET /wiki/pages/search with q and optional spaceId params', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.searchPages('hello', 2);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/pages/search', {
        params: { q: 'hello', spaceId: 2 },
      });
    });

    it('calls GET /wiki/pages/search with only q when spaceId is omitted', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.searchPages('world');
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/pages/search', {
        params: { q: 'world', spaceId: undefined },
      });
    });
  });

  // ── Attachments ─────────────────────────────────────────────────────────────

  describe('getAttachments', () => {
    it('calls GET /wiki/pages/:id/attachments', async () => {
      mockedApi.get.mockResolvedValueOnce({ data: [] });
      await wikiService.getAttachments(12);
      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/pages/12/attachments');
    });
  });

  describe('uploadAttachment', () => {
    it('calls POST /wiki/pages/:id/attachments with FormData', async () => {
      const file = new File(['content'], 'test.txt', { type: 'text/plain' });
      mockedApi.post.mockResolvedValueOnce({ data: { id: 1 } });
      await wikiService.uploadAttachment(12, file);
      expect(mockedApi.post).toHaveBeenCalledWith(
        '/wiki/pages/12/attachments',
        expect.any(FormData)
      );
    });
  });

  describe('deleteAttachment', () => {
    it('calls DELETE /wiki/attachments/:attId', async () => {
      mockedApi.delete.mockResolvedValueOnce({ data: null });
      await wikiService.deleteAttachment(55);
      expect(mockedApi.delete).toHaveBeenCalledWith('/wiki/attachments/55');
    });
  });

  describe('downloadAttachment', () => {
    it('GETs the endpoint as a blob and triggers a save (not a raw href)', async () => {
      const blob = new Blob(['x'], { type: 'image/png' });
      mockedApi.get.mockResolvedValueOnce({ data: blob });
      const createObjectURL = vi.fn(() => 'blob:mock');
      const revokeObjectURL = vi.fn();
      window.URL.createObjectURL = createObjectURL;
      window.URL.revokeObjectURL = revokeObjectURL;

      await wikiService.downloadAttachment(42, 'pic.png');

      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/attachments/42/download', {
        responseType: 'blob',
      });
      expect(createObjectURL).toHaveBeenCalledWith(blob);
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock');
    });
  });

  describe('fetchAttachmentObjectUrl', () => {
    it('GETs the endpoint as a blob and returns an object URL for preview', async () => {
      const blob = new Blob(['x'], { type: 'image/png' });
      mockedApi.get.mockResolvedValueOnce({ data: blob });
      window.URL.createObjectURL = vi.fn(() => 'blob:preview');

      const url = await wikiService.fetchAttachmentObjectUrl(7);

      expect(mockedApi.get).toHaveBeenCalledWith('/wiki/attachments/7/download', {
        responseType: 'blob',
      });
      expect(url).toBe('blob:preview');
    });
  });
});
