#!/usr/bin/env bash
# Starts the backend for test-with-ff.js.yml in the background, on port 8090. Run from the
# workspace, after prepare-backend.sh. Use wait-for-server.sh to wait until the server is healthy.
#
# Needs environment variables:
#   BUILD_CODE  true if the code of frankframework and ladybug is built.
#   TEST_APP    Matrix value test-app: true if ladybug-ff-test-webapp is tested.
set -euo pipefail

if [[ "${TEST_APP}" == "true" ]]; then
  if [[ "${BUILD_CODE}" == "true" ]]; then
    echo "Building the Frank!Framework and ladybug with frank-runner/specials/ladybug, without starting them"
    (cd frank-runner/specials/ladybug && ./restart.sh > buildIafWebapp.log)
  fi
  echo "Starting ladybug-ff-test-webapp with the Frank!Framework as Maven artifact"
  (cd ladybug/ladybug-ff-test-webapp && exec ./restart.sh > serverStartLog.log) &
elif [[ "${BUILD_CODE}" == "true" ]]; then
  echo "Starting the locally-built Frank!Framework and ladybug with frank-runner/specials/ladybug"
  (cd frank-runner/specials/ladybug && exec ./restart.sh > serverStartLog.log) &
else
  echo "Starting an official release of the Frank!Framework with Frank!Runner"
  (cd frank-runner && exec ./restart.sh > serverStartLog.log) &
fi
