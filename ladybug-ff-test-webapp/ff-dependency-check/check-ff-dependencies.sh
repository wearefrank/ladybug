#!/bin/sh
# Reports dependencies of ladybug-ff-test-webapp that differ from what the Frank!Framework gives
# the same webapp outside the Ladybug project. Fails when a difference is not listed in
# allowed-differences.txt. See CheckFfDependencies.java.
#
# Requires mvn and java (11 or higher) on the path. Run "mvn install" in the root of the Ladybug
# project first, so that the Ladybug artifacts used by the webapp are in the local Maven repository.
# Arguments are passed to Maven, for example -o to work offline.
#
# Works on Linux and with Git Bash / MinGW on Windows.
set -e

cd "$(dirname "$0")"
TREE_GOAL=org.apache.maven.plugins:maven-dependency-plugin:3.7.0:tree
# Relative to the module directory ladybug-ff-test-webapp, which is where Maven resolves outputFile
WORK=target/ff-dependency-check

rm -rf "../$WORK"
mkdir -p "../$WORK/baseline"

echo "Resolving dependencies of ladybug-ff-test-webapp"
mvn -B -q -f ../../pom.xml -pl ladybug-ff-test-webapp "$@" "$TREE_GOAL" "-DoutputFile=$WORK/webapp-tree.txt"

echo "Resolving dependencies of the same webapp without the Ladybug parent pom"
java CheckFfDependencies.java baseline-pom ../pom.xml "../$WORK/webapp-tree.txt" "../$WORK/baseline/pom.xml"
mvn -B -q -f "../$WORK/baseline/pom.xml" "$@" "$TREE_GOAL" -DoutputFile=baseline-tree.txt

java CheckFfDependencies.java compare "../$WORK/webapp-tree.txt" "../$WORK/baseline/baseline-tree.txt" allowed-differences.txt
