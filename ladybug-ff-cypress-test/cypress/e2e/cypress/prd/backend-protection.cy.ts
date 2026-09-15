import { AUTHENTICATIONS } from "../support/commands";

describe('dtap.stage=PRD test whether API URLs are safe', () => {
  const storageName = Cypress.env('debugStorageName') as string;

  it('Role tester can query metadata', () => {
    cy.request({
      method: 'GET',
      url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`,
      auth: AUTHENTICATIONS.get('tester')!,
    }).then(response => {
      cy.wrap(response).its('status').should('equal', 200)
    })
  });
})
