#!/usr/bin/env bash
# Calculates the paths that test-with-ff.js.yml needs and sets them as environment variables for
# the next steps. Run from the workspace, after checking out frank-runner, ladybug and (if
# BUILD_CODE) frankframework.
#
# Needs environment variables:
#   BUILD_CODE  true if the code of frankframework and ladybug is built.
#   TEST_APP    Matrix value test-app: true if ladybug-ff-test-webapp is tested.
#
# Sets environment variables:
#   FRANK_APP                Directory of the Frank application that Frank!Runner starts.
#   FRANK_PROJECT_DIR        Directory with the original files of the Frank application.
#   RESOURCES_DIR            Resources directory of the Frank application.
#   LADYBUG_JDBC_DATASOURCE  JDBC datasource that Ladybug uses.
#   RELATIVE_PATH_TO_LADYBUG_FF_CYPRESS_TEST  Path to ladybug-ff-cypress-test, relative to
#                                             FRANK_APP.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../../helpers.sh"

if [[ "${TEST_APP}" == "true" ]]; then
  frank_app="$(realpath ladybug/ladybug-ff-test-webapp)"
  frank_project_dir="${frank_app}"
  resources_dir="${frank_app}/src/main/resources"
elif [[ "${BUILD_CODE}" == "true" ]]; then
  frank_app="$(realpath frankframework/webapp)"
  frank_project_dir="$(realpath frank-runner/examples/Frank2Example1)"
  resources_dir="${frank_app}/src/main/resources"
else
  frank_app="$(realpath frank-runner/examples/Frank2Example1)"
  frank_project_dir="${frank_app}"
  resources_dir="${frank_app}/classes"
fi
set_env FRANK_APP "${frank_app}"
set_env FRANK_PROJECT_DIR "${frank_project_dir}"
set_env RESOURCES_DIR "${resources_dir}"

# Frank!Runner derives its JDBC datasource name from the basename of the directory it runs
# (ant property instance.name, see build.xml), which is always the same directory as FRANK_APP.
# This assumes no DeploymentSpecifics.properties sets instance.name explicitly, which would
# override the basename and make this calculation incorrect.
set_env LADYBUG_JDBC_DATASOURCE "jdbc/$(basename "${frank_app}" | tr '[:upper:]' '[:lower:]')"

set_env RELATIVE_PATH_TO_LADYBUG_FF_CYPRESS_TEST "$(realpath --relative-to="${frank_app}" ladybug/ladybug-ff-cypress-test)"
