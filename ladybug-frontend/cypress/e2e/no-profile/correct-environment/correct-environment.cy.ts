describe('Correct environment', () => {
  it('Running test webapp does not have optional Ladybug dependencies', () => {
    cy.checkRunningBackendOneOf(['ladybug-test-webapp', 'ladybug-spring-boot-test-webapp']);
  })
})