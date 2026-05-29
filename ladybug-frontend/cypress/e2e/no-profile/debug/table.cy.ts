describe('Tests for Debug tab table', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('Should sort by column naming when clicking on table column header', () => {
    cy.getDebugTableRows().first().contains("Simple report");
    cy.get('[data-cy-debug="metadataLabel"]').eq(3).click();
    cy.getDebugTableRows().first().contains("Another simple report");
    cy.get('[data-cy-debug="metadataLabel"]').eq(3).click();
    cy.getDebugTableRows().first().contains("Simple report");
  });
});

describe('Styling', () => {
  before(() => {
    cy.resetApp();
    cy.createReport();
    cy.createReportWithStatusError();
    cy.initializeApp();
  });

  after(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('When status is success then the row and all of its cells have class statusSuccess', () => {
    cy.visit('');
    cy.checkDebugTableRowsAre(['Simple report', 'Complex error report']);
    checkRowStatus(0, 'statusSuccess');
  })

  it('When status is error then the row and all of its cells have class statusError', () => {
    cy.visit('');
    cy.checkDebugTableRowsAre(['Simple report', 'Complex error report']);
    checkRowStatus(1, 'statusError');
  })
})

function checkRowStatus(index: number, status: string) {
  const ARBITRARY_COLUMN = 1;
  cy.getDebugTableRows()
    .eq(index)
    .should('have.class', status)
    .find(`td:eq(${ARBITRARY_COLUMN})`)
    .should('have.class', status);
}