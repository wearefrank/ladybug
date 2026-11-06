#!/usr/bin/env bash
# Helper of action.yml in the same directory. See there for an explanation of the caches.
#
# Usage: pnpm-install.sh prepare|decide-about-saving
#   prepare              Sets outputs storePath, pnpmStoreKey, pnpmStoreRestoreKeys, hasCypress
#                        and cypressKey. Needs environment variable PROJECT_DIR.
#   decide-about-saving  Sets outputs savePnpmStore and saveCypress. Needs environment variables
#                        PNPM_STORE_CACHE_HIT, CYPRESS_CACHE_HIT and HAS_CYPRESS.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../cache-common.sh"

mode="$1"
case "${mode}" in
  prepare)
    cd "${PROJECT_DIR}"
    echo "pnpm version: $(pnpm -v)"
    set_output storePath "$(pnpm store path --silent)"

    # The name of the project is part of the key, because projects have different packages.
    project_name="$(basename "$(pwd)")"
    pnpm_store_key_prefix="${RUNNER_OS}-pnpm-store-${project_name}-"
    set_output pnpmStoreKey "${pnpm_store_key_prefix}$(hash_of_files pnpm-lock.yaml)"
    if is_branch_master; then
      # Without restore-keys, a cache saved on master contains only what the current
      # pnpm-lock.yaml needs.
      set_output pnpmStoreRestoreKeys ""
    else
      # Branches never save caches. When pnpm-lock.yaml has changed, an older cache still
      # provides most packages.
      set_output pnpmStoreRestoreKeys "${pnpm_store_key_prefix}"
    fi

    # The Cypress binary only depends on the version of Cypress. The version must be exact in
    # package.json, otherwise the key does not match the binary that is installed.
    cypress_version="$(node -p "const p = require('./package.json'); (p.devDependencies || {}).cypress || (p.dependencies || {}).cypress || ''")"
    if [[ -n "${cypress_version}" ]]; then
      set_output hasCypress true
      set_output cypressKey "${RUNNER_OS}-cypress-${cypress_version}"
    else
      echo "Project does not depend on Cypress"
      set_output hasCypress false
      set_output cypressKey ""
    fi
    ;;
  decide-about-saving)
    echo "pnpm store:"
    decide_about_saving savePnpmStore "${PNPM_STORE_CACHE_HIT:-}"
    if [[ "${HAS_CYPRESS}" == "true" ]]; then
      echo "Cypress binary:"
      decide_about_saving saveCypress "${CYPRESS_CACHE_HIT:-}"
    else
      set_output saveCypress false
    fi
    ;;
  *)
    echo "Unknown mode '${mode}'" >&2
    exit 1
    ;;
esac
