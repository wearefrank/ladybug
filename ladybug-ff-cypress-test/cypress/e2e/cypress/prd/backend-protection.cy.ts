import { AUTHENTICATIONS } from "../support/commands";

const API_BASE = '/iaf/ladybug/api/';

interface TestCase {
  method: string;
  url: string;
  user: string;
  body?: string;
  expectedStatus: number;
}

function testCaseToString(t: TestCase): string {
  return `${t.method} ${t.url} as ${t.user} should produce ${t.expectedStatus}`;
}

function doTest(t: TestCase): void {
  cy.request({
    method: t.method,
    url: `${API_BASE}${t.url}`,
    auth: AUTHENTICATIONS.get(t.user)!,
    headers: { 'Content-Type': 'application/json' },
    body: t.body,
    failOnStatusCode: false,
  }).then(response => {
    cy.wrap(response).its('status').should('equal', t.expectedStatus)
  })
}

describe('dtap.stage=PRD test whether API URLs are safe', () => {
  const storageName = Cypress.env('debugStorageName') as string;

  // TODO: Add a test user that has no roles and add tests that it has no rights. Requires restart of backend so postponed.
  const simpleCases: TestCase[] = [
    /*
     * MetadataApi
     */

    // Valid requests.

    { method: 'GET', url: `metadata/${storageName}?metadataNames=storageId`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `metadata/${storageName}?metadataNames=storageId`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `metadata/${storageName}?metadataNames=storageId`, user: 'xxx', expectedStatus: 401 },
    { method: 'GET', url: `metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `metadata/${storageName}/userHelp?metadataNames=storageId`, user: 'xxx', expectedStatus: 401 },
    { method: 'GET', url: `metadata/${storageName}/count`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `metadata/${storageName}/count`, user: 'tester', expectedStatus: 200 },

    // Nonsensical URLs.

    // Required query parameter is missing.
    { method: 'GET', url: `metadata/${storageName}`, user: 'observer', expectedStatus: 400 },
    // Slash missing between base URL and path parameter.
    { method: 'GET', url: `metadata/${storageName}count`, user: 'observer', expectedStatus: 400 },
  ];

  const withReportCases: TestCase[] = [
    // Report API happy.

    { method: 'GET', url: `report/${storageName}/<<storageId>>`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `report/${storageName}/<<storageId>>`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `report/${storageName}/<<storageId>>/checkpoints/uids?view=White%20box&invert=false`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `report/${storageName}/<<storageId>>/checkpoints/uids?view=White%20box&invert=false`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `report/${storageName}?storageIds=<<storageId>>`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `report/${storageName}?storageIds=<<storageId>>`, user: 'tester', expectedStatus: 200 },
    { method: 'GET', url: `report/shownReports/${storageName}?storageIds=<<storageId>>&view=White%20box`, user: 'observer', expectedStatus: 200 },
    { method: 'GET', url: `report/shownReports/${storageName}?storageIds=<<storageId>>&view=White%20box`, user: 'tester', expectedStatus: 200 },
    { method: 'PUT', url: `report/store/Test`, body: `{"${storageName}": [<<storageId>>]}`, user: 'observer', expectedStatus: 403 },
    { method: 'PUT', url: `report/store/Test`, body: `{"${storageName}": [<<storageId>>]}`, user: 'dataAdmin', expectedStatus: 200 },
    { method: 'PUT', url: `report/store/Test`, body: `{"${storageName}": [<<storageId>>]}`, user: 'admin', expectedStatus: 200 },
    { method: 'PUT', url: `report/store/Test`, body: `{"${storageName}": [<<storageId>>]}`, user: 'tester', expectedStatus: 200 },
    { method: 'DELETE', url: `report/Test?storageIds=<<testStorageId>>`, user: 'observer', expectedStatus: 403 },
    { method: 'DELETE', url: `report/Test?storageIds=<<testStorageId>>`, user: 'dataAdmin', expectedStatus: 200 },
    { method: 'DELETE', url: `report/Test?storageIds=<<testStorageId>>`, user: 'admin', expectedStatus: 200 },
    { method: 'DELETE', url: `report/Test?storageIds=<<testStorageId>>`, user: 'tester', expectedStatus: 200 },

    // Run API happy.

    { method: 'POST', url: 'runner/run/Test/<<testStorageId>>', user: 'observer', expectedStatus: 403 },
    { method: 'POST', url: 'runner/run/Test/<<testStorageId>>', user: 'dataAdmin', expectedStatus: 403 },
    { method: 'POST', url: 'runner/run/Test/<<testStorageId>>', user: 'admin', expectedStatus: 403 },
    { method: 'POST', url: 'runner/run/Test/<<testStorageId>>', user: 'tester', expectedStatus: 200 },

    // Invalid URLs

    // Missing all query parameters
    { method: 'GET', url: `report/${storageName}/<<storageId>>/checkpoints/uids`, user: 'tester', expectedStatus: 400 },
    // Misses mandator query parameter "invert"
    { method: 'GET', url: `report/${storageName}/<<storageId>>/checkpoints/uids?view=White%20box`, user: 'observer', expectedStatus: 400 },
    // Missing mandatory query parameter storageIds
    { method: 'GET', url: `report/${storageName}`, user: 'observer', expectedStatus: 400 },
    { method: 'GET', url: `report/shownReports/${storageName}&view=White%20box`, user: 'observer', expectedStatus: 400 },
  ]

  describe('Simple cases that do not depend on anything', () => {
    for (const t of simpleCases) {
      it(testCaseToString(t), () => doTest(t))
    }
  })


  describe('With report', () => {
    const TEST_STORAGE_NAME = 'Test';
    let storageId: number;
    let testStorageId: number;

    before(() => {
      cy.apiDeleteAllAsTester(storageName)
      cy.apiSetGeneratorEnabledAsTester(true)
    })

    after(() => {
      cy.apiSetGeneratorEnabledAsTester(false)
    })

    beforeEach(() => {
      cy.createReportWithTestPipelineApi('Example1a', 'Adapter1a', 'xxx', 'tester', 'IbisTester')
      cy.request({
        method: 'GET',
        url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`,
        auth: AUTHENTICATIONS.get('tester')!,
      }).then(response => {
        cy.wrap(response.body).should('have.length', 1)
        storageId = parseInt(response.body[0].storageId)
        cy.apiCopyReportToTestTabAsTester(storageName, storageId).then(newStorageId => {
          testStorageId = newStorageId
        })
      })
    })

    afterEach(() => {
      cy.apiDeleteAllAsTester(storageName)
      cy.apiDeleteAllAsTester(TEST_STORAGE_NAME)
    })

    function fillPlaceholders(v: string): string {
      return v
        .replace('<<storageId>>', `${storageId}`)
        .replace('<<testStorageId>>', `${testStorageId}`)
    }

    for(const c of withReportCases) {
      it(testCaseToString(c), () => {
        const url = fillPlaceholders(c.url);
        const body = c.body === undefined ? undefined : fillPlaceholders(c.body);
        const t: TestCase = {
          method: c.method,
          url,
          body,
          user: c.user,
          expectedStatus: c.expectedStatus,
        }
        doTest(t);
      })
    }
  })
});
