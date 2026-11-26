#!/bin/bash

set -e

expectedVersion=$1

shift 1

function checkSingleClassFile() {
    classFile=$1
    javap -v ${classFile} | awk '/major version/ {print $3}' > classVersion.txt
    if [[ $(cat classVersion.txt) -ne ${expectedVersion} ]]; then
        echo "Class ${classFile} has version $(cat classVersion.txt), expected ${expectedVersion}"
        exit 1
    fi
}

for item in $@; do
    if [ ! -e ${item} ]; then
        echo "${item} does not exist"
        exit 1
    fi
    if [ -d ${item} ]; then
        pushd ${item}/target/classes
        for classFile in $(find . -name '*.class'); do
            echo "Checking ${classFile}"
            checkSingleClassFile ${classFile}
            rm classVersion.txt
        done
        popd
    else
        checkSingleClassFile $item
        rm -rf classVersion.txt
    fi
done
