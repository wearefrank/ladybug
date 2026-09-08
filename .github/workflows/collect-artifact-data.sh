#!/usr/bin/env bash
# Collects failure data for a single GitHub Actions upload-artifact step.
#
# Every candidate path is only added to the staging directory if it actually
# exists, so that upload-artifact never has to deal with missing files or
# empty glob matches (which can otherwise produce a corrupt/empty zip). Each
# job therefore ends up with exactly one artifact zip, containing only the
# files this run actually produced.
#
# Usage: collect-artifact-data.sh <staging-dir> <candidate-path-or-glob> ...
set -euo pipefail
shopt -s nullglob globstar

staging_dir="$1"
shift

mkdir -p "$staging_dir"

for pattern in "$@"; do
  for match in $pattern; do
    if [ -e "$match" ]; then
      cp -r --parents "$match" "$staging_dir"/
    fi
  done
done
