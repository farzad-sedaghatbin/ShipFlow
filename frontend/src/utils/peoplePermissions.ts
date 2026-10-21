/**
 * What a role may do on the People page. These mirror the server, so the page stops offering
 * controls the API will refuse (a manager used to see "Add Person", click it, and get
 * "You don't have permission to perform this action"):
 *
 * - Creating/editing a PERSON needs USER:CREATE / USER:UPDATE on PersonController. ADMIN and
 *   MANAGER hold them (the MANAGER grant is V2026_10_21_0001; V58 had given MANAGER read only).
 * - Creating a LOGIN ACCOUNT goes through POST /api/auth/register, which only an authenticated
 *   ADMIN is exempt from the registration gate for (UserService.createUser).
 *
 * If either server rule changes, change it here too.
 */
export interface PeopleCapabilities {
  canManagePeople: boolean;
  canCreateLoginAccounts: boolean;
}

export function getPeopleCapabilities(role?: string | null): PeopleCapabilities {
  return {
    canManagePeople: role === 'ADMIN' || role === 'MANAGER',
    canCreateLoginAccounts: role === 'ADMIN',
  };
}
