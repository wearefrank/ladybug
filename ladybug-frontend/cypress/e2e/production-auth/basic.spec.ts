describe('Tests with authentication as in production', () => {
  it('When logged in as IbisObserver then debug settings dialog disables field to enable/disable report generator', () => {
    cy.initializeAppAsUser(Cypress.env('observerUsername'), Cypress.env('observerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
    cy.get('[data-cy-settings="generatorEnabled"]').should('be.disabled');
  })

  it('When logged in as IbisObserver then can revert report generator to factory settings', () => {
    cy.initializeAppAsUser(Cypress.env('testerUsername'), Cypress.env('testerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
    cy.get('[data-cy-settings="generatorEnabled"]').should('not.be.disabled');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-settings="root"]').should('not.exist');
    cy.initializeAppAsUser(Cypress.env('observerUsername'), Cypress.env('observerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Disabled');
    cy.get('[data-cy-settings="generatorEnabled"]').should('be.disabled');
    cy.get('[data-cy-settings="factoryReset"]').click();
    cy.get('[data-cy-settings="root"]').should('not.exist');
    cy.initializeAppAsUser(Cypress.env('observerUsername'), Cypress.env('observerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
  })
})