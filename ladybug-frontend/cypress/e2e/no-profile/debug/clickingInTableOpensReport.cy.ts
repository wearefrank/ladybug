describe('Clicking a report', () => {
  before(() => {
    cy.resetApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

  it('Selecting report should show a tree', () => {
    cy.get('[data-cy-debug-tree="buttons"]').should('not.exist');
    cy.getDebugTableRows().first().click();
    cy.get('[data-cy-debug-tree="buttons"]').should('be.visible');
  });

  it('Selecting report should show display', () => {
    cy.get('[data-cy-debug-editor="buttons"]').should('not.exist');
    cy.get('[data-cy-element-name="checkpointEditor"]').should('not.exist');
    cy.getDebugTableRows().first().click();
    cy.get('[data-cy-debug-editor="buttons"]').should('be.visible');
    cy.get('[data-cy-element-name="checkpointEditor"]').should('be.visible');
  });
});
