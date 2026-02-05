describe('Stub strategy', () => {
  before(() => {
    cy.resetApp();
  });

  beforeEach(() => {
    cy.createReport();
    cy.initializeApp();
    cy.copyReportsToTestTab(['Simple report']);
    cy.navigateToTestTabAndAwaitLoadingSpinner();
    cy.get('[data-cy-test="openReport"]').click();
  });

  afterEach(() => {
    cy.resetApp();
  });

  it('When checkpoint level stub strategy is edited then edit can be saved', () => {
    cy.get('[data-cy-checkpoint-stub-strategy]').find(':selected').invoke('text').should('equal', 'Use report level stub strategy');
    cy.get('[data-cy-checkpoint-stub-strategy]').select('Always stub this checkpoint');
    cy.get('[data-cy-report="save"]').click();
    cy.get(':contains(Use report level stub strategy)').should('be.visible');
    cy.get(':contains(Always stub this checkpoint)');
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-checkpoint-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always stub this checkpoint');
  });

  it('When report level stub strategy is edited then edit can be saved', () => {
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Stub all external connection code');
    cy.get('[data-cy-report-stub-strategy]').select('Always');
    cy.get('[data-cy-report="save"]').click();
    cy.get(':contains(Stub all external connection code)').should('be.visible');
    cy.get(':contains(Always)');
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
  });

  it('When report node is selected then no checkpoint stub strategy visible', () => {
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-checkpoint-stub-strategy]').should('not.exist');
  });

  it('When report node is selected then report level stub strategy can be edited', () => {
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Stub all external connection code');
    cy.get('[data-cy-report-stub-strategy]').select('Always');
    cy.get('[data-cy-report="save"]').click();
    cy.get(':contains(Stub all external connection code)').should('be.visible');
    cy.get(':contains(Always)');
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-report-stub-strategy]').find(':selected').invoke('text').should('equal', 'Always');
  });
});
