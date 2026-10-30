#!/usr/bin/env bash
# Prints the highest version of frankframework-webapp that is available in the
# Frank!Framework Nexus releases repository.
#
# The <release> element of maven-metadata.xml is not used, because it holds the
# most recently deployed version, not the highest one. When a nightly build of
# an older branch (e.g. 9.4.x) is deployed after the newest 10.x build,
# <release> points to that older version. Instead, all <version> elements are
# compared with a version sort.
#
# This script also works under MinGW (Git Bash on Windows) for local testing.
# It therefore avoids grep -P, which is not available in every MinGW build, and
# strips carriage returns from the downloaded metadata.
#
# Usage: get-highest-ff-version.sh [<metadata-url>]
set -euo pipefail

metadata_url="${1:-https://nexus.frankframework.org/content/repositories/releases/org/frankframework/frankframework-webapp/maven-metadata.xml}"

metadata=$(curl -sSf "$metadata_url" | tr -d '\r')

version=$(printf '%s\n' "$metadata" \
  | sed -n 's#.*<version>\([^<]*\)</version>.*#\1#p' \
  | sort -V \
  | tail -n 1)

if [ -z "$version" ]; then
  echo "Could not determine highest version of Frank!Framework from Nexus metadata at $metadata_url" >&2
  exit 1
fi

echo "$version"
