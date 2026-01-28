#!/bin/sh

echo "Using JAVA_HOME: $JAVA_HOME"

cd $(dirname $0)/..
./gradlew --version
./gradlew clean generateGradleJdkConfigs baselineUpdateConfig build test shadowJar --info --rerun-tasks
