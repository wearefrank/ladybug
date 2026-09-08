describe('Tests that backend is protected even if frontend permits unauthorized actions', () => {
  beforeEach(() => {
    cy.initializeAppAsUser(Cypress.env('observerUsername'), Cypress.env('observerPassword'));
    cy.get('[data-cy-debug="openSettings"]').as('openSettingsModal').click();
    cy.get('[data-cy-settings="nav-server"]').click();
    cy.get('[data-cy-settings="factoryReset"]').click();
    cy.get('[data-cy-settings="root"]').should('not.exist');
  })

  it('When observer tries to disable report generator then error and not executed', () => {
    cy.initializeAppAsUser(Cypress.env('observerUsername'), Cypress.env('observerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
    cy.get('[data-cy-settings="generatorEnabled"]').should('not.be.disabled');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-toast="warning"]').should('contains.text', 'Not allowed');
    cy.get('[data-cy-settings="root"]').should('not.exist');
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
  })

  it('When tester tries to disable report generator then no error and done', () => {
    cy.initializeAppAsUser(Cypress.env('testerUsername'), Cypress.env('testerPassword'));
    cy.enterSettingsDialogAndExpectReportGenerator('Enabled');
    cy.get('[data-cy-settings="generatorEnabled"]').should('not.be.disabled');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.get('[data-cy-toast]').should('not.exist');
    cy.get('[data-cy-settings="root"]').should('not.exist');
    cy.enterSettingsDialogAndExpectReportGenerator('Disabled');
  })
})