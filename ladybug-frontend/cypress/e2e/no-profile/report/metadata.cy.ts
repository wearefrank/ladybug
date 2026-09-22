describe('Metadata', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.clearDebugStore();
  });

  it('When root node is selected then metadata can be shown', () => {
    cy.visit('');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.clickRootNodeInFileTree();
    cy.wait(200);
    cy.get('[data-cy-metadata-table="table"]').should('not.exist');
    cy.get('[data-cy-open-metadata-table]').should('not.be.checked');
    cy.get('[data-cy-open-metadata-table]').click();
    cy.get('[data-cy-open-metadata-table]').should('be.checked');
    cy.get('[data-cy-metadata-table="table"]').should('be.visible');
  });

  it('When checkpont node is selected then metadata can be shown', () => {
    cy.visit('');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-metadata-table="table"]').should('not.exist');
    cy.get('[data-cy-open-metadata-table]').should('not.be.checked');
    cy.get('[data-cy-open-metadata-table]').click();
    cy.get('[data-cy-open-metadata-table]').should('be.checked');
    cy.get('[data-cy-metadata-table="table"]').should('be.visible');
  });

  it('The checkbox for metadata stays consistent with whether the table is actually shown when switching checkpoints', () => {
    cy.visit('');
    cy.createOtherReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.checkFileTreeLength(1);
    // First checkpoint is selected by default, checkbox and table both start out closed.
    cy.get('[data-cy-open-metadata-table]').should('not.be.checked');
    assertMetadataCheckboxConsistentWithTable();
    // Open the metadata table for this checkpoint.
    cy.get('[data-cy-open-metadata-table]').click();
    cy.get('[data-cy-open-metadata-table]').should('be.checked');
    cy.get('[data-cy-metadata-table="table"]').should('exist');
    assertMetadataCheckboxConsistentWithTable();
    // Selecting another checkpoint hides the table again; the checkbox must follow.
    cy.clickEndCheckpointOfThreeNodeReport();
    assertMetadataCheckboxConsistentWithTable();
  });
});

// Checks the invariant that the checkbox is checked if, and only if, the metadata table is shown.
function assertMetadataCheckboxConsistentWithTable() {
  cy.get('[data-cy-open-metadata-table]').then(($checkbox) => {
    const isChecked = $checkbox.is(':checked');
    cy.get('[data-cy-metadata-table="table"]').should(isChecked ? 'exist' : 'not.exist');
  });
}
