describe('Test api that is by external applications', () => {
  before(() => {
    cy.resetApp();
    cy.createReport();
    cy.createOtherReport();
    cy.createJsonReport();
  })

  after(() => {
    cy.resetApp();
  })

  it('When metadata is requested then offset is taken into account', () => {
    cy.request(metadataUrl(10)).should((response) => {
      checkNames(response, ['Simple report', 'Another simple report', 'Json checkpoint']);
    });
    cy.request(metadataUrl(2)).should((response) => {
      checkNames(response, ['Simple report', 'Another simple report']);
    });
    cy.request(metadataUrl(2, 1)).should((response) => {
      checkNames(response, ['Another simple report', 'Json checkpoint']);
    });
    cy.request(metadataUrl(1, 1)).should((response) => {
      checkNames(response, ['Another simple report']);
    });
  })
})

function metadataUrl(limit: number, offset?: number): string {
  const backendServer = Cypress.env('backendServer');
  let result = `${backendServer}/ladybug/api/metadata/Debug?metadataNames=storageId&metadataNames=name&limit=${limit}`;
  if (offset !== undefined) {
    result += `&offset=${offset}`;
  }
  return result;
}

function checkNames(response: any, names: string[]) {
  const fields = Object.keys(response.body[0]);
  expect(fields).to.have.length(2);
  expect(fields[0]).to.equal('storageId');
  expect(fields[1]).to.equal('name');
  expect(response.body).to.have.length(names.length);
  for (let index = 0; index < names.length; index++) {
    expect(response.body[index].name).to.equal(names[index]);
  }
}