const NUM_COLUMNS_WHITE_BOX_VIEW = 10

describe('Tests with views and filtering', () => {
  before(() => {
    cy.apiDeleteAll(Cypress.env('debugStorageName') as string)
    cy.apiDeleteAll('Test')
    cy.visit('')
    cy.createReportWithTestPipelineApi('Example1a', 'Adapter1a', 'xxx')
    // Waits are to prevent all reports to be created in the same second.
    // If that would happen, filtering on end time would not reduce the
    // number of shown report.
    cy.wait(300)
    cy.createReportWithTestPipelineApi('Example1b', 'Adapter1b', 'xxx')
    cy.wait(300)
    cy.createReportWithTestPipelineApi('Example1c', 'Adapter1c', 'xxxyyy')
    cy.wait(300)
    cy.createReportWithTestPipelineApi('UseToStreamPipe', 'UseToStreamPipeChar', 'yyy')
    cy.wait(300)
    cy.createReportWithTestPipelineApi('UseToStreamPipe', 'UseToStreamPipeBin', 'yyy')
    cy.wait(300)
    cy.getNumLadybugReports().should('equal', 5)
  })

  interface ColumnAndName {
    readonly name: string
    readonly colNr: number
    readonly labelFilterPanel: string
    // TODO: Get rid of this when issue https://github.com/wearefrank/ladybug/issues/429 is fixed
    readonly enabled: boolean
  }

  const isDatabaseStorage: boolean = Cypress.env('debugStorageName') === 'DatabaseDebugStorage'

  const columnAndNameCombinations: ColumnAndName[] = [
    { name: 'Storage Id', colNr: 1, labelFilterPanel: 'Storageid', enabled: !isDatabaseStorage },
    { name: 'End time', colNr: 2, labelFilterPanel: 'Endtime', enabled: !isDatabaseStorage },
    { name: 'Duration', colNr: 3, labelFilterPanel: 'Duration', enabled: true },
    { name: 'Name', colNr: 4, labelFilterPanel: 'Name', enabled: true },
    { name: 'Correlation Id', colNr: 5, labelFilterPanel: 'Correlationid', enabled: true },
    { name: 'Status', colNr: 6, labelFilterPanel: 'Status', enabled: true },
    { name: 'Checkpoints', colNr: 7, labelFilterPanel: 'Numberofcheckpoints', enabled: !isDatabaseStorage },
    { name: 'Memory', colNr: 8, labelFilterPanel: 'Estimatedmemoryusage', enabled: true },
    { name: 'Size', colNr: 9, labelFilterPanel: 'Storagesize', enabled: true },
    { name: 'Input', colNr: 10, labelFilterPanel: 'Input', enabled: true }
  ]

  const testedColumnAndNameCombinations = columnAndNameCombinations.filter((testCase) => testCase.name !== 'Status')

  for (const testCase of testedColumnAndNameCombinations.filter((c) => c.enabled)) {
    it(`Filter on field ${testCase.name}, expected at column ${testCase.colNr}`, () => {
      cy.visit('')
      // Enter Ladybug
      cy.getNumLadybugReports().should('equal', 5)
      // Check the name and column number combination
      cy.inIframeBody('[data-cy-debug="table"]').find(`th:eq(${testCase.colNr})`).contains(`${testCase.name}`)
      cy.inIframeBody('[data-cy-debug="tableRow"]:eq(0)').find(`td:eq(${testCase.colNr})`).then((el: JQuery<HTMLElement>) => {
        // TODO: It would be nice to get rid of this trim().
        const firstRowFieldValue = el.text().trim()
        cy.log(`Filtering on value: ${firstRowFieldValue}`)
        cy.inIframeBody('[data-cy-debug="filter"]').click()
        cy.enterFilter(testCase.labelFilterPanel, firstRowFieldValue)
        cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length.lessThan', 5)
        cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length.greaterThan', 0)
        cy.checkActiveFilterSphere(testCase.labelFilterPanel, firstRowFieldValue).should('be.visible')
        cy.inIframeBody('[data-cy-debug="clear-filter-btn"]').should('have.length', 1).click()
        cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 5)
        cy.inIframeBody('[data-cy-debug="close-filter-btn"]').click()
        cy.inIframeBody('[data-cy-debug="close-filter-btn"]').should('not.exist')
        cy.checkActiveFilterSphere(testCase.labelFilterPanel, firstRowFieldValue).should('not.exist')
      })
    })
  }

  it('Filter on two criteria', () => {
    cy.visit('')
    // Enter Ladybug
    cy.getNumLadybugReports().should('equal', 5)
    cy.inIframeBody('[data-cy-debug="filter"]').click()
    cy.enterFilter('Name', 'Adapter')
    cy.enterFilter('Input', 'yyy')
    cy.inIframeBody('[data-cy-debug="close-filter-btn"]').click()
    cy.checkActiveFilterSphere('Name', 'Adapter').should('be.visible')
    cy.checkActiveFilterSphere('Input', 'yyy').should('be.visible')
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 1)
    cy.inIframeBody('[data-cy-debug="filter"]').click()
    cy.inIframeBody('[data-cy-debug="clear-filter-btn"]').should('have.length', 1).click()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 5)
    cy.checkActiveFilterSphere('Name', 'Adapter').should('not.exist')
    cy.checkActiveFilterSphere('Input', 'yyy').should('not.exist')
    cy.inIframeBody('[data-cy-debug="close-filter-btn"]').click()
    cy.inIframeBody('[data-cy-debug="close-filter-btn"]').should('not.exist')
  })

  it('Change view so that a column goes on which there was a filter and original filter not saved', () => {
    cy.visit('')
    // Enter Ladybug
    cy.getNumLadybugReports().should('equal', 5)
    cy.inIframeBody('[data-cy-debug="filter"]').click()
    cy.enterFilter('Input', 'yyy')
    cy.inIframeBody('[data-cy-debug="close-filter-btn"]').click()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 3)
    cy.checkActiveFilterSphere('Input', 'yyy').should('be.visible')
    cy.inIframeBody('[data-cy-change-view-dropdown]').select('White box view no input')
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 5)
    cy.checkActiveFilterSphere('Input', 'yyy').should('not.exist')
    // Check that the original filter is not saved
    cy.inIframeBody('[data-cy-change-view-dropdown]').select('White box')
    cy.awaitLoadingSpinner()
    cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', 5)
    cy.checkActiveFilterSphere('Input', 'yyy').should('not.exist')
  })
})
