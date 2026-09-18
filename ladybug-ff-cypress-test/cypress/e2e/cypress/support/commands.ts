// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

interface Authentication {
  username: string;
  password: string;
}

export const AUTHENTICATIONS = new Map<string, Authentication>([
  ['observer', { username: 'observer', password: 'IbisObserver' }],
  ['dataAdmin', { username: 'dataAdmin', password: 'IbisDataAdmin' }],
  ['admin', { username: 'admin', password: 'IbisAdmin'}],
  ['tester', { username: 'tester', password: 'IbisTester' }],
  // User that does not exist
  ['xxx', { username: 'xxx', password: 'xxx' }],
])

declare global {
  namespace Cypress {
    interface Chainable<Subject = any> {
      inIframeBody(query: string): Chainable<any>
      clickTableRowWithStorageId(storageId: number): Chainable<any>
      enterLadybug(): void
      getNumLadybugReports(): Chainable<any>
      createReportWithTestPipelineApi(config: string, adapter: string, message: string, username?: string, password?: string): Chainable<any>
      getNumLadybugReportsForNameFilter(name: string): Chainable<number>
      createReportInLadybug(config: string, adapter: string, message: string, username?: string, password?: string): Chainable<number>
      createReportAndOpen(config: string, adapter: string, message: string, username?: string, password?: string);
      getAllStorageIdsInTable(): Chainable<number[]>
      guardedCopyReportToTestTab(alias: string)
      checkTestTabHasReportNamed(name: string): Cypress.Chainable<any>
      enterFilter(field: string, filter: string)
      checkActiveFilterSphere(field: string, value: string): Cypress.Chainable<any>
      apiDeleteAll(storageName: string)
      apiDeleteAllAsTester(storageName: string)
      apiSetGeneratorEnabledAsTester(enabled: boolean): Chainable<any>
      selectTreeNode(path: NodeSelection[]): Cypress.Chainable<any>
      awaitDebugTree(): void
      awaitLoadingSpinner(): void
      waitForVideo(): void
      trimmedText(): Chainable<any>
      checkpointValueEquals(expectedValue: string): void
      checkpointValueTrimmedEquals(expectedValue: string): void
      checkpointValueEmpty(): void
      checkNumCheckpointValueLabels(expectedNumLabels: number): void
      checkpointValueLabel(index: number): Chainable<any>
      visitAsTester(): void
      visitAs(username: string): void
      goToEnvironmentVariables(): void
      enableReportGenerator(): void
      executeJdbcQuery(): void
      stopAdapter(configuration: string, adapter: string): void
      startAdapter(configuration: string, adapter: string): void
      awaitAdapterStatus(configuration: string, adapter: string, status: string, retries: number): void
      checkCorrelationIdFromRow(row: unknown, expectedCorrelationId: string): void
      checkStatusFromRow(row: unknown, expectedStatus: string): void
    }
  }
}

Cypress.Commands.add('inIframeBody', (query) => {
  cy
    .get('iframe')
    .its('0.contentDocument')
    .its('body')
    .then(body => {
      cy.wrap(body).find(query)
    })
})

// The debug table can re-render while a matching row/cell is being located, which detaches
// jQuery references collected via .each() and makes the eventual .click() flaky. Using
// .contains() instead lets Cypress re-query the DOM and retry until the cell is actionable.
Cypress.Commands.add('clickTableRowWithStorageId', (storageId) => {
  cy.inIframeBody('[data-cy-debug="tableRow"]')
    .contains('td:nth-child(2)', new RegExp(`^\\s*${storageId}\\s*$`))
    .click()
})

Cypress.Commands.add('enterLadybug', () => {
  // Temporary diagnostic for issue #977: cy.contains().click() on a table row keeps
  // occasionally failing with "the page updated as a result of this command" further
  // down the line, in callers that rely on getNumLadybugReports() as a stability guard.
  // Logging every count/list response with its arrival time (shown in the command log
  // captured by the failure screenshot) should show whether more than one automatic
  // reload happens, and how its timing relates to the failing click. Remove once the
  // guard has been fixed for real.
  // Timestamps are absolute (Date.now()) so callers logging via cy.logDiag() elsewhere
  // (e.g. right before a click that might race a reload) can be lined up against these.
  // Intercept handlers run outside Cypress's normal command queue, so they must not call
  // queued cy commands (that broke every test using enterLadybug() with "Cypress detected
  // that you returned a promise from a command while also invoking one or more cy
  // commands in that promise"). Cypress.log() is the unqueued, direct logging API that is
  // safe to call from here.
  cy.intercept(
    {
      method: 'GET',
      url: `iaf/ladybug/api/metadata/${Cypress.env('debugStorageName') as string}/count`
    },
    (req) => {
      req.continue(() => {
        Cypress.log({ name: 'diag', message: `/count response at ${Date.now()}` })
      })
    }
  )
  cy.intercept(
    {
      method: 'GET',
      url: `iaf/ladybug/api/metadata/${Cypress.env('debugStorageName') as string}?*`
    },
    (req) => {
      req.continue(() => {
        Cypress.log({ name: 'diag', message: `list response at ${Date.now()}` })
      })
    }
  )
  cy.get('[data-cy-nav="status"]', { timeout: 10000 }).click()
  cy.get('[data-cy-nav="testingLadybug"]').should('not.be.visible')
  cy.get('[data-cy-nav="testing"]').click()
  cy.get('[data-cy-nav="testingLadybug"]').click()
  cy.awaitLoadingSpinner()
  cy.log(`[diag] debug tab clicked at ${Date.now()}`)
  cy.inIframeBody('[data-cy-nav-tab="debug"]').click()
})

// Temporary diagnostic for issue #977, see enterLadybug(). Lets callers elsewhere (e.g.
// right before a click that might race a reload) log a timestamp comparable to the
// count/list response timestamps logged in enterLadybug().
Cypress.Commands.add('logDiag', (message: string) => {
  cy.log(`[diag] ${message} at ${Date.now()}`)
})

Cypress.Commands.add('getNumLadybugReports', () => {
  cy.enterLadybug()
  cy.awaitLoadingSpinner()
  cy.intercept({
    method: 'GET',
    url: `iaf/ladybug/api/metadata/${Cypress.env('debugStorageName') as string}/count`,
    times: 1
  }).as('apiGetReports_2')
  // The refresh button also fires a separate request for the table's own row data
  // (no /count suffix). Waiting only for the count above let this command return before
  // that second request had come back, so the table could still change under callers
  // that immediately act on a row.
  cy.intercept({
    method: 'GET',
    url: `iaf/ladybug/api/metadata/${Cypress.env('debugStorageName') as string}?*`,
    times: 1
  }).as('apiGetReportsList')
  cy.inIframeBody('[data-cy-debug="refresh"]').click()
  cy.wait(['@apiGetReports_2', '@apiGetReportsList']).then(([interception]) => {
    const count: number = interception.response.body
    cy.inIframeBody('[data-cy-debug="amountShown"]').invoke('text')
      .should('equal', "/" + count);
    cy.inIframeBody('[data-cy-debug="tableRow"]')
      .should('have.length', count)
    return cy.wrap(count)
  })
})

// When the Test a Pipeline UI was used, the empty message was supported.
// Now that the API endpoint is called directly, the empty message does not work.
// An internal server error (500) was observed for that case.
Cypress.Commands.add('createReportWithTestPipelineApi', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  const formData = new FormData();
  formData.append('configuration', config);
  formData.append('adapter', adapter);
  formData.append('message', new Blob([message], { type: 'text/plain' }), 'message');
  const multipartHeader = {
    'Content-Type': 'multipart/form-data'
  }
  let authorizationHeader = {}
  if (username !== undefined) {
    if (password === undefined) {
      throw new Error('When you want to authorize with a username, then a password should be provided')
    }
    // Encode to Base64
    const encodedCredentials = btoa(`${username}:${password}`);
    authorizationHeader = {
      Authorization: `Basic ${encodedCredentials}`,
    }
  }
  const headers = { ...multipartHeader, ...authorizationHeader }
  cy.request({
    method: 'POST',
    url: 'iaf/api/test-pipeline',
    headers,
    body: formData
  }).then((response) => {
    expect(response.status).to.equal(200);
    const dec = new TextDecoder();
    const parsedResponse = JSON.parse(dec.decode(response.body));
    expect(parsedResponse.state).to.equal('SUCCESS');
  })
})

// Only works if some reports are expected to be omitted because of the filter
Cypress.Commands.add('getNumLadybugReportsForNameFilter', (name) => {
  cy.getNumLadybugReports().then(totalNumReports => {
    cy.inIframeBody('[data-cy-debug="filter"]').click()
    cy.enterFilter('Name', name)
    cy.inIframeBody('[data-cy-debug="tableRow"]').its('length')
      .should('be.lessThan', totalNumReports).then(result => {
        cy.inIframeBody('app-filter-side-drawer').find('label:contains(Name)')
          .parent().find('button:contains(Clear)').click()
        cy.inIframeBody('[data-cy-debug="tableRow"]').should('have.length', totalNumReports)
        cy.inIframeBody('app-filter-side-drawer').find('button:contains(Close)').click()
        cy.inIframeBody('app-filter-side-drawer').find('label').should('not.exist')
        return cy.wrap(result)
      })
  })
})

Cypress.Commands.add('createReportInLadybug', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  cy.getNumLadybugReports().then(numBefore => {
    cy.createReportWithTestPipelineApi(config, adapter, message, username, password)
    cy.getNumLadybugReports().should('equal', numBefore + 1)
    cy.getAllStorageIdsInTable().then(storageIds => {
      const storageId = Math.max.apply(null, storageIds)
      cy.log(`Last created report has storageId ${storageId.toString()}`)
      return cy.wrap(storageId)
    })
  })
})

Cypress.Commands.add('createReportAndOpen', (config: string, adapter: string, message: string, username?: string, password?: string) => {
  cy.createReportInLadybug('Example1a', 'Adapter1a', 'xxx').then(storageId => {
    cy.wrap('Found report just created, storageId=' + storageId)
    cy.clickTableRowWithStorageId(storageId)
  })
})

Cypress.Commands.add('getAllStorageIdsInTable', () => {
  const storageIds: number[] = []
  cy.inIframeBody('[data-cy-debug="tableRow"]').each($row => {
    cy.wrap($row).find('td:eq(1)').invoke('text').then(s => {
      storageIds.push(parseInt(s))
    })
  }).then(() => {
    cy.log(`Ladybug debug tab table has storage ids: ${storageIds.toString()}`)
    return cy.wrap(storageIds)
  })
})

Cypress.Commands.add('guardedCopyReportToTestTab', (alias) => {
  cy.intercept({
    method: 'PUT',
    url: /\/api\/report\/store\/*?/g,
    times: 1
  }).as(alias)
  cy.intercept({
    method: 'GET',
    url: /\/iaf\/ladybug\/api\/metadata\/Test*/g
  }).as('apiGetTestReports')
  cy.inIframeBody('[data-cy-debug-editor="copy"]').click()
  cy.wait(`@${alias}`).then((interception) => {
    cy.wrap(interception).its('request.url').should('contain', 'Test')
    cy.wrap(interception).its('response.statusCode').should('equal', 200)
  })
  cy.wait('@apiGetTestReports', { timeout: 30000 })
})

Cypress.Commands.add('checkTestTabHasReportNamed', (name) => {
  cy.inIframeBody('[data-cy-nav-tab="test"]').click()
  cy.inIframeBody('[data-cy-test="table"] tbody tr')
    .should('have.length', 1)
    .as('testtabReportRow')
  // TODO: It would be nice not to trim the text here.
  cy.get('@testtabReportRow').find('td:eq(2)').trimmedText().should('equal', name)
  cy.get('@testtabReportRow').find('td:eq(4)').should('be.empty')
  return cy.get('@testtabReportRow')
})

Cypress.Commands.add('enterFilter', (field: string, filter: string) => {
  const fieldQuery = `label:contains(${field})`
  cy.inIframeBody('app-filter-side-drawer').find(fieldQuery)
    .parent().find('input')
    .type(filter + '{enter}')
})

Cypress.Commands.add('checkActiveFilterSphere', (field: string, value: string) => {
  const expectedText = `${field}: ${value}`
  return cy.inIframeBody('app-active-filters').contains(expectedText)
})

Cypress.Commands.add('apiDeleteAll', (storageName: string) => {
  cy.request({
    method: 'DELETE',
    url: `/iaf/ladybug/api/report/all/${storageName}`
  }).then(response => {
    cy.wrap(response).its('status').should('equal', 200)
  })
})

Cypress.Commands.add('apiDeleteAllAsTester', (storageName: string) => {
  cy.request({
    method: 'DELETE',
    url: `/iaf/ladybug/api/report/all/${storageName}`,
    auth: AUTHENTICATIONS.get('tester')!
  }).then(response => {
    cy.wrap(response).its('status').should('equal', 200)
  })
})

Cypress.Commands.add('apiSetGeneratorEnabledAsTester', (enabled: boolean) => {
  cy.request({
    method: 'POST',
    url: '/iaf/ladybug/api/testtool',
    auth: AUTHENTICATIONS.get('tester')!,
    headers: { 'Content-Type': 'application/json' },
    body: { generatorEnabled: enabled ? 'true' : 'false' }
  }).then(response => {
    cy.wrap(response).its('status').should('equal', 200)
  })
})

interface TextWithSeq {
  text: string
  seq: number
}

type NodeSelection = TextWithSeq | string

function normalizeNodeSelection (input: NodeSelection): TextWithSeq {
  if (typeof input === 'string') {
    return { text: input, seq: 0 }
  } else {
    return input
  }
}

Cypress.Commands.add('selectTreeNode', (path: NodeSelection[]) => {
  const head = normalizeNodeSelection(path.shift())
  cy.inIframeBody(`[data-cy-debug-tree="root"] > app-tree-item > div > div:nth-child(1):contains(${head.text})`).then((elementsWithTexts) => {
    const chosen = elementsWithTexts[head.seq]
    return cy.wrap(chosen).parent().parent().then((element) => {
      if (path.length === 0) {
        return cy.wrap(element)
      } else {
        return selectTreeNodeImpl(element, path)
      }
    })
  })
})

function selectTreeNodeImpl (subject: JQuery<HTMLElement>, path: NodeSelection[]): Cypress.Chainable<any> | void {
  const head = normalizeNodeSelection(path.shift())
  cy.wrap(subject).find(`> div > div > div > app-tree-item > div > div:nth-child(1):contains(${head.text})`).then((elementsWithTexts) => {
    const chosen = elementsWithTexts[head.seq]
    if (path.length === 0) {
      return cy.wrap(chosen)
    } else {
      cy.wrap(chosen).parent().parent().then((element) => {
        return selectTreeNodeImpl(element, path)
      })
    }
  })
}

// [data-cy-debug-tree="root"] is the <ng-simple-tree> element itself, which is always
// present once the debug tab is mounted, whether or not a report has been opened yet.
// So asserting on its presence guards nothing; waiting for at least one app-tree-item is
// what actually guards against querying the tree before it has been built.
// Assumption: callers only use this right after navigating to a fresh page (e.g. via
// cy.visit()), so the tree is genuinely empty beforehand and cannot already contain stale
// items left over from a previously opened report. If a caller ever needs to await a
// report being opened while another one is already showing, this guard is not sufficient
// and would need to check for content specific to the new report instead of mere presence.
Cypress.Commands.add('awaitDebugTree', () => {
  cy.inIframeBody('[data-cy-debug-tree="root"] > app-tree-item').should('have.length.at.least', 1)
})

Cypress.Commands.add('awaitLoadingSpinner', () => {
  // We do not want to catch the moment that the loading spinner is NOT YET present
  cy.wait(400)
  cy.inIframeBody('[data-cy-loading-spinner]').should('not.exist')
  // TODO issue https://github.com/wearefrank/ladybug/issues/977. Until the
  // loading spinner is reliable, we do with a timeout that is larger than
  // the debounce time of 300 ms applied in FilterService.
  cy.wait(400)
})

// Wait so that the state of the UI is shown more clearly in videos.
Cypress.Commands.add('waitForVideo', () => {
  cy.wait(3000)
})

Cypress.Commands.add('trimmedText', { prevSubject: true }, (subject) => {
  cy.wrap(subject).invoke('text').then((theText) => {
    cy.wrap(trimMonacoText(theText))
  })
})

function trimMonacoText (value: string): string {
  const nbspRegex = /\u00A0/g
  return value.replace(nbspRegex, ' ').trim()
}

Cypress.Commands.add('checkpointValueEquals', { prevSubject: false }, (expectedValue) => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue) => actualValue === expectedValue, 6, 1000)
})

function checkLadybugCheckpointValue (checker: (string) => boolean, numberOfTimes: number, frequencyMS: number): void {
  if (numberOfTimes === 0) {
    throw new Error('checkLadybugCheckpointValue number of tries exceeded')
  }
  cy.log(`Remaining number of tries: ${numberOfTimes}`).then(() => {
    cy.inIframeBody('[data-cy-element-name="checkpointEditor"]').then((appEditor) => {
      const textOfAppEditor: string = appEditor.text()
      cy.log(`Text out of scrollable element: ${trimForLog(textOfAppEditor)}`).then(() => {
        if (checker(textOfAppEditor)) {
          cy.log('Text of app editor already matches')
        } else if (textOfAppEditor.length === 0) {
          cy.log('No text in app editor, next try').then(() => {
            cy.wait(frequencyMS)
            checkLadybugCheckpointValue(checker, numberOfTimes - 1, frequencyMS)
          })
        } else {
          cy.wrap(appEditor).find('.monaco-scrollable-element').then((monacoScrollableElement) => {
            const textOfScrollableElement: string = monacoScrollableElement.text()
            cy.log(`Text inside scrollable element: ${trimForLog(textOfScrollableElement)}`).then(() => {
              if (checker(textOfScrollableElement)) {
                cy.log('Text inside scrollable element matches')
              } else {
                cy.wait(frequencyMS)
                checkLadybugCheckpointValue(checker, numberOfTimes - 1, frequencyMS)
              }
            })
          })
        }
      })
    })
  })
}

function trimForLog (value: string): string {
  if (value.length > 20) {
    return `${value.substring(0, 17)}...`
  } else {
    return value
  }
}

Cypress.Commands.add('checkpointValueTrimmedEquals', { prevSubject: false }, (expectedValue) => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue: string) => trimMonacoText(actualValue) === expectedValue, 6, 1000)
})

Cypress.Commands.add('checkpointValueEmpty', { prevSubject: false }, () => {
  cy.inIframeBody('app-checkpoint-value');
  checkLadybugCheckpointValue((actualValue: string) => actualValue.length === 0, 6, 1000)
})

Cypress.Commands.add('checkNumCheckpointValueLabels', { prevSubject: false }, (expectedNumLabels: number) => {
  cy.inIframeBody('app-report-alert-message2 > div > div')
    .should('have.length', expectedNumLabels)
})

Cypress.Commands.add('checkpointValueLabel', { prevSubject: false }, (index: number) => {
  cy.inIframeBody(`app-report-alert-message2 > div > div:eq(${index})`)
})

Cypress.Commands.add('visitAsTester', { prevSubject: false }, () => {
  cy.visit('', {
    auth: AUTHENTICATIONS.get('tester')!,
  })
})

Cypress.Commands.add('visitAs', { prevSubject: false }, (username: string) => {
  cy.visit('', {
    auth: AUTHENTICATIONS.get(username)!,
  })
})

Cypress.Commands.add('goToEnvironmentVariables', { prevSubject: false }, () => {
  // The nav link's pointer-events stay disabled until an async permissions
  // check resolves. Clicking before that is a silent no-op that leaves the
  // page on the previous route, so wait for the link to become clickable.
  cy.contains('Environment Variables', { timeout: 10000 })
    .should('not.have.css', 'pointer-events', 'none')
    .click()
  // Adapter Status also has an input[name=search]. Wait for the actual
  // navigation to the Environment Variables page so a caller does not
  // interact with the search box still shown from the previous page.
  cy.url().should('include', '/environment-variables')
})

Cypress.Commands.add('enableReportGenerator', { prevSubject: false }, () => {
  cy.inIframeBody('[data-cy-debug="openSettings"]').should('be.visible').click()
  cy.inIframeBody('[role=dialog]').should('be.visible')
  cy.inIframeBody('[data-cy-settings="generatorEnabled"]').select('Enabled');
  cy.inIframeBody('[data-cy-settings="generatorEnabled"]').find(':selected').invoke('text').should('equal', 'Enabled');
  cy.inIframeBody('[data-cy-settings="saveChanges"]').click()
})

Cypress.Commands.add('executeJdbcQuery', { prevSubject: false }, () => {
  cy.request({
    method: 'POST',
    url: `${Cypress.config('baseUrl')}/iaf/api/jdbc/query`,
    headers: {
      'Content-Type': 'application/json',
    },
    body: {
      query: 'SELECT * FROM LADYBUG',
      queryType: 'AUTO',
      datasource: Cypress.env('jdbcDatasource'),
      resultType: 'csv',
      avoidLocking: false,
      trimSpaces: false,
    },
  }).then((resp) => {
    expect(resp.status).to.equal(200);
  });
})

Cypress.Commands.add('stopAdapter', { prevSubject: false }, (configuration: string, adapter: string) => {
  cy.request({
    method: 'PUT',
    url: `${Cypress.config('baseUrl')}/iaf/api/configurations/${encodeURIComponent(configuration)}/adapters/${encodeURIComponent(adapter)}`,
    headers: {
      'Content-Type': 'application/json',
    },
    body: '{"action":"stop"}',
  }).then((resp) => {
    expect(resp.status).to.equal(202);
    cy.awaitAdapterStatus(configuration, adapter, 'stopped', 20)
  })
})

Cypress.Commands.add('startAdapter', { prevSubject: false }, (configuration: string, adapter: string) => {
  cy.request({
    method: 'PUT',
    url: `${Cypress.config('baseUrl')}/iaf/api/configurations/${encodeURIComponent(configuration)}/adapters/${encodeURIComponent(adapter)}`,
    headers: {
      'Content-Type': 'application/json',
    },
    body: '{"action":"start"}',
  }).then((resp) => {
    expect(resp.status).to.equal(202);
    cy.awaitAdapterStatus(configuration, adapter, 'started', 20)
  })
})

Cypress.Commands.add('awaitAdapterStatus', { prevSubject: false }, (configuration: string, adapter: string, status: string, retries: number) => {
  cy.request({
    method: 'GET',
    url: `${Cypress.config('baseUrl')}/iaf/api/configurations/${encodeURIComponent(configuration)}/adapters/${encodeURIComponent(adapter)}`,
  }).then((resp) => {
    expect(resp.status).to.equal(200);
    if (resp.body.state === status || retries <= 0) {
      expect(resp.body.state).to.equal(status)
    } else {
      cy.wait(5000)
      cy.awaitAdapterStatus(configuration, adapter, status, retries - 1);
    }
  })
})

// http://localhost/iaf/api/configurations/Example1a/adapters/Adapter1a
// awaitAdapterStatus(configuration: string, adapter: string, status: string): void

Cypress.Commands.add('checkCorrelationIdFromRow', { prevSubject: true }, (row, expectedCorrelationId) => {
  cy.wrap(row).find('td:eq(5)').trimmedText().should('equal', expectedCorrelationId)
})

Cypress.Commands.add('checkStatusFromRow', { prevSubject: true }, (row, expectedStatus) => {
  cy.wrap(row).find('td:eq(6)').trimmedText().should('equal', expectedStatus)
})
