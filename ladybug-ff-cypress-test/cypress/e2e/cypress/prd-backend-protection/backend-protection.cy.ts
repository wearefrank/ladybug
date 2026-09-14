describe('dtap.stage=PRD test whether API URLs are safe', () => {
  const ibisTesterUser = 'tester';
  const ibisTesterPwd = 'IbisTester';
  const storageName = Cypress.env('debugStorageName') as string;

  it('Role tester can query metadata', () => {
    cy.request({
      method: 'GET',
      url: `/iaf/ladybug/api/metadata/${storageName}?metadataNames=storageId`,
      auth: {
        username: ibisTesterUser,
        password: ibisTesterPwd
      }
    }).then(response => {
      cy.wrap(response).its('status').should('equal', 200)
    })
  });
})
