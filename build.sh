#!/usr/bin/env bash
# --------------------------------------------
# main bash build script for CI, dev and releasing. make calls this too.
# --------------------------------------------

set -e
# if build/bin does not exists then clone the bin scripts
[ ! -e build/bin ] && git clone https://github.com/yakworks/bin.git build/bin --single-branch --depth 1;
# user.env overrides for local dev, not to be checked in
[[ -f user.env ]] && source user.env
# import build_functions.sh which has the the other source imports for functions
source build/bin/all.sh

# default init from yml file
init_from_build_yml "gradle/build.yml"
# echo "PROJECT_NAME $PROJECT_NAME"

# cats key files into a cache-checksum.tmp file for circle to use as key
# change this based on how project is structured
function catKeyFiles {
  cat gradle.properties build.gradle gorm-tools/build.gradle gorm-tools-rest/build.gradle gorm-tools-security/build.gradle rally-domain/build.gradle examples/restify/build.gradle examples/testify/build.gradle > cache-checksum.tmp
}

# compile used for circle
function compile {
  # Downloads Dependencies
  ./gradlew resolveConfigurations --no-daemon
  ./gradlew classes --no-daemon
#  ./gradlew testClasses
#  ./gradlew integrationTestClasses
}

# check used for circle
function check {
  ./gradlew check --max-workers=2
}

# helper/debug function ex: `build.sh logVars test sqlserver`
function logVars {
  # initEnv ${1:-dev} ${2:-mysql}
  for varName in $BUILD_VARS; do
    echo "$varName = ${!varName}"
  done
}


function dock_build_prep {
  rm -rf examples/$1/build/docker
  mkdir -p examples/$1/build/docker
  cp examples/$1/src/deploy/Dockerfile examples/$1/build/docker/
  echo " copied dockerfile"
  cp examples/$1/src/deploy/docker-entrypoint.sh examples/$1/build/docker/
  echo " copied entrypoint"
  #cp -r rootLocation build/docker/
  # un-jar so we can layer the docker
  # see https://blog.jdriven.com/2019/08/layered-spring-boot-docker-images/
  cp examples/$1/build/libs/*.jar examples/$1/build/docker/
  echo " copied jars"
  cd examples/$1/build/docker
  echo " cd to docker"
  #rm $1/*javadoc.jar
  #rm $1/*sources.jar
  jar -xf *.jar
  rm *.jar
  cd -
}


# Setup app name, docker app url, KUB_NAMESPACE etc
# # arg $1 - app name (eg ar-api)
function setupVersion {
  appName="${1}-${VERX_NO_DOTS}"
  APP_NAME="$appName"

  DOCKER_APP_URL="dock9/$1:${VERSION}"
  KUB_NAMESPACE="${1}-demo"
  KUB_INGRESS_URL="$1-demo10.9ci.io"

  setVar APP_NAME "$APP_NAME"
  setVar DOCKER_APP_URL "$DOCKER_APP_URL"
  setVar DOCKER_APP_URL_CURRENT_VERSION "$DOCKER_APP_URL"
  setVar KUB_NAMESPACE "$KUB_NAMESPACE"
  setVar KUB_INGRESS_URL "$KUB_INGRESS_URL"
}

function buildDocker {
  setupVersion $1
  gradle $1:assemble
  dock_build_prep $1
  echo " will build docker"
  docker build -t $DOCKER_APP_URL examples/$1/build/docker/.
  echo " docker built"
}

function runDockerApp {
  # env doesn't matter here so just set it to dev
  #initEnv dev $1
  initEnv dev ${2:-mysql}
  setupVersion $1

  appName="$APP_NAME"
  dockerStart $appName  \
    --network builder-net \
    -p 8080:8080 \
    $DOCKER_APP_URL
}

function dockDeploy {
  setupVersion $1
  docker push ${DOCKER_APP_URL}
}

# runs sed on the kubernetes tpl.yml template files to update and replace variables with values
# arg $1 dbms
# arg $2 the file to apply
function applyTpl {
  # env doesn't matter here so just set it to dev
  initEnv dev $1
  setupVersion ${2%%/*} ## Cut app name eg ar-api from path and setup app name etc.
  local processedTpl=$(sedTplYml $2 "build/kube")
  kubectl apply -f $processedTpl
}


# set build environment
# arg $1 - BUILD_ENV (test, dev, seed)
# arg $2 - DBMS Vendor (sqlserver,mysql, etc)
function initEnv {
  setVar BUILD_ENV ${1:-test}
  setDbEnv $2

  # build and env vars
  setVar DB_NAME rcm_9ci_${BUILD_ENV}
  setVar DB_BAK_NAME "${DB_NAME}.sql"

  # kubernetes
  setVar KUB_DB_SERVICE_NAME "${APP_NAME_BASE}-${DBMS}-${VERX_NO_DOTS}"
}

# --- boiler plate function runner, stay at end of file ------
if declare -f "$1" > /dev/null; then
  "$@" #call function with arguments verbatim
else
  [ "$1" ] && echo "'$1' is not a known function name" >&2 && exit 1
fi
