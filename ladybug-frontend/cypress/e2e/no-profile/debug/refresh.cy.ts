describe('Refresh', () => {
  before(() => {
    cy.resetApp();
  });

  beforeEach(() => cy.initializeApp());

  afterEach(() => cy.resetApp());

  it('New reports are only shown on refresh', () => {
    cy.assertDebugTableLength(0);
    cy.createReport();
    cy.get('[data-cy-debug="refresh"]').click();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="amountShown"]').invoke('text').should('contain', '1');
  });
});
