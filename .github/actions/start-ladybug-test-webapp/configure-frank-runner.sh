#!/usr/bin/env bash
# Helper of action.yml in the same directory. Configures Frank!Runner to build and start
# ladybug-test-webapp. Run from the workspace, in which Frank!Runner is checked out in directory
# frank-runner.
#
# Needs environment variable SPRING_PROFILE: Spring profile for the backend, or none to use the
# default.
set -euo pipefail

ladybug_properties=frank-runner/specials/ladybug/build.properties
frank_runner_properties=frank-runner/build.properties

# Skip backend unit tests, Javadoc and checking licenses.
echo "maven.skip.tests=true" > "${ladybug_properties}"
echo "maven.skip.javadoc=true" >> "${ladybug_properties}"
echo "maven.skip.copyright=true" >> "${ladybug_properties}"
if [[ "${SPRING_PROFILE}" != "none" ]]; then
  echo "spring.profiles.active=${SPRING_PROFILE}" >> "${ladybug_properties}"
fi

# Port where ladybug-test-webapp is served.
echo "tomcat.connector.port=8090" > "${frank_runner_properties}"
# Prevent Java exception about binding port 443 (for https).
echo "tomcat.secure.port=8443" >> "${frank_runner_properties}"

chmod a+x frank-runner/specials/ladybug/restart.sh

echo "${ladybug_properties}:"
cat "${ladybug_properties}"
echo "${frank_runner_properties}:"
cat "${frank_runner_properties}"
