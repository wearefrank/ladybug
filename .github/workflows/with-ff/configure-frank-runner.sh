#!/usr/bin/env bash
# Configures Frank!Runner and the Frank application for test-with-ff.js.yml. Run from the
# workspace, after calculate-paths.sh.
#
# Needs environment variables:
#   DTAP                     Matrix value dtap: LOC or PRD.
#   STORAGE                  Matrix value storage: file or database.
#   CONFIGURATIONS           Matrix value configurations: directory in
#                            ladybug-ff-cypress-test/TestConfigurations.
#   RESOURCES_DIR            Set by calculate-paths.sh.
#   RELATIVE_PATH_TO_LADYBUG_FF_CYPRESS_TEST  Set by calculate-paths.sh.
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"
frank_runner_properties=frank-runner/build.properties
deployment_specifics="${RESOURCES_DIR}/DeploymentSpecifics.properties"

cp "${here}/build-frankrunner-unconditional.properties" "${frank_runner_properties}"
if [[ "${DTAP}" == "PRD" ]]; then
  echo "dtap.stage=PRD" >> "${frank_runner_properties}"
fi
# Load configurations from ladybug-ff-cypress-test.
echo "override.configurations.dir=${RELATIVE_PATH_TO_LADYBUG_FF_CYPRESS_TEST}/TestConfigurations/${CONFIGURATIONS}" >> "${frank_runner_properties}"

if [[ "${STORAGE}" == "database" ]]; then
  # Let the Frank!Framework use a database for Ladybug.
  cat "${here}/DeploymentSpecifics-enable-database.properties" >> "${deployment_specifics}"
fi

if [[ "${DTAP}" == "PRD" ]]; then
  # Require authentication.
  cat "${here}/DeploymentSpecifics-authorization.properties" >> "${deployment_specifics}"
  cp "${here}/localUsers.yml.txt" "${RESOURCES_DIR}/localUsers.yml"
fi

echo "${frank_runner_properties}:"
cat "${frank_runner_properties}"
if [[ -f "${deployment_specifics}" ]]; then
  echo "${deployment_specifics}:"
  cat "${deployment_specifics}"
fi
