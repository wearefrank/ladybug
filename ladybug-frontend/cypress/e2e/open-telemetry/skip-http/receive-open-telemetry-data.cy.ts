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
    cy.get('[data-cy-debug-tree="root"]')
      .find('app-tree-icon').eq(0)
      .parent()
      .should('contain.text', 'Root');
    // Parent checkpoint
    cy.get('[data-cy-debug-tree="root"]')
      .find('app-tree-item').eq(0)
      .find('app-tree-item').eq(0)
      .find('app-tree-icon').eq(0)
      .parent()
      .should('contain.text', 'Root')
    // Child checkpoint
    cy.get('[data-cy-debug-tree="root"]')
      .find('app-tree-item').eq(0)
      .find('app-tree-item').eq(0)
      .find('app-tree-item').eq(0)
      .find('app-tree-icon').eq(0)
      .parent()
      .should('contain.text', 'Child')
    // End of parent checkpoint
    cy.get('[data-cy-debug-tree="root"]')
      .find('app-tree-item').eq(0)
      .find('app-tree-item').eq(0)
      .find('app-tree-item').eq(1)
      .find('app-tree-icon').eq(0)
      .parent()
      .should('contain.text', 'Root')
  })
})