import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { taskService } from '../services/taskService';
import { getUserFriendlyError } from '../utils/errorMessages';
import type { Task } from '../types';

/**
 * Convert a task to / from a subtask with an Undo toast, Jira-style.
 *
 * Converting moves the task into its parent's cycle and lifts it out of Backlog, so Undo restores
 * all three things (parent, cycle, status) from the snapshot taken before the change — not just
 * the parent link.
 *
 * @param onChanged called after every successful change (including Undo) so the page can reload.
 */
export function useTaskHierarchy(onChanged: () => void | Promise<void>) {
  const { t } = useTranslation();

  const restore = useCallback(
    async (before: Task) => {
      try {
        const detached = (await taskService.setParent(before.id, null)).data;
        if ((detached.cycleId ?? null) !== (before.cycleId ?? null)) {
          await taskService.assignCycle(before.id, before.cycleId ?? null);
        }
        if (detached.status !== before.status) {
          await taskService.updateStatus(before.id, before.status);
        }
        toast.success(t('taskHierarchy.undone', { task: before.title }));
        await onChanged();
      } catch (error) {
        toast.error(getUserFriendlyError(error, t));
      }
    },
    [onChanged, t]
  );

  /** Make `child` a subtask of `parent`. `child` is the snapshot used for Undo. */
  const makeSubtask = useCallback(
    async (child: Task, parent: Task) => {
      try {
        await taskService.setParent(child.id, parent.id);
        toast.success(t('taskHierarchy.converted', { task: child.title, parent: parent.title }), {
          action: { label: t('taskHierarchy.undo'), onClick: () => void restore(child) },
          duration: 8000,
        });
        await onChanged();
        return true;
      } catch (error) {
        toast.error(getUserFriendlyError(error, t));
        return false;
      }
    },
    [onChanged, restore, t]
  );

  /** Turn a subtask back into a top-level task. Undo re-attaches it to the same parent. */
  const makeTopLevel = useCallback(
    async (child: Task) => {
      const parentId = child.parentTaskId;
      try {
        await taskService.setParent(child.id, null);
        toast.success(t('taskHierarchy.detached', { task: child.title }), {
          action: parentId
            ? {
                label: t('taskHierarchy.undo'),
                onClick: () =>
                  void taskService
                    .setParent(child.id, parentId)
                    .then(() => onChanged())
                    .catch((error) => toast.error(getUserFriendlyError(error, t))),
              }
            : undefined,
          duration: 8000,
        });
        await onChanged();
      } catch (error) {
        toast.error(getUserFriendlyError(error, t));
      }
    },
    [onChanged, t]
  );

  return { makeSubtask, makeTopLevel };
}
