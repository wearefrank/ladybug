#!/usr/bin/env bash
# Helper of action.yml in the same directory. Waits until ladybug-test-webapp is healthy.
#
# Health check: a GET of the Ladybug API, which the frontend also uses. Without authentication it
# returns 200. With authentication, web.xml (the CICD_AUTH part) makes Tomcat require Basic
# authentication for the whole webapp, so the request without credentials returns 401. That still
# proves that ladybug-test-webapp is deployed, because that security constraint is part of its
# web.xml. Any other status (for example 404 when the webapp is not deployed) is not healthy.
#
# Needs environment variable AUTHENTICATION: true if authentication is enabled.
set -euo pipefail

if [[ "${AUTHENTICATION}" == "true" ]]; then
  expected_status=401
else
  expected_status=200
fi

bash "$(dirname "${BASH_SOURCE[0]}")/../../workflows/wait-for-server.sh" \
  http://localhost:8090/ladybug/api/testtool 300 "${expected_status}"
