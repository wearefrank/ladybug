// init.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test

describe('Ladybug simple protractor test', function() {
    it('Confirm title of ladybug app', function() {
        cy.visit('http://localhost:8000');
        cy.title().should('eq', 'Ladybug');
    });
});


describe('Go through each of the tabs', function () {

    beforeEach(() => {
        cy.visit('http://localhost:8000');
    })

    it('Click on the Test tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view2';
        cy.get('#view2').click()
        cy.url().should('eq', expectedUrl)
    });

    it('Click on the Compare tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view3';
        cy.get('#view3').click()
        cy.url().should('eq', expectedUrl)
    });

    it('Click on the Debug tab', function () {
        let expectedUrl = 'http://localhost:8000/#!/view1';
        cy.get('#view1').click()
        cy.url().should('eq', expectedUrl)
    });
})

describe('Write something', function () {
    it('Should write something', function () {
        cy.visit('http://localhost:8000');
        cy.get('#view2').click()
        cy.get('#moveToInput').type('Hello World');
        cy.get('#moveToInput').should('have.value', 'Hello World');
    })
})