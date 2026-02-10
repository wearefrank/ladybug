describe('Report generator', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.initializeApp();
    cy.get('[data-cy-debug="openSettings"]').should('be.visible').click();
    cy.get('[role=dialog]').should('be.visible');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Enabled').invoke('val').should('contain', 'true');
    cy.get('[data-cy-settings="saveChanges"]').click();
  });

  afterEach(() => cy.resetApp());

  it('disable and enable', () => {
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.get('[role=dialog]').should('be.visible');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Disabled').invoke('val').should('contain', 'false');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.contains('Settings saved');
    cy.createOtherReport();
    // If we do not wait here, we do not test properly that no report is created.
    // Without waiting, the test could succeed because we would count the number of reports
    // before refresh.
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="openSettings"]').click();
    cy.get('[role=dialog]').should('be.visible');
    cy.get('[data-cy-settings="generatorEnabled"]').select('Enabled').invoke('val').should('contain', 'true');
    cy.get('[data-cy-settings="saveChanges"]').click();
    cy.contains('Settings saved');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(2);
  });
});
