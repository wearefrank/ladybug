describe('Refresh', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.initializeApp();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('New reports are only shown on refresh', () => {
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="amountShown"]').invoke('text').should('contain', '1');
  });
});
