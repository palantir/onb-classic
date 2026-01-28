#!/bin/bash
set -e -x

if [ $# -ne 1 ]; then
    echo "Usage: $0 <TYPE>"
    exit 1
fi

GIT_COMMIT=$(git rev-parse --short HEAD)
CACHE_DIR=~/assets-cache
cd $CACHE_DIR

onbVersion=$(cat onbversion.txt)
SNAPSHOT_VERSION="${onbVersion}-g${GIT_COMMIT}"

TYPE=$1

if [ "$TYPE" == "release" ]; then
    SNAPSHOT_VERSION=$onbVersion
fi

URL="https://artifactory.PUBLIC_URL_NEEDED/artifactory/internal-jar-${TYPE}/com/palantir/onb-classic/onb-classic/${SNAPSHOT_VERSION}/onb-classic-${SNAPSHOT_VERSION}.jar"
echo Publishing to ${URL} from ${CACHE_DIR}/onb-classic-all.jar
curl --silent --show-error --fail --user "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD}" --request PUT ${URL} --upload-file ${CACHE_DIR}/onb-classic-all.jar
