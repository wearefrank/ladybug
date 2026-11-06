#!/usr/bin/env bash
# Helper of action.yml in the same directory. See there for an explanation of the cache.
#
# Usage: cache.sh restore|save|remember
#   restore   Sets the paths of the cache (environment variable FRANK_RUNNER_DOWNLOAD_CACHE_PATHS)
#             and output "key".
#   remember  After restoring: remembers the key and the cache hit for saving, in environment
#             variables. Needs environment variables CACHE_KEY and CACHE_HIT.
#   save      Sets output "save" to tell whether the cache should be saved.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../cache-common.sh"

mode="$1"
case "${mode}" in
  restore)
    # The paths must be the same when restoring and saving, otherwise the cache cannot be found.
    # Excluded are the Frank!Framework WAR (a new snapshot every day, downloaded when needed)
    # and Frank!Runner's runtime state (metadata about latest versions and a copy of its
    # build.xml).
    {
      echo "FRANK_RUNNER_DOWNLOAD_CACHE_PATHS<<EOF"
      echo "frank-runner/download"
      echo "!frank-runner/download/frankframework-webapp-*"
      echo "!frank-runner/download/metadata"
      echo "!frank-runner/download/frank-runner"
      echo "EOF"
    } >> "${GITHUB_ENV}"
    # build.xml and env.sh define what Frank!Runner downloads.
    set_output key "${RUNNER_OS}-frank-runner-download-$(hash_of_files frank-runner/build.xml frank-runner/env.sh)"
    ;;
  remember)
    # Frank!Runner may regenerate env.sh, so the key is not computed again when saving.
    # CACHE_KEY and CACHE_HIT are set by the step, see action.yml. CACHE_HIT is empty on a miss.
    # shellcheck disable=SC2153
    set_env FRANK_RUNNER_DOWNLOAD_CACHE_KEY "${CACHE_KEY}"
    set_env FRANK_RUNNER_DOWNLOAD_CACHE_HIT "${CACHE_HIT:-}"
    ;;
  save)
    if [[ -z "${FRANK_RUNNER_DOWNLOAD_CACHE_KEY:-}" ]]; then
      echo "Call this action with mode restore before calling it with mode save" >&2
      exit 1
    fi
    decide_about_saving save "${FRANK_RUNNER_DOWNLOAD_CACHE_HIT}"
    ;;
  *)
    echo "Input mode must be restore or save, but is '${mode}'" >&2
    exit 1
    ;;
esac
