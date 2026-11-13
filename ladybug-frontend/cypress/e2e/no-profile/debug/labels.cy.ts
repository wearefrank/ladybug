describe('Test labels', () => {
  before(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('Test label null', () => {
    cy.createReportWithLabelNull();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.checkFileTreeLength(1);
    testTreeView('Message is null');
  });

  it('Test label empty string', () => {
    cy.createReportWithLabelEmpty();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.checkFileTreeLength(1);
    testTreeView('Message is an empty string');
  });

  it('When checkpoint is modified then label Edited is shown', () => {
    cy.createReport();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Hello');
    cy.get(':contains(Edited)').should('not.exist');
    cy.editCheckpointValue('Other value');
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Other');
    cy.get(':contains(Edited)').should('be.visible');
    cy.editCheckpointValue('Hello World!');
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Hello');
    cy.get(':contains(Edited)').should('not.exist');
  });

  it('When report node is modified then label Edited is shown', () => {
    cy.createReport();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="name"]').invoke('val').should('equal', 'Simple report');
    cy.get(':contains(Edited)').should('not.exist');
    cy.get('[data-cy-element-name="name"]').type('{selectAll}My name');
    cy.get(':contains(Edited)').should('be.visible');
    cy.get('[data-cy-element-name="name"]').type('{selectAll}Simple report');
    cy.get(':contains(Edited)').should('not.exist');
  });

  it('When checkpoint node is in read-only storage then read-only label', () => {
    cy.createReport();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Hello');
    cy.get(':contains(Read only)').should('be.visible');
  });

  it('When report node is in read-only storage then read-only label', () => {
    cy.createReport();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.clickRootNodeInFileTree();
    cy.get(':contains(Read only)').should('be.visible');
  });

  it('When report uses LF as newline then initial value not silently modified', () => {
    cy.createReportOnlyLF();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    // Cypress does not give us the newline character. We check the value to see whether we
    // have the right checkpoint.
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Start');
    cy.get(':contains(Edited)').should('not.exist');
    // End checkpoint value is only \n.
    cy.clickEndCheckpointOfThreeNodeReport();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('not.contain', 'Start');
    cy.get(':contains(Edited)').should('not.exist');
  })

  it('When report uses CR as newline then initial value not silently modified', () => {
    cy.createReportOnlyCR();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Start');
    cy.get(':contains(Edited)').should('not.exist');
    // End checkpoint value is only \r.
    cy.clickEndCheckpointOfThreeNodeReport();
    cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('not.contain', 'Start');
    cy.get(':contains(Edited)').should('not.exist');
  })

  describe('With test tab', () => {
    afterEach(() => {
      cy.navigateToTestTabAndAwaitLoadingSpinner();
      cy.get('[data-cy-test="deleteAll"]').click();
      // Prepare for parent after each
      cy.navigateToDebugTabAndAwaitLoadingSpinner();
      cy.get('[data-cy-debug="selectAll"]').click();
      cy.get('[data-cy-debug="openSelected"]').click();
    });

    it('When checkpoint node is in editable storage then no read-only label', () => {
      cy.createReport();
      cy.initializeApp();
      cy.get('[data-cy-debug="selectAll"]').click();
      cy.get('[data-cy-debug="openSelected"]').click();
      cy.copyReportsToTestTab(['Simple report']);
      cy.navigateToTestTabAndAwaitLoadingSpinner();
      cy.get('[data-cy-test="openReport"]').click();
      cy.editCheckpointValue('Other value');
      cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Other');
      cy.get(':contains(Edited)').should('be.visible');
      // This is the key, testing that read only label does not exist.
      // But we check this when the value is properly shown.
      cy.get(':contains(Read only)').should('not.exist');
    });

    it('When report node is in editable storage then no read-only label', () => {
      cy.createReport();
      cy.initializeApp();
      cy.get('[data-cy-debug="selectAll"]').click();
      cy.get('[data-cy-debug="openSelected"]').click();
      cy.copyReportsToTestTab(['Simple report']);
      cy.navigateToTestTabAndAwaitLoadingSpinner();
      cy.get('[data-cy-test="openReport"]').click();
      cy.clickRootNodeInFileTree();
      cy.get('[data-cy-element-name="name"]').type('{selectAll}My name');
      cy.get(':contains(Edited)').should('be.visible');
      // This is the key, testing that read only label does not exist.
      // But we check this when the value is properly shown.
      cy.get(':contains(Read only)').should('not.exist');
    });
  });
});

function testTreeView(reportName: string): void {
  cy.getRootNodeOfDebugTree()
    .find('[data-cy-tree-view="text"]')
    .should('contain', reportName);

  // The icons of all nodes, the report node first.
  cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view="icon"]').as('tree-icons');

  cy.get('@tree-icons').eq(1)
    .should('satisfy', ($el) => {
      const classList = Array.from($el[0].classList);
      return classList.includes('bi') && classList.includes('bi-arrow-bar-right');
    })

  cy.get('@tree-icons').eq(2)
    .should('satisfy', ($el) => {
      const classList = Array.from($el[0].classList);
      return classList.includes('bi') && classList.includes('bi-info-square');
    })

  cy.get('@tree-icons').eq(3)
    .should('satisfy', ($el) => {
      const classList = Array.from($el[0].classList);
      return classList.includes('bi') && classList.includes('bi-arrow-bar-left');
    })
}
