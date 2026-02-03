describe('Message context', () => {
  before(() => cy.resetApp());

  afterEach(() => {
    cy.clearDebugStore();
  });

  it('By default the first checkpoint under the root is shown and that checkpoint has an empty message context', () => {
    cy.visit('');
    cy.createReportWithMessageContext();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-messagecontext-table="table"]').should('not.exist');
    cy.get(':contains(Show messagecontext)').should('be.visible');
    cy.get('[data-cy-open-messagecontext-table]').click();
    cy.get(':contains(Hide messagecontext)').should('be.visible');
    cy.get('[data-cy-messagecontext-table="table"]').should('exist');
    cy.get('[data-cy-messagecontext-table="value"]').should('not.exist');
  });

  it('When the report root is selected then the button for the message context is absent', () => {
    cy.visit('');
    cy.createReportWithMessageContext();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.clickRootNodeInFileTree();
    cy.get(':contains(Show messagecontext)').should('not.exist');
    cy.get('[data-cy-messagecontext-table="table"]').should('not.exist');
  });

  it('When a checkpoint with a message context is selected then the value is shown', () => {
    cy.visit('');
    cy.createReportWithMessageContext();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.clickEndCheckpointOfThreeNodeReport();
    cy.get('[data-cy-messagecontext-table="table"]').should('not.exist');
    cy.get(':contains(Show messagecontext)').should('be.visible');
    cy.get('[data-cy-open-messagecontext-table]').click();
    cy.get(':contains(Hide messagecontext)').should('be.visible');
    cy.get('[data-cy-messagecontext-table="table"]').should('be.visible');
    cy.get('[data-cy-messagecontext-table="value"]').should('be.visible');
  });
});
