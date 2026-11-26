#!/bin/bash

set -e

ladybugCheckout=$(realpath $1)
version=$2

work=${ladybugCheckout}/work

if [ -e ${work} ]; then
    echo "There are results from an earlier run, please clean up"
fi
mkdir ${work}

subprojects="ladybug-common ladybug-backend-jaxrs ladybug-backend-springmvc ladybug-test-webapp ladybug-test-webapp-otel ladybug-spring-boot-test-webapp ladybug-spring-boot-test-webapp-otel"

for subproject in ${subprojects}; do
    resultFile=${work}/${subproject}.txt
    warFile=${ladybugCheckout}/${subproject}/target/${subproject}-${version}.war
    jarFile=${ladybugCheckout}/${subproject}/target/${subproject}-${version}.jar
    echo "Module ${subproject}"
    if [ -e ${warFile} ]; then
        echo "  has war"
        jar tf ${warFile} > ${resultFile}
    elif [ -e ${jarFile} ]; then
        echo "  has jar"
        jar tf ${jarFile} > ${resultFile}
    else
        echo "  ${subproject} does not exist, skipping"
    fi
done
