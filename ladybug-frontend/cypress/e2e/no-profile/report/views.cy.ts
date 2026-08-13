import { indexOf } from "cypress/types/lodash";

describe('Views', () => {
  beforeEach(() => {
    cy.resetApp();
    // Has checkpoints that are hidden in the blackbox view.
    // Also has checkpoints other than the first and the last
    // that are NOT to be hidden in the black box view.
    // The black box view is not the black box view of the FF!
    // but the black box view of ladybug-test-webapp.
    cy.createReportWithInfopoint();
    cy.createReportInDatabaseStorage();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('When the black box view is selected then the view determines which checkpoints are shown', () => {
    // Check that all checkpoints are there in the white box view.
    cy.getDebugTableRows().should('have.length', 1);
    cy.clickRowInTable(0);
    cy.get('[data-cy-debug-tree="expandAll"]').click();
    // Expect one open report
    cy.checkFileTreeLength(1);
    checkAllCheckpointsAreShown();
    const indexOfBlackBoxView = 1;
    changeView('Black box', indexOfBlackBoxView);
    // TODO issue https://github.com/wearefrank/ladybug/issues/802
    // When you select the view, it should not be necessary to reopen
    // the report before it has effect.
    cy.clickRowInTable(0);
    checkOnlyCheckpointsOfBlackBoxViewAreShown();
  })

  it('When view is changed then debug table is updated', () => {
    const textWhiteBoxRecord: string = 'Hide a checkpoint in blackbox view';
    const textDatabaseRecord: string = 'Report for database storage';
    cy.get('[data-cy-change-view-dropdown] :selected').should('contain.text', "White box");
    cy.getDebugTableRows().should('have.length', 1).find(`:contains(${textWhiteBoxRecord})`);
    cy.getDebugTableRows().find(`:contains(${textDatabaseRecord})`).should('not.exist');
    changeView('Database storage', 4);
    cy.get('[data-cy-change-view-dropdown] :selected').should('contain.text', "Database storage");
    cy.getDebugTableRows().find(`:contains(${textWhiteBoxRecord})`).should('not.exist');
    cy.getDebugTableRows().find(`:contains(${textDatabaseRecord})`).should('have.length', 1);
  })
})

function checkAllCheckpointsAreShown() {
  // Report root, first checkpoint and last checkpoint  
  checkTreeNodeWithTextOccurs('Hide a checkpoint in blackbox view', 3);
  checkTreeNodeWithTextOccurs('Hide this checkpoint', 1);
  checkTreeNodeWithTextOccurs('This checkpoint should be visible', 1);
}

function checkOnlyCheckpointsOfBlackBoxViewAreShown() {
  // Report root, first checkpoint and last checkpoint  
  checkTreeNodeWithTextOccurs('Hide a checkpoint in blackbox view', 3);
  checkTreeNodeWithTextOccurs('Hide this checkpoint', 0);
  checkTreeNodeWithTextOccurs('This checkpoint should be visible', 1);
}

function checkTreeNodeWithTextOccurs(text: string, amount: number) {
  cy.getShownNodesOfReportTreeWithText(text).should('have.length', amount);
}

// For some reason, the wrong view gets selected when you query by name.
// If the view ever gets another place in the list, check out the new
// index and update the expectedAtIndex passed to this method.
function changeView(name: string, expectedAtIndex: number) {
  cy.get('[data-cy-change-view-dropdown]').find(`option:contains(${name})`).invoke('val').should('equal', `${expectedAtIndex}`);
  cy.get('[data-cy-change-view-dropdown]').select(expectedAtIndex);
}