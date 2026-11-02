#!/usr/bin/env bash
# Prints the users that were actually authenticated during a Cypress run, so that a passing production-auth or
# backend-auth job still shows which user it logged in as, instead of only being visible when a test fails.
#
# Relies on ladybug.log containing lines in the "[METHOD]path[user]" format, written by
# org.wearefrank.ladybug.web.jaxrs.ApiAuthorizationFilter (jaxrs) and AuthenticatedUserLoggingInterceptor
# (springmvc).
#
# Usage: show-authenticated-users.sh <path-to-ladybug.log>
set -uo pipefail

log_file="$1"

users="$(grep -oP '\[[A-Z]+\][^][]*\[[^]]*\]' "$log_file" | awk -F'[][]' '{print $4}' | sort -u || true)"

if [ -z "$users" ]; then
  echo "ERROR: could not find any authenticated user logged in $log_file" >&2
  exit 1
fi

echo "$users"
