describe('Correct environment', () => {
  it('Running test webapp has optional Ladybug dependencies', () => {
    cy.checkRunningBackendOneOf(['ladybug-test-webapp-otel', 'ladybug-spring-boot-test-webapp-otel']);
  })
})