#!/bin/bash

pomFilePath=$1
outputPath=$2

grep -F "<frontend.version>" ${pomFilePath} | cut -d '>' -f2 | cut -d '<' -f1 | tr -d '\n' > ${outputPath}