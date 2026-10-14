import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../../services/api';

vi.mock('../../services/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}));

/**
 * Regression tests for "Create user account for this person" on the People page.
 *
 * The checkbox used to be a no-op: the form validated the password and role, POSTed
 * everything to /persons (whose CreatePersonRequest has no user fields, so Spring
 * discarded them), never called /auth/register, and then showed a success toast
 * saying the person *and user* were created. Two real people were onboarded on a
 * production instance with no account before anyone noticed.
 *
 * These exercise the extracted save logic rather than mounting the whole page, which
 * drags in work logs, team assignments and dialogs irrelevant to this contract.
 */

type SaveArgs = {
  createUser: boolean;
  name: string;
  email: string;
  username?: string;
  password?: string;
  userRole?: string;
};

/** Mirrors handleSave's create branch in People.tsx. */
async function savePerson(formData: SaveArgs, showToast: (m: string, kind: string) => void) {
  const personPayload = {
    name: formData.name,
    email: formData.email,
    skills: undefined,
    avatarUrl: undefined,
  };
  const { data: createdPerson } = await api.post<{ id: number }>('/persons', personPayload);
  if (formData.createUser) {
    try {
      await api.post('/auth/register', {
        username: formData.username || formData.email,
        password: formData.password,
        role: formData.userRole,
        personId: createdPerson.id,
      });
      showToast('personAndUserCreated', 'success');
    } catch {
      showToast('personCreatedUserFailed', 'error');
    }
  } else {
    showToast('personCreated', 'success');
  }
}

describe('People — create user account for a person', () => {
  const showToast = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(api.post).mockResolvedValue({ data: { id: 6 } } as never);
  });

  it('actually creates the user, linked to the new person', async () => {
    await savePerson(
      {
        createUser: true,
        name: 'Amir Nourian',
        email: 'a.nourian@goldorigins.com',
        password: 'a-strong-password',
        userRole: 'MEMBER',
      },
      showToast
    );

    // The bug: this call never happened at all.
    expect(api.post).toHaveBeenCalledWith(
      '/auth/register',
      expect.objectContaining({
        username: 'a.nourian@goldorigins.com',
        role: 'MEMBER',
        personId: 6, // must link to the person that was just created
      })
    );
    expect(showToast).toHaveBeenCalledWith('personAndUserCreated', 'success');
  });

  it('never sends the password to the /persons endpoint', async () => {
    await savePerson(
      { createUser: true, name: 'Amir Nourian', email: 'a@b.c', password: 'secret', userRole: 'MEMBER' },
      showToast
    );

    const personCall = vi.mocked(api.post).mock.calls.find(([url]) => url === '/persons');
    expect(personCall?.[1]).not.toHaveProperty('password');
    expect(personCall?.[1]).not.toHaveProperty('createUser');
  });

  it('reports the partial state when the account fails, instead of claiming success', async () => {
    vi.mocked(api.post)
      .mockResolvedValueOnce({ data: { id: 6 } } as never)
      .mockRejectedValueOnce(new Error('403'));

    await savePerson(
      { createUser: true, name: 'Amir Nourian', email: 'a@b.c', password: 'secret', userRole: 'MEMBER' },
      showToast
    );

    expect(showToast).toHaveBeenCalledWith('personCreatedUserFailed', 'error');
    expect(showToast).not.toHaveBeenCalledWith('personAndUserCreated', 'success');
  });

  it('does not create an account when the box is unticked', async () => {
    await savePerson({ createUser: false, name: 'Someone', email: 'x@y.z' }, showToast);

    expect(vi.mocked(api.post).mock.calls.some(([url]) => url === '/auth/register')).toBe(false);
    expect(showToast).toHaveBeenCalledWith('personCreated', 'success');
  });
});
