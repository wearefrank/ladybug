
// This test should be run as the first. Other tests
// have to delete all reports from the test tab and
// would remove pre-existing reports.
//
// We test here that there are no predefined reports
//
describe('Test that should be done before all other tests', () => {
  beforeEach(() => {
    cy.initializeApp();
    cy.navigateToTestTabAndAwaitLoadingSpinner();
  })

  it('Report in src/test/testtool should not be shown', () => {
    // We cannot search for 'Pre existing report' within the
    // test rows when there are not table rows at all.
    cy.getTestTableRows().should('not.exist')
  })
})
