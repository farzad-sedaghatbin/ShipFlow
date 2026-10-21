import { describe, it, expect } from 'vitest';
import { getPeopleCapabilities } from './peoplePermissions';

/**
 * Regression: a MANAGER on a production instance could open People, click "Add Person", and be
 * refused with "You don't have permission to perform this action", because the server gave
 * MANAGER only USER:READ while the page showed the controls to everyone. The server now grants
 * MANAGER create/update on people (V2026_10_21_0001); these pin what the page offers each role.
 */
describe('getPeopleCapabilities', () => {
  it('lets a manager add and edit people', () => {
    expect(getPeopleCapabilities('MANAGER').canManagePeople).toBe(true);
  });

  it('lets an admin add and edit people', () => {
    expect(getPeopleCapabilities('ADMIN').canManagePeople).toBe(true);
  });

  it('does not offer add/edit to roles the server refuses', () => {
    expect(getPeopleCapabilities('MEMBER').canManagePeople).toBe(false);
    expect(getPeopleCapabilities('READONLY').canManagePeople).toBe(false);
  });

  it('offers the login-account option to admins only, never managers', () => {
    // POST /api/auth/register refuses non-admins when public registration is off, so showing the
    // tickbox to a manager would create the person and then fail the account step.
    expect(getPeopleCapabilities('ADMIN').canCreateLoginAccounts).toBe(true);
    expect(getPeopleCapabilities('MANAGER').canCreateLoginAccounts).toBe(false);
    expect(getPeopleCapabilities('MEMBER').canCreateLoginAccounts).toBe(false);
    expect(getPeopleCapabilities('READONLY').canCreateLoginAccounts).toBe(false);
  });

  it('offers nothing before the user is known or for an unrecognised role', () => {
    for (const role of [undefined, null, '', 'manager', 'SUPERUSER']) {
      expect(getPeopleCapabilities(role)).toEqual({ canManagePeople: false, canCreateLoginAccounts: false });
    }
  });
});
