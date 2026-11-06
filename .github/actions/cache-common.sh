#!/usr/bin/env bash
# Functions shared by the scripts of the actions in .github/actions. Source this file.

source "$(dirname "${BASH_SOURCE[0]}")/../helpers.sh"

# Caches are only saved on master. GitHub scopes caches to the branch that saved them, but every
# branch and pull request can restore the caches of master. Saving only on master gives one set
# of caches that all branches share, instead of a set of caches per branch.
is_branch_that_saves_caches() {
  [[ "${GITHUB_REF}" == "refs/heads/master" ]]
}

# Prints a hash of the contents of the given files, for use in a cache key.
hash_of_files() {
  local file
  for file in "$@"; do
    if [[ ! -f "${file}" ]]; then
      echo "Cannot compute cache key, file not found: ${file}" >&2
      return 1
    fi
  done
  cat "$@" | sha256sum | cut -d ' ' -f 1
}

# Sets an output to true or false, telling whether a cache should be saved.
# Arguments: the name of the output, and the cache hit of restoring the cache (true if the cache
# already exists).
decide_about_saving() {
  local output_name="$1"
  local cache_hit="$2"
  if ! is_branch_that_saves_caches; then
    echo "Not saving the cache, because only master saves caches"
    set_output "${output_name}" false
  elif [[ "${cache_hit}" == "true" ]]; then
    echo "Not saving the cache, because it already exists. A cache cannot be overwritten"
    set_output "${output_name}" false
  else
    set_output "${output_name}" true
  fi
}
