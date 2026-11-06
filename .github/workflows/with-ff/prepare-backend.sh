#!/usr/bin/env bash
# Prepares building the backend for test-with-ff.js.yml: puts build.properties files in place and
# adjusts pom.xml files. Run from the workspace.
#
# Needs environment variables:
#   BUILD_CODE  true if the code of frankframework and ladybug is built.
#   TEST_APP    Matrix value test-app: true if ladybug-ff-test-webapp is tested.
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"
test_webapp=ladybug/ladybug-ff-test-webapp
sync_pom_versions=frank-runner/specials/util/syncPomVersions

if [[ "${TEST_APP}" == "true" ]]; then
  cp "${here}/build-ladybug-ff-test-webapp.properties" "${test_webapp}/build.properties"
fi

if [[ "${BUILD_CODE}" == "false" && "${TEST_APP}" == "true" ]]; then
  # ladybug-ff-test-webapp/pom.xml refers to org.frankframework:frankframework-webapp as LATEST.
  # Resolving LATEST forces Maven to re-walk the whole dependency tree of the Frank!Framework
  # over the network on every run, which is slow and makes the Maven download cache useless.
  # Substitute a concrete version instead, so Maven can resolve it from the (cached) local
  # repository once it has been downloaded once. This queries the same Nexus repository that
  # ladybug-ff-test-webapp/pom.xml itself declares under <repositories>, so the version found
  # here is guaranteed to be resolvable from there.
  ff_version="$(bash "${here}/../get-highest-ff-version.sh")"
  echo "Highest version of Frank!Framework from Nexus is ${ff_version}"
  sed -i "s#<ff.version>LATEST</ff.version>#<ff.version>${ff_version}</ff.version>#" "${test_webapp}/pom.xml"
fi

if [[ "${BUILD_CODE}" == "true" ]]; then
  if [[ "${TEST_APP}" == "true" ]]; then
    # Let ladybug-ff-test-webapp, the Frank!Framework and ladybug find each other. This
    # substitutes the exact locally-built revision of the Frank!Framework in
    # ladybug-ff-test-webapp, so no version lookup is needed.
    (cd "${sync_pom_versions}" && ./runIncludingLadybugFfTestWebapp.sh)
  else
    # Let ladybug and the Frank!Framework reference each other.
    (cd "${sync_pom_versions}" && ./run.sh)
  fi

  # When frank-runner/specials/iaf-webapp is executed, we always get the Maven artifacts in .m2.
  cp "${here}/build-iaf-webapp.properties" frank-runner/specials/iaf-webapp/build.properties
  if [[ "${TEST_APP}" == "true" ]]; then
    # ladybug-ff-test-webapp is started instead, see start-backend.sh.
    echo -e "\nskip.start=true" >> frank-runner/specials/iaf-webapp/build.properties
  fi
  cp "${here}/build-ladybug.properties" frank-runner/specials/ladybug/build.properties
fi
