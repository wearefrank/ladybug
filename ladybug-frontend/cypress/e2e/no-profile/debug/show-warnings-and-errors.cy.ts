describe('Tests for showing errors and warnings', () => {
  it('should show errors that occur for the storage of the current view', () => {
    cy.initializeApp();
    const apiCallAlias: string = 'warningsAndErrors';
    cy.intercept({
      method: 'GET',
      hostname: 'localhost',
      url: /\/warningsAndErrors\/*?/g,
    }).as(apiCallAlias);
    cy.get('[data-cy-change-view-dropdown]').select('Low level error demo view');
    cy.wait(`@${apiCallAlias}`);
    cy.get('[data-cy-toast="danger"]').should('contain', 'A simulated error').should('contain', '...');
  });
});
