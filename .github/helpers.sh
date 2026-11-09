#!/usr/bin/env bash
# Functions for the scripts that GitHub Actions runs (in .github/actions and .github/workflows).
# Source this file.

# Sets an output of the current step and shows it in the log.
set_output() {
  local name="$1"
  local value="$2"
  echo "${name}=${value}" >> "${GITHUB_OUTPUT}"
  echo "${name}=${value}"
}

# Sets an environment variable for the next steps of the job and shows it in the log.
set_env() {
  local name="$1"
  local value="$2"
  echo "${name}=${value}" >> "${GITHUB_ENV}"
  echo "${name}=${value}"
}
