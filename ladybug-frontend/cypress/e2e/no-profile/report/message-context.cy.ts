describe('Message context', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

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
    cy.get('[data-cy-open-messagecontext-table]').should('not.be.checked');
    cy.get('[data-cy-open-messagecontext-table]').click();
    cy.get('[data-cy-open-messagecontext-table]').should('be.checked');
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
    cy.get('[data-cy-open-messagecontext-table]').should('not.exist');
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
    cy.get('[data-cy-open-messagecontext-table]').should('not.be.checked');
    cy.get('[data-cy-open-messagecontext-table]').click();
    cy.get('[data-cy-open-messagecontext-table]').should('be.checked');
    cy.get('[data-cy-messagecontext-table="table"]').should('be.visible');
    cy.get('[data-cy-messagecontext-table="value"]').should('be.visible');
  });

  it('The checkbox for the message context stays consistent with whether the table is actually shown when switching checkpoints', () => {
    cy.visit('');
    cy.createReportWithMessageContext();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    // First checkpoint is selected by default, checkbox and table both start out closed.
    cy.get('[data-cy-open-messagecontext-table]').should('not.be.checked');
    assertMessageContextCheckboxConsistentWithTable();
    // Open the message context table for this checkpoint.
    cy.get('[data-cy-open-messagecontext-table]').click();
    cy.get('[data-cy-open-messagecontext-table]').should('be.checked');
    // The start checkpoint has an empty message context, so the table div renders
    // with no content and collapses to zero height; assert it exists, not that it is visible.
    cy.get('[data-cy-messagecontext-table="table"]').should('exist');
    assertMessageContextCheckboxConsistentWithTable();
    // Selecting another checkpoint hides the table again; the checkbox must follow.
    cy.clickEndCheckpointOfThreeNodeReport();
    assertMessageContextCheckboxConsistentWithTable();
  });
});

// Checks the invariant that the checkbox is checked if, and only if, the message context table is shown.
function assertMessageContextCheckboxConsistentWithTable() {
  cy.get('[data-cy-open-messagecontext-table]').then(($checkbox) => {
    const isChecked = $checkbox.is(':checked');
    // Never expect 'be.visible' because that won't become true for an empty message context.
    cy.get('[data-cy-messagecontext-table="table"]').should(isChecked ? 'exist' : 'not.exist');
  });
}
