#!/usr/bin/env bash
# Helper of action.yml in the same directory. See there for an explanation of the cache.
#
# Usage: cache.sh restore|save|remember
#   restore   Sets output "key". Needs environment variable NAME.
#   remember  After restoring: remembers the key and the cache hit for saving, in environment
#             variables. Needs environment variables CACHE_KEY and CACHE_HIT.
#   save      Sets output "save" to tell whether the cache should be saved.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/../cache-common.sh"

mode="$1"
case "${mode}" in
  restore)
    if [[ -z "${NAME:-}" ]]; then
      echo "Input name is required for mode restore" >&2
      exit 1
    fi
    # A new key every month refreshes the cache regularly.
    month="$(date -u +%Y-%m)"
    set_output key "${RUNNER_OS}-maven-${NAME}-${month}"
    ;;
  remember)
    # The key is not computed again when saving, because the month may have changed meanwhile.
    # CACHE_KEY and CACHE_HIT are set by the step, see action.yml. CACHE_HIT is empty on a miss.
    # shellcheck disable=SC2153
    set_env MAVEN_CACHE_KEY "${CACHE_KEY}"
    set_env MAVEN_CACHE_HIT "${CACHE_HIT:-}"
    ;;
  save)
    if [[ -z "${MAVEN_CACHE_KEY:-}" ]]; then
      echo "Call this action with mode restore before calling it with mode save" >&2
      exit 1
    fi
    decide_about_saving save "${MAVEN_CACHE_HIT}"
    ;;
  *)
    echo "Input mode must be restore or save, but is '${mode}'" >&2
    exit 1
    ;;
esac
