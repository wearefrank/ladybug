#!/usr/bin/env bash
# Runs the Cypress tests of test-with-ff.js.yml. Run from directory
# ladybug/ladybug-ff-cypress-test, when the server is up.
#
# Needs environment variables:
#   SPECS                    Matrix value specs: the Cypress specs to run.
#   DEBUG_STORAGE_NAME       Matrix value debugStorageName.
#   STORAGE                  Matrix value storage: file or database.
#   RESOURCES_DIR, FRANK_PROJECT_DIR, LADYBUG_JDBC_DATASOURCE  Set by calculate-paths.sh.
set -euo pipefail

# Be sure there are no old videos and screenshots.
rm -rf cypress/videos cypress/screenshots

expose="debugStorageName=${DEBUG_STORAGE_NAME}"
expose+=",frankProjectResourcesDir=${RESOURCES_DIR}"
expose+=",frankProjectOriginalFiles=${FRANK_PROJECT_DIR}"
expose+=",storage=${STORAGE}"
expose+=",jdbcDatasource=${LADYBUG_JDBC_DATASOURCE}"

pnpm exec cypress run --config baseUrl=http://localhost:8090 --spec "${SPECS}" --expose "${expose}"
