describe('Stub strategy null', () => {
  beforeEach(() => {
    cy.resetApp();
    cy.createReportWithStubStrategyNull();
    cy.initializeApp();
    cy.checkDebugTableRowsAre(['Report without stub strategy and without link method']);
    cy.copyReportsToTestTab(['Report without stub strategy and without link method']);
    cy.navigateToTestTabAndAwaitLoadingSpinner();
  });

  after(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('When stub strategy is null then stub strategy can still be edited', () => {
    cy.checkTestTableReportsAre(['Report without stub strategy and without link method']);
    cy.get('[data-cy-test="openReport"]').click();
    // Assume we are in first child
    cy.get('[data-cy-report-stub-strategy]').find(':selected').should('not.exist');
    cy.get('[data-cy-report-stub-strategy]').select('Always');
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
    cy.get('[data-cy-report="save"]').click();
    cy.get(':contains(null)').should('be.visible');
    cy.get(':contains(Always)').should('be.visible');
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
  })

  it('When stub strategy of root node is null then stub strategy can still be edited', () => {
    cy.checkTestTableReportsAre(['Report without stub strategy and without link method']);
    cy.get('[data-cy-test="openReport"]').click();
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').should('not.exist');
    cy.get('[data-cy-report-stub-strategy]').select('Always');
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
    cy.get('[data-cy-report="save"]').click();
    cy.get(':contains(null)').should('be.visible');
    cy.get(':contains(Always)').should('be.visible');
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
  })
})