describe('Receive open telemetry data', () => {
  beforeEach(() => {
    cy.clearDebugStore();
    cy.initializeApp();
  });

  it('When data is received then Ladybug creates report in debug tab', () => {
    cy.createOpenTelemetryReport();
    cy.refreshApp();
    cy.checkDebugTableRowsAre(['Root']);
    cy.clickRowInTable(0);
    cy.clickRowInTable(0);
    // Report node
    cy.getRootNodeOfDebugTree()
      .should('contain.text', 'Root');
    // Parent checkpoint
    cy.getRootNodeOfDebugTree()
      .getChildNodesInDebugTree().eq(0)
      .should('contain.text', 'Root')
    // Child checkpoint
    cy.getRootNodeOfDebugTree()
      .getChildNodesInDebugTree().eq(0)
      .getChildNodesInDebugTree().eq(0)
      .should('contain.text', 'Child')
    // End of parent checkpoint
    cy.getRootNodeOfDebugTree()
      .getChildNodesInDebugTree().eq(0)
      .getChildNodesInDebugTree().eq(1)
      .should('contain.text', 'Root')
  })
})