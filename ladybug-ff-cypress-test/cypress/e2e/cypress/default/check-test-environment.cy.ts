describe('Check test environment WITHOUT test webapp', () => {
  it('Check that the extra view defined in ladybug-ff-test-webapp DOES NOT exist', () => {
    cy.visit('/iaf/ladybug')
    cy.get('[data-cy-change-view-dropdown]').contains('White box').should('exist')
    cy.get('[data-cy-change-view-dropdown]').contains('White box view no name no input').should('not.exist')
    cy.waitForVideo()
  })
})
