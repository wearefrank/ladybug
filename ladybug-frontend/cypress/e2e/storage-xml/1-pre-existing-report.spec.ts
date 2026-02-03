// This test should be run as the first. Other tests
// have to delete all reports from the test tab and
// would remove pre-existing reports.
//
describe('Pre existing report', () => {
  beforeEach(() => {
    cy.initializeApp();
    cy.navigateToTestTabAndAwaitLoadingSpinner();
  })

  it('Report present in src/test/testtool should be shown', () => {
    cy.getTestTableRows().contains('Pre existing report').should('have.length', 1)
  })
})
