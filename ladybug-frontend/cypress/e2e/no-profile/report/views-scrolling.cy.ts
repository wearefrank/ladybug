const DEBUG_TREE = '[data-cy-debug-tree="root"]';
const VISIBLE_CHECKPOINT = 'This checkpoint should be visible';
const HIDDEN_CHECKPOINT = 'Hide this checkpoint';
// Where the selected checkpoint is put, in pixels below the top of the
// visible part of the debug tree.
const OFFSET = 100;
// Rounding of fractional pixels may cause small differences.
const TOLERANCE = 1;

describe('Scrolling of the debug tree when the view changes', () => {
  beforeEach(() => {
    cy.resetApp();
    cy.createReportWithManyHiddenCheckpoints();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('keeps the selected checkpoint at the same height when the view hides or shows checkpoints above it', () => {
    cy.getDebugTableRows().should('have.length', 1);
    cy.clickRowInTable(0);
    cy.getShownNodesOfReportTreeWithText(HIDDEN_CHECKPOINT).should('have.length', 300);
    cy.getShownNodesOfReportTreeWithText(VISIBLE_CHECKPOINT).should('have.length', 1).selectTreeNode();
    cy.checkShownNodeWithTextSelected(VISIBLE_CHECKPOINT, 0, true);
    // The checkpoint is about 150 rows below the top of the tree, so the
    // tree is scrolled down.
    cy.get(DEBUG_TREE).then((tree) => {
      tree[0].scrollTop += offsetOfVisibleCheckpoint(tree[0]) - OFFSET;
    });
    cy.get(DEBUG_TREE).invoke('scrollTop').should('be.greaterThan', 0);
    visibleCheckpointShouldBeAt(OFFSET);

    // In the black box view the report has only four nodes. They fit in
    // the visible part, so the tree cannot scroll and the checkpoint
    // cannot stay at OFFSET; it is shown where it is without scrolling.
    changeView('Black box', 1);
    cy.getShownNodesOfReportTreeWithText(HIDDEN_CHECKPOINT).should('have.length', 0);
    cy.checkShownNodeWithTextSelected(VISIBLE_CHECKPOINT, 0, true);
    cy.get(DEBUG_TREE).should((tree) => {
      expect(tree[0].scrollTop).to.equal(0);
      const offset = offsetOfVisibleCheckpoint(tree[0]);
      expect(offset).to.be.within(-TOLERANCE, tree[0].clientHeight + TOLERANCE);
    });

    // The hidden checkpoints come back above the selected checkpoint,
    // which stays where it is.
    cy.get(DEBUG_TREE).then((tree) => {
      const offsetInBlackBox = offsetOfVisibleCheckpoint(tree[0]);
      changeView('White box', 2);
      cy.getShownNodesOfReportTreeWithText(HIDDEN_CHECKPOINT).should('have.length', 300);
      cy.checkShownNodeWithTextSelected(VISIBLE_CHECKPOINT, 0, true);
      cy.get(DEBUG_TREE).invoke('scrollTop').should('be.greaterThan', 0);
      visibleCheckpointShouldBeAt(offsetInBlackBox);
    });
  });
});

// Retries until the checkpoint that all views show is at the expected
// distance from the top of the visible part of the debug tree.
function visibleCheckpointShouldBeAt(expected: number): void {
  cy.get(DEBUG_TREE).should((tree) => {
    expect(Math.abs(offsetOfVisibleCheckpoint(tree[0]) - expected)).to.be.at.most(TOLERANCE);
  });
}

// Distance from the top of the visible part of the debug tree to the
// top of the checkpoint that all views show.
function offsetOfVisibleCheckpoint(tree: HTMLElement): number {
  const nodes = Array.from(tree.querySelectorAll('[data-cy-tree-view-node]')).filter((node) =>
    node.querySelector('[data-cy-tree-view-text]')?.textContent?.includes(VISIBLE_CHECKPOINT),
  );
  expect(nodes).to.have.length(1);
  return nodes[0].getBoundingClientRect().top - tree.getBoundingClientRect().top;
}

// See views.cy.ts: selecting by name may select the wrong view, so the
// index of the view is checked.
function changeView(name: string, expectedAtIndex: number): void {
  cy.get('[data-cy-change-view-dropdown]')
    .find(`option:contains(${name})`)
    .invoke('val')
    .should('equal', `${expectedAtIndex}`);
  cy.get('[data-cy-change-view-dropdown]').select(expectedAtIndex);
}
