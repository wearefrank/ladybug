#!/usr/bin/env bash
# Collects the files that help to analyze a failure of test-with-ff.js.yml into directory
# artifact-data, to be uploaded as a single artifact. Run from the workspace.
#
# Needs environment variable RESOURCES_DIR, set by calculate-paths.sh.
set -euo pipefail

bash "$(dirname "${BASH_SOURCE[0]}")/../collect-artifact-data.sh" artifact-data \
  "ladybug/ladybug-ff-cypress-test/cypress/screenshots" \
  "ladybug/ladybug-ff-cypress-test/cypress/videos" \
  "frank-runner/serverStartLog.log" \
  "frank-runner/specials/ladybug/serverStartLog.log" \
  "frank-runner/specials/ladybug/buildIafWebapp.log" \
  "ladybug/ladybug-ff-test-webapp/serverStartLog.log" \
  "frank-runner/build/apache-tomcat-10*/logs" \
  "frank-runner/build/**/apache-tomcat-1*/logs" \
  "frank-runner/build.properties" \
  "frank-runner/specials/ladybug/build.properties" \
  "frank-runner/specials/iaf-webapp/build.properties" \
  "ladybug/ladybug-ff-test-webapp/build.properties" \
  "ladybug/ladybug-ff-test-webapp/pom.xml" \
  "${RESOURCES_DIR:-missing-resources-dir}/DeploymentSpecifics.properties" \
  "${RESOURCES_DIR:-missing-resources-dir}/localUsers.yml"
