describe('Report buttons', () => {
  before(() => cy.resetApp());

  afterEach(() => {
    cy.clearDebugStore();
  });

  // We omit testing that a Larva test was actually created.
  it('When custom report action is done from checkpoint node then success toast is shown', () => {
    cy.visit('');
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-report-buttons="customReportAction"]').click();
    // The test custom action produces both a success message and an error message.
    // We test that both are shown although this situation is not realistic.
    cy.get(':contains(Success for reports: [Simple report])').should('be.visible');
    cy.get(':contains(Failure!)').should('be.visible');
    cy.get(':contains(Success for reports: [Simple report])', {timeout: 10000}).should('not.exist');
  });

  it('When JSON checkpoint value is prettified then we get more lines', () => {
    cy.visit('');
    cy.createJsonReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', '1');
    // We test that line number 23 is introduced by prettifying.
    // We cannot test for the last line because that does not fit on the windown.
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('not.contain', '23');
    cy.get('[data-cy-report="prettify"]').click();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', '1');
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', '23');
  });

  it('When make null is pressed then the checkpoint value becomes null', () => {
    cy.visit('');
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Hello');
    cy.get('[data-cy-report="alert-messages"] :contains(null)').should('not.exist');
    cy.get('[data-cy-debug-editor="makeNull"]').click();
    // Line number 1, nothing more
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('equal', '1');
    cy.get('[data-cy-report="alert-messages"] :contains(null)').should('be.visible');
  });
});
