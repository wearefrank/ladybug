// The span names used by ladybug-otel-test-companion. Span "Otel root" is the parent of the other two.
const ROOT_SPAN_NAME = 'Otel root';
const HELLO_WORLD_SPAN_NAME = 'Otel here, hello world!';
const GOODBYE_WORLD_SPAN_NAME = 'Otel here, goodbye world';

describe('Receive trace from ladybug-otel-test-companion', () => {
  beforeEach(() => {
    cy.clearDebugStore();
    cy.initializeApp();
  });

  for (const protocol of ['http/json', 'http/protobuf'] as const) {
    it(`When trace is sent as ${protocol} in a single request then Ladybug creates report with the span names`, () => {
      cy.runOtelTestCompanion(protocol);
      cy.refreshApp();
      cy.checkDebugTableRowsAre([ROOT_SPAN_NAME]);
      cy.clickRowInTable(0);
      // Report node, start and end of the root span
      cy.getShownNodesOfReportTreeWithText(ROOT_SPAN_NAME).should('have.length', 3);
      cy.getShownNodesOfReportTreeWithText(HELLO_WORLD_SPAN_NAME).should('have.length', 1);
      cy.getShownNodesOfReportTreeWithText(GOODBYE_WORLD_SPAN_NAME).should('have.length', 1);
    });
  }
});
