import { STORAGE_ID_COLUMN } from '../../../support/e2e';

describe('Report buttons', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

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

  it('When key button above tree is pressed then storage id and checkpoint id shown', () => {
    cy.visit('');
    cy.createReport();
    cy.createReport();
    cy.createReport();
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    // We want a storage id that is different from all checkpoint ids.
    cy.assertDebugTableLength(4)
      .eq(3)
      .find(`td:eq(${STORAGE_ID_COLUMN})`)
      .should('contain.text', '3').click();
    checkNoCheckpointIds();
    cy.get('[data-cy-debug-tree="toggleShowCheckpointId"]').click();
    cy.get('[data-cy-debug-tree="root"]')
      .find(`.item-name:eq(0)`)
      .contains('Simple report (3)');
    for(const nodeIndex of [1, 2]) {
      cy.get('[data-cy-debug-tree="root"]')
        .find(`.item-name:eq(${nodeIndex})`)
        // We test the exact checkpoint id-s. These are deterministic.
        // This check makes sure that the storage id and the checkpoint
        // id are not confused - both are extracted from the uid by
        // http.service.ts.
        .contains(`Simple report (${nodeIndex - 1})`);
    }
    cy.get('[data-cy-debug-tree="toggleShowCheckpointId"]').click();
    checkNoCheckpointIds();
  })

  it('When node is collapsed and expanded then the same node remains selected', () => {
    cy.visit('');
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 3)
      .eq(2)
      .click();
    checkSelectedNode();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.collapseNode('Simple report', 0);
    // Selected node is not visible
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 1);
    cy.expandNode('Simple report', 0);
    checkSelectedNode();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 3);
  })

  it('When all nodes are collapsed and expanded then same node remains selected', () => {
    cy.visit('');
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 3)
      .eq(2)
      .click();
    checkSelectedNode();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.get('[data-cy-debug-tree="collapseAll"]').click();
    // Selected node is not visible
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 1);
    cy.get('[data-cy-debug-tree="expandAll"]').click();
    checkSelectedNode();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Goodbye');
    cy.getShownNodesOfReportTreeWithText('Simple report').should('have.length', 3);
  })

  it('When node meets search key then red', () => {
    cy.visit('');
    cy.createReportWithStatusError();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1).click();
    cy.getShownNodesOfReportTreeWithText('Complex').should('have.length', 4);
    cy.getShownNodesOfReportTreeWithText('First').should('have.length', 6);
    cy.getShownNodesOfReportTreeWithText('Second').should('have.length', 1);
    cy.checkShownNodeWithTextSearched('Complex', false);
    cy.checkShownNodeWithTextSearched('First', false);
    cy.checkShownNodeWithTextSearched('Second', false);
    cy.get('[data-cy-debug-tree="search"]').type('Complex');
    cy.checkShownNodeWithTextSearched('Complex', true);
    cy.checkShownNodeWithTextSearched('First', false);
    cy.checkShownNodeWithTextSearched('Second', false);
  })
});

function checkNoCheckpointIds() {
  for(const nodeIndex of [0, 1, 2]) {
    cy.get('[data-cy-debug-tree="root"]')
      .find(`.item-name:eq(${nodeIndex})`)
      .invoke('text')
      .should('contain', 'Simple report')
      .should('not.contain', '(');
  }
}

function checkSelectedNode() {
  cy.checkShownNodeWithTextSelected('Simple report', 0, false);
  cy.checkShownNodeWithTextSelected('Simple report', 1, false);
  cy.checkShownNodeWithTextSelected('Simple report', 2, true);
}