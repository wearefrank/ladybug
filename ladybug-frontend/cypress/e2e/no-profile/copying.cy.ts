describe('Tests about copying', () => {
  before(() => {
    cy.resetApp();
  });

  beforeEach(() => cy.initializeApp());

  afterEach(() => {
    cy.resetApp();
  });

  it('Copy report to test tab', () => {
    cy.get('[data-cy-nav-tab="testTab"]').click();
    cy.checkTestTableNumRows(0);
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.debugTreeGuardedCopyReport('Simple report', 3, '');
    cy.get('[data-cy-nav-tab="testTab"]').click();
    // We test that the user does not have to refresh here.
    cy.checkTestTableReportsAre(['Simple report']);
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(1);
    cy.clickRowInTable(0);
    cy.checkFileTreeLength(1);
    cy.get('[data-cy-nav-tab="testTab"]').click();
    // Do not refresh. The test tab should have saved its state.
    cy.checkTestTableReportsAre(['Simple report']);
  });

  it('When we edit a checkpoint then we can use toasts to copy to test tab and enter test tab', () => {
    cy.get('[data-cy-nav-tab="testTab"]').click();
    cy.checkTestTableNumRows(0);
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.wait(200);
    cy.editCheckpointValue('Other value');
    cy.get('[data-cy-toast="warning"]').invoke('text').should('contain', 'test tab');
    cy.get('[data-cy-toast="callback"]').click();
    cy.get('[data-cy-toast="success"] button:contains(Go to test tab)').as('toTestTab').should('have.length', 1);
    cy.get('@toTestTab').click();
    cy.checkTestTableNumRows(1);
  });

  it('When we rerun an edited checkpoint then we can use toasts to copy to test tab and enter test tab', () => {
    cy.get('[data-cy-nav-tab="testTab"]').click();
    cy.checkTestTableNumRows(0);
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.wait(200);
    cy.editCheckpointValue('Other value');
    cy.get('[data-cy-toast="warning"]').invoke('text').should('contain', 'test tab');
    // Warning after starting editing should go away
    cy.get('[data-cy-toast="warning"]', {timeout: 10000}).should('not.exist');
    cy.get('[data-cy-report="rerun"]').click();
    cy.get('[data-cy-report-buttons="rerunResult"]').invoke('text').should('contain', 'checkpoints');
    cy.get('[data-cy-toast="warning"]').invoke('text').should('contain', 'test tab');
    cy.get('[data-cy-toast="callback"]').click();
    cy.get('[data-cy-toast="success"] button:contains(Go to test tab)').as('toTestTab').should('have.length', 1);
    cy.get('@toTestTab').click();
    cy.checkTestTableNumRows(1);
    // Test that the new report in the debug is shown without the need to refresh
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(2);
  });

  it('When we rerun a pristine checkpoint then no toast is shown', () => {
    cy.get('[data-cy-nav-tab="testTab"]').click();
    cy.checkTestTableNumRows(0);
    cy.get('[data-cy-nav-tab="debugTab"]').click();
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.wait(200);
    cy.get('[data-cy-report="rerun"]').click();
    cy.get('[data-cy-report-buttons="rerunResult"]').invoke('text').should('contain', 'checkpoints');
    cy.wait(200);
    cy.get('[data-cy-toast="warning"]').should('not.exist');
    // Test that the new report is shown without the need to refresh.
    cy.assertDebugTableLength(2);
  });
});
