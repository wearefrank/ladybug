describe('Tests for table filter', () => {
  before(() => cy.resetApp());

  beforeEach(() => {
    cy.createReport();
    cy.createOtherReport();
    cy.initializeApp();
  });

  afterEach(() => cy.resetApp());

  it('Should change table size when changing display amount', () => {
    // We only assume here that the default is two or more.
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="displayAmount"]').type('{selectAll}{del}{enter}');
    cy.assertDebugTableLength(0);
    // From now on, we type one character at a time. Cypress can type very rapidly.
    // We do not expect our app to catch up without guards.
    cy.get('[data-cy-debug="displayAmount"]').type('{selectAll}1{enter}');
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="displayAmount"]').type('{backspace}{enter}');
    cy.assertDebugTableLength(0);
    cy.get('[data-cy-debug="displayAmount"]').type('{selectAll}2{enter}');
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="displayAmount"]').type('{backspace}{enter}');
    cy.assertDebugTableLength(0);
    cy.get('[data-cy-debug="displayAmount"]').type('{selectAll}9{enter}');
    cy.assertDebugTableLength(2);
  });

  it('Should show filter side drawer when clicking the filter button', () => {
    cy.get('[data-cy-debug="filter-side-drawer"]').should('not.exist');
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="filter-side-drawer"]').should('be.visible');
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="filter-side-drawer"]').should('not.exist');
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="filter-side-drawer"]').should('be.visible');
    cy.get('[data-cy-debug="close-filter-btn"]').click();
    cy.get('[data-cy-debug="filter-side-drawer"]').should('not.exist');
  });

  it('Should limit viewed records in table when entering value in filter parameter', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('(Simple report){enter}');
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="tableFilter"]').eq(3).clear().type('{enter}');
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('(Simple report){enter}');
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="clear-filter-btn"').click();
    cy.assertDebugTableLength(2);
  });

  it('Should show autocomplete options when partially filling entering the filter parameter', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('test');
    cy.get('[data-cy-debug="matAutocompleteOption"]').should('be.visible');
    cy.assertDebugTableLength(2);
  });

  //Disabled because form validation has been removed for filter fields
  xit('Should display error when entering wrong type for filter header', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(0).type('test');
    cy.get('[data-cy-debug="filter-error-message"]').should('be.visible');
    cy.contains('Filter Error: Search value \'test\' is not a valid \'number\' ');
    cy.get('[data-cy-debug="filter"]').click();
  });

  it('Should update table when choosing the autocomplete options', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('test');
    cy.get('[data-cy-debug="matAutocompleteOption"]').first().click();
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('test');
    cy.get('[data-cy-debug="filterClearButton"]').eq(3).click();
    cy.assertDebugTableLength(2);
    cy.get('[data-cy-debug="filter"]').click();
  });

  it('Should show Simple report when using a wildcard with the input in the filter', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('Simple*');
    cy.assertDebugTableLength(1);
    cy.get('[data-cy-debug="clear-filter-btn"]').click()
    cy.get('[data-cy-debug="tableFilter"]').eq(3).type('*Simple*');
    cy.assertDebugTableLength(2);
  });

  it('Should convert filter to new values when a different view is selected with different columns', () => {
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"]').eq(1).type('1{enter}');
    cy.get('[data-cy-change-view-dropdown]').select('Report name only');
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="filterLabel"]').eq(1).should('not.contain', 'Endtime');
    cy.get('[data-cy-change-view-dropdown]').select('White box');
    cy.get('[data-cy-debug="filter"]').click();
    cy.get('[data-cy-debug="tableFilter"').eq(1).should('not.contain.value', '1')
  })
});
