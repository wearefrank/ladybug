URL = 'http://localhost:4200'

describe('Changing the table size', function () {
  it('Typing in a table size', function () {
    cy.visit(URL);
    cy.get('#displayAmount').type(5)
    cy.get('.table-responsive tbody').find('tr').should('have.length', 5)
  })

  it('Remove table size', function () {
    cy.get('#displayAmount').clear()
    cy.get('.table-responsive tbody').find('tr').should('have.length', 0)
  })

  it('Retype larger table size', function () {
    cy.get('#displayAmount').type(10)
    cy.get('.table-responsive tbody').find('tr').should('have.length', 10)
  })
})
