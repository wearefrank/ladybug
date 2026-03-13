describe('Test the Test tab', () => {
  before(() => {
    cy.resetApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
    cy.copyReportsToTestTab(['Simple report', 'Another simple report'])
    cy.navigateToTestTabAndAwaitLoadingSpinner();
  });

  afterEach(() => cy.resetApp());

  it('should show storage ids in table when setting is enabled', () => {
    // Default is that storag ids are not shown.
    cy.get('[data-cy-test="showHideStorageIds"]').as('showHideStorageIds').contains('Show storage ids');
    cy.get('[data-cy-test-table="storageId"]').should('not.exist');
    cy.get('@showHideStorageIds').click();
    cy.get('@showHideStorageIds').contains('Hide storage ids');
    cy.get('[data-cy-test-table="storageId"]').should('be.visible');
    cy.get('@showHideStorageIds').click();
    cy.get('@showHideStorageIds').contains('Show storage ids');
    cy.get('[data-cy-test-table="storageId"]').should('not.exist');
  });

  it('Should delete one report at a time with deleteSelected button', () => {
    cy.getTestTableRows().contains('Another simple report').parent('tr').find('[data-cy-test="selectOne"]').check();
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
    cy.checkTestTableReportsAre(['Simple report']);
    cy.getTestTableRows().contains('Simple report').parent('tr').find('[data-cy-test="selectOne"]').check();
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
  });

  it('Should delete all tests with deleteSelected button', () => {
    cy.get('[data-cy-test="toggleSelectAll"]').check();
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
    cy.checkTestTableNumRows(0);
  });

  it('Should not open delete modal when clicking on deleteSelected button and there are no tests selected', () => {
    cy.get('[data-cy-test="toggleSelectAll"]').uncheck()
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('not.exist');
    cy.checkTestTableNumRows(2);
    cy.get('[data-cy-test="toggleSelectAll"]').check();
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
  });

  it('Should delete all tests with deleteAll button', () => {
    cy.checkTestTableNumRows(2);
    cy.get('[data-cy-test="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
    cy.checkTestTableNumRows(0);
  });

  it('Should not open delete modal when there are no tests', () => {
    cy.get('[data-cy-test="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('exist').click();
    cy.get('[data-cy-test="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('not.exist');
    cy.get('[data-cy-test="deleteSelected"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('not.exist');
  });

  it('should keep rerun results after switching tabs', () => {
    cy.get('[data-cy-test="runReport"]').eq(0).click();
    cy.get('[data-cy-test="runResult"]').should('be.visible');
    cy.get('[data-cy-test="compareReport"]').eq(0).click();
    cy.get('[data-cy-nav-tab="testTab"]').click();
    cy.get('[data-cy-test="runResult"]').should('be.visible');
  });
});
