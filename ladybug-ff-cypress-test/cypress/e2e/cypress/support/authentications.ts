interface Authentication {
  username: string;
  password: string;
}

export const AUTHENTICATIONS = new Map<string, Authentication>([
  ['observer', { username: 'observer', password: 'IbisObserver' }],
  ['dataAdmin', { username: 'dataAdmin', password: 'IbisDataAdmin' }],
  ['admin', { username: 'admin', password: 'IbisAdmin'}],
  ['tester', { username: 'tester', password: 'IbisTester' }],
  // User does not have roles
  ['withoutRoles', { username: 'withoutRoles', password: 'withoutRoles'} ],
  // User that does not exist
  ['xxx', { username: 'xxx', password: 'xxx' }],
])
