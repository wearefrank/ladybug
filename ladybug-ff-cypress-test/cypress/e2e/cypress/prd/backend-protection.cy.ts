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

describe('dtap.stage=PRD test whether API URLs are safe', () => {
  const storageName = Cypress.env('debugStorageName') as string;

  const cases: TestCase[] = [
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`, user: 'xxx', expectedStatus: 401 },
  ];
  for (const t of cases) {
    it(testCaseToString(t), () => {
      cy.request({
        method: t.method,
        url: t.url,
        auth: AUTHENTICATIONS.get(t.user)!,
        failOnStatusCode: false,
      }).then(response => {
        cy.wrap(response).its('status').should('equal', t.expectedStatus)
      })
    });
  }
})
