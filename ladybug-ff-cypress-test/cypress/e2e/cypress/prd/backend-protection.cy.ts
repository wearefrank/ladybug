import { AUTHENTICATIONS } from "../support/commands";

interface TestCase {
  method: string;
  url: string;
  user: string;
  expectedStatus: number;
}

function testCaseToString(t: TestCase): string {
  return `${t.method} ${t.url} as ${t.user} should produce ${t.expectedStatus}`;
}

function doTest(t: TestCase): void {
  cy.request({
    method: t.method,
    url: t.url,
    auth: AUTHENTICATIONS.get(t.user)!,
    failOnStatusCode: false,
  }).then(response => {
    cy.wrap(response).its('status').should('equal', t.expectedStatus)
  })
}

describe('dtap.stage=PRD test whether API URLs are safe', () => {
  const storageName = Cypress.env('debugStorageName') as string;

  // TODO: Add a test user that has no roles and add tests that it has no rights. Requires restart of backend so postponed.
  const metadataApiCases: TestCase[] = [
    // Valid requests, checking user authorized / unauthorized.
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`, user: 'xxx', expectedStatus: 401 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'xxx', expectedStatus: 401 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}/count`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}/count`, user: 'tester', expectedStatus: 200 },
    
    // Nonsensical URLs
    
    // Required query parameter is missing.
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}`, user: 'observer', expectedStatus: 400 },
    // Slash missing between base URL and path parameter.
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}count`, user: 'observer', expectedStatus: 400 },
  ];
  describe('MetadataApi', () => {
    for (const t of metadataApiCases) {
      it(testCaseToString(t), () => doTest(t))
    }
  })
});
