describe('About deleting reports', () => {
  beforeEach(() => {
    cy.resetApp()
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
    cy.clearDatabaseStorage();
    cy.get('[data-cy-change-view-dropdown]').select('White box');
  });

  afterEach(() => {
    cy.get('[data-cy-change-view-dropdown]').select('White box');
    cy.resetApp()
  });

  it('Should change view and delete a single report with the deleteSelected button', () => {
    cy.get('[data-cy-change-view-dropdown]').select('Database storage');
    cy.createReportInDatabaseStorage();
    cy.refreshApp();
    cy.assertDebugTableLength(1);
    cy.createReportInDatabaseStorage();
    cy.refreshApp();
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="selectOne"]').eq(0).click();
    cy.get('[data-cy-debug="deleteSelected"]').click();
    cy.assertDebugTableLength(1);
  });

  it ('Should delete all reports with the deleteAll button', () => {
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').click();
    cy.assertDebugTableLength(0);
  });

  it ('Should not open the delete modal when there are no reports', () => {
    cy.get('[data-cy-debug="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('exist').click();
    cy.get('[data-cy-debug="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('not.exist');
    cy.get('[data-cy-debug="deleteAll"]').click();
    cy.get('[data-cy-delete-modal="confirm"]').should('not.exist');
  });

  it('Delete button should be absent for log storage, present for database storage', () => {
    cy.get('[data-cy-change-view-dropdown]').select('Database storage');
    cy.get('[data-cy-debug="deleteSelected"]').should('be.visible');
    cy.get('[data-cy-change-view-dropdown]').select('White box');
    cy.get('[data-cy-debug="deleteSelected"]').should('not.exist');
  })
});
