#!/usr/bin/env bash
# Collects the files that help to analyze a failure of springmvc.js.yml into directory
# artifact-data, to be uploaded as a single artifact. Run from the workspace.
set -euo pipefail

bash "$(dirname "${BASH_SOURCE[0]}")/../collect-artifact-data.sh" artifact-data \
  "ladybug/ladybug-frontend/cypress/screenshots" \
  "ladybug/ladybug-frontend/cypress/videos" \
  "ladybug/ladybug-spring-boot-test-webapp/data/ladybug.log" \
  "ladybug/ladybug-frontend/cypress.env.json"
