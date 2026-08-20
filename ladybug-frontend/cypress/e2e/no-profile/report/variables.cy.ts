describe('Variables', () => {
  beforeEach(() => {
    cy.resetApp();
    cy.createReport();
    cy.initializeApp();
  });

  afterEach(() => {
    cy.resetApp();
    cy.initializeApp();
  });

  it('When a report has variables then they are read and shown correctly', () => {
    cy.copyReportsToTestTab(['Simple report']);
    cy.navigateToTestTabAndAwaitLoadingSpinner();
    openTheReport();
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="variableName"]').should('have.length', 1);
    cy.get('[data-cy-element-name="variableValue"]').should('have.length', 1);
    cy.get('[data-cy-element-name="variableName"]').invoke('val').should('have.length', 0);
    cy.get('[data-cy-element-name="variableName"]').type('A variable');
    cy.get('[data-cy-element-name="variableValue"]:eq(0)').invoke('val').should('have.length', 0);
    cy.get('[data-cy-element-name="variableValue"]:eq(0)').type('A value');
    cy.get('[data-cy-element-name="variableValue"]').should('have.length', 2);
    cy.get('[data-cy-report="save"]').click();
    cy.get('[data-cy-difference-modal="confirm"]').click();
    cy.get('[data-cy-debug-tree="close"]').click();
    openTheReport();
    cy.clickRootNodeInFileTree();
    cy.get('[data-cy-element-name="variableName"]').should('have.length', 2);
    cy.get('[data-cy-element-name="variableValue"]').should('have.length', 2);
    cy.get('[data-cy-element-name="variableName"]:eq(0)').invoke('val').should('contain', 'A variable');
    cy.get('[data-cy-element-name="variableValue"]:eq(0)').invoke('val').should('contain', 'A value');
    cy.get('[data-cy-element-name="variableValue"]:eq(1)').invoke('val').should('have.length', 0);
    cy.get('[data-cy-element-name="variableValue"]:eq(1)').invoke('val').should('have.length', 0);
  })

  function openTheReport() {
    cy.wait(200);
    cy.get('[data-cy-test="openReport"]').should('have.length', 1).click();
    cy.wait(200);
    cy.checkFileTreeLength(1);
  }
})