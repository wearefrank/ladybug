#!/usr/bin/env bash
# Helper of action.yml in the same directory. Starts ladybug-spring-boot-test-webapp in the
# background on port 8090, with its output in serverStartLog.log. Run from directory
# ladybug/ladybug-spring-boot-test-webapp.
#
# Needs environment variables:
#   SPRING_PROFILE  Spring profile for the backend, or none for the default.
#   UI_TEST_MODE    Value of ladybug.ui.test.mode, DEFAULT for the default.
set -euo pipefail

if [[ "${SPRING_PROFILE}" == "none" && "${UI_TEST_MODE}" == "DEFAULT" ]]; then
  echo "Starting with default Spring profile and default ladybug.ui.test.mode"
  mvn -B spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=8090" \
    > serverStartLog.log 2>&1 &
else
  # Spring profile "none" is passed as is when only the ladybug.ui.test.mode is not the default.
  echo "Starting with Spring profile ${SPRING_PROFILE} and ladybug.ui.test.mode=${UI_TEST_MODE}"
  mvn -B spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=8090 --spring.profiles.active=${SPRING_PROFILE}" \
    -Dspring-boot.run.jvmArguments="-Dladybug.ui.test.mode=${UI_TEST_MODE}" \
    > serverStartLog.log 2>&1 &
fi
