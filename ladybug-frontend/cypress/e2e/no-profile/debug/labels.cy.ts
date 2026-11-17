// TODO issue https://github.com/wearefrank/ladybug/issues/1134.
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
    testTreeView('Message is null', 'Null String');
    testMessageLabelOfInfopoint('Null String', 'Message is null', 'Message is empty string');
  });

  it('Test label empty string', () => {
    cy.createReportWithLabelEmpty();
    cy.initializeApp();
    cy.get('[data-cy-debug="selectAll"]').click();
    cy.get('[data-cy-debug="openSelected"]').click();
    cy.checkFileTreeLength(1);
    testTreeView('Message is an empty string', 'Empty String');
    testMessageLabelOfInfopoint('Empty String', 'Message is empty string', 'Message is null');
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

function testTreeView(reportName: string, infopointName: string): void {
  cy.getRootNodeOfDebugTree()
    .find('[data-cy-tree-view-text]')
    .should('contain', reportName);
  cy.getShownNodesOfReportTreeWithText(infopointName).should('have.length', 1);

  // The types of all nodes in the order in which they are shown, the report
  // node first. Which icon belongs to a type is tested by Karma.
  cy.get('[data-cy-debug-tree="root"] [data-cy-tree-view-type]').as('tree-types');
  cy.get('@tree-types').eq(1).should('have.attr', 'data-cy-tree-view-type', 'startpoint');
  cy.get('@tree-types').eq(2).should('have.attr', 'data-cy-tree-view-type', 'infopoint');
  cy.get('@tree-types').eq(3).should('have.attr', 'data-cy-tree-view-type', 'endpoint');
}

// The startpoint is selected when the report is opened. Its message is not null or empty,
// so there are no labels about that. Then select the infopoint, whose message is null or the
// empty string, and check that the right label is shown above the checkpoint value.
function testMessageLabelOfInfopoint(infopointName: string, expectedLabel: string, otherLabel: string): void {
  cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('contain', 'Hello');
  cy.get(`[data-cy-report="alert-messages"] :contains(${expectedLabel})`).should('not.exist');
  cy.get(`[data-cy-report="alert-messages"] :contains(${otherLabel})`).should('not.exist');
  cy.getShownNodesOfReportTreeWithText(infopointName).should('have.length', 1).selectTreeNode();
  cy.get('[data-cy-element-name="checkpointEditor"]').invoke('text').should('not.contain', 'Hello');
  cy.get(`[data-cy-report="alert-messages"] :contains(${expectedLabel})`).should('be.visible');
  cy.get(`[data-cy-report="alert-messages"] :contains(${otherLabel})`).should('not.exist');
}
