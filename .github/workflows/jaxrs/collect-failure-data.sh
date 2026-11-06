#!/usr/bin/env bash
# Collects the files that help to analyze a failure of jaxrs.js.yml into directory artifact-data,
# to be uploaded as a single artifact. Run from the workspace. Used by both jobs: the tests and
# the scheduled renewal of the caches. Paths that a job did not produce (for example the Cypress
# screenshots and videos in job renew-caches) are skipped.
set -euo pipefail

bash "$(dirname "${BASH_SOURCE[0]}")/../collect-artifact-data.sh" artifact-data \
  "ladybug/ladybug-frontend/cypress/screenshots" \
  "ladybug/ladybug-frontend/cypress/videos" \
  "ladybug/ladybug-test-webapp/data/ladybug.log" \
  "frank-runner/installFrankRunnerEnv.log" \
  "frank-runner/build/apache-tomcat-1*/logs" \
  "frank-runner/build/**/apache-tomcat-1*/logs" \
  "ladybug/ladybug-frontend/cypress.env.json" \
  "ladybug/ladybug-test-webapp/src/main/resources/test.properties" \
  "ladybug/ladybug-test-webapp/src/main/webapp/WEB-INF/web.xml" \
  "frank-runner/specials/ladybug/build.properties" \
  "frank-runner/build.properties"
