import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import NotificationCenter from '../NotificationCenter';

// The bell dropdown used to show read and unread notifications identically and had no way to clear
// the list (its per-row buttons were also pushed out of view by the long titles).

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { count?: number }) => (opts?.count !== undefined ? `${key}:${opts.count}` : key),
    i18n: { language: 'en' },
  }),
}));

vi.mock('../../hooks/useNotificationStream', () => ({ useNotificationStream: vi.fn() }));

const api = vi.hoisted(() => ({
  getAllNotifications: vi.fn(),
  getUnreadCount: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
  deleteNotification: vi.fn(),
  clearAll: vi.fn(),
}));
vi.mock('../../services/dashboardApi', () => ({ dashboardNotificationApi: api }));

const note = (id: number, isRead: boolean) => ({
  id,
  type: 'TASK_OVERDUE',
  title: `Task Overdue ${id}`,
  message: 'is overdue',
  severity: 'WARNING',
  isRead,
  createdAt: new Date().toISOString(),
});

async function openMenu() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationCenter />
    </MemoryRouter>,
  );
  await user.click(await screen.findByRole('button', { name: /notificationCenter\./ }));
  return user;
}

describe('NotificationCenter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.getAllNotifications.mockResolvedValue([note(1, false), note(2, true)]);
    api.getUnreadCount.mockResolvedValue(1);
    api.clearAll.mockResolvedValue(undefined);
  });

  it('marks unread notifications distinctly from read ones', async () => {
    await openMenu();

    const unreadRow = (await screen.findByText('Task Overdue 1')).closest('[class*="border-l-4"]')!;
    const readRow = screen.getByText('Task Overdue 2').closest('[class*="border-l-4"]')!;

    expect(unreadRow.querySelector('[aria-label="notificationCenter.unread"]')).not.toBeNull();
    expect(readRow.querySelector('[aria-label="notificationCenter.unread"]')).toBeNull();
    expect(unreadRow.className).toContain('bg-primary/10');
    expect(readRow.className).toContain('opacity-60');
  });

  it('offers a remove button on every row', async () => {
    const user = await openMenu();
    await screen.findByText('Task Overdue 1');

    const removeButtons = screen.getAllByRole('button', { name: 'notificationCenter.delete' });
    expect(removeButtons).toHaveLength(2);

    await user.click(removeButtons[1]);
    expect(api.deleteNotification).toHaveBeenCalledWith(2);
  });

  it('clears everything only after a second confirming click', async () => {
    const user = await openMenu();
    await screen.findByText('Task Overdue 1');

    await user.click(screen.getByRole('button', { name: /notificationCenter\.clearAll$/ }));
    expect(api.clearAll).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: /notificationCenter\.clearAllConfirm/ }));
    await waitFor(() => expect(api.clearAll).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('notificationCenter.empty')).toBeTruthy();
  });
});
