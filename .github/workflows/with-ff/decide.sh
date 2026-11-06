#!/usr/bin/env bash
# Decides what test-with-ff.js.yml builds and checks the inputs of the workflow.
#
# Needs environment variables:
#   BUILD_CODE_INPUT  Input buildLatestFFandLadybugCode: true, false or empty (when not started
#                     manually).
#   FF_FORK           Input ffFork, can be empty.
#   FF_BRANCH         Input ffBranch, can be empty.
#   TEST_APP          Matrix value test-app: true or false.
#
# Sets outputs:
#   buildCode       true if the code of frankframework and ladybug is built, otherwise false.
#   ffRepository    Repository of the Frank!Framework to check out if buildCode.
#   needsMaven      true if Maven builds something, so the Maven cache is needed.
#   mavenCacheName  Name of the Maven cache.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../../helpers.sh"

if [[ "${BUILD_CODE_INPUT}" == "true" ]]; then
  build_code=true
else
  build_code=false
fi
set_output buildCode "${build_code}"

if [[ "${build_code}" == "false" ]]; then
  for input in "ffFork=${FF_FORK}" "ffBranch=${FF_BRANCH}"; do
    if [[ -n "${input#*=}" ]]; then
      echo "Input ${input%%=*} can only be set if input buildLatestFFandLadybugCode is true" >&2
      exit 1
    fi
  done
fi

set_output ffRepository "${FF_FORK:-frankframework}/frankframework"

# Maven builds the code if buildCode. ladybug-ff-test-webapp is always built by Maven.
if [[ "${build_code}" == "true" || "${TEST_APP}" == "true" ]]; then
  set_output needsMaven true
else
  set_output needsMaven false
fi

# Building the code and building ladybug-ff-test-webapp need different Maven artifacts.
set_output mavenCacheName "test-with-ff-buildCode-${build_code}-test-app-${TEST_APP}"
