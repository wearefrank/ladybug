#!/usr/bin/env bash
# Waits until ladybug-spring-boot-test-webapp is healthy. Run from the workspace.
#
# Health check: a GET of the Ladybug API, which the frontend also uses. With
# ladybug.ui.test.mode=NO_AUTH, Spring Security lets every request through, so it returns 200.
# Otherwise Spring Security requires Basic authentication for /ladybug/**, so the request without
# credentials returns 401. That still proves that the application is up, because Spring Security
# is configured by the application. Any other status is not healthy.
#
# Needs environment variable UI_TEST_MODE: the value of ladybug.ui.test.mode.
set -euo pipefail

if [[ "${UI_TEST_MODE}" == "NO_AUTH" ]]; then
  expected_status=200
else
  expected_status=401
fi

bash "$(dirname "${BASH_SOURCE[0]}")/../wait-for-server.sh" \
  http://localhost:8090/ladybug/api/testtool 300 "${expected_status}"
