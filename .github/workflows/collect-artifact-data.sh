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
# .ts files are renamed to .ts.txt, since Windows does not associate .ts
# files with a text editor by default, which makes them awkward to open
# from a downloaded artifact.
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

while IFS= read -r -d '' ts_file; do
  mv "$ts_file" "${ts_file%.ts}.ts.txt"
done < <(find "$staging_dir" -type f -name '*.ts' -print0)
