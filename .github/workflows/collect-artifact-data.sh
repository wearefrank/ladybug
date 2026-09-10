#!/usr/bin/env bash
# Collects failure data for a single GitHub Actions upload-artifact step.
#
# Every candidate path is only added to the staging directory if it actually
# exists, so that upload-artifact never has to deal with missing files or
# empty glob matches (which can otherwise produce a corrupt/empty zip). Each
# job therefore ends up with exactly one artifact zip, containing only the
# files this run actually produced.
#
# Candidate paths are staged relative to the working directory (the checkout
# root, which is the shared parent of ladybug, frank-runner and
# frankframework), even when a candidate is passed in as an absolute path
# (e.g. built from `realpath`). Otherwise, an absolute path would be staged
# under its full path, e.g. home/runner/work/ladybug/ladybug/frankframework/...,
# burying the directory several levels deep instead of at the archive root.
#
# On Martijn's computer, .ts files could not be opened quickly from
# a downloaded artifact because .ts files were opened with the Windows media
# player. In Windows Settings you can configure for each file type which app
# should open it.
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
      # $PWD is not set by this script; it is the working directory this
      # script was invoked from. None of the callers of this script override
      # the default working directory, so $PWD is $GITHUB_WORKSPACE, the
      # shared parent of the ladybug, frank-runner and frankframework
      # checkouts.
      rel_match="$(realpath --relative-to="$PWD" "$match")"
      cp -r --parents "$rel_match" "$staging_dir"/
    fi
  done
done
