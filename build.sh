#!/usr/bin/env bash
# --------------------------------------------
# main bash build script for CI, dev and releasing. Used in Makefile
# --------------------------------------------
set -e  # Abort script at first error, when a command exits with non-zero status (except in until or while loops, if-tests, list constructs)

# if build/bin scripts do not exists then clone it
[ ! -e build/bin ] && git clone https://github.com/yakworks/bin.git build/bin -b 2.1 # --single-branch --depth 1}
# .env overrides for local dev, not to be checked in
[ -f .env ] && set -o allexport && source .env && set +o allexport
source build/bin/init_env # main init script

# NOTE: keep build.sh light & simples. create a script with helper functions in a script dir and source it in
# source scripts/build_support.sh


# --- boiler plate function runner, keep at end of file ------
# check if first param is a functions
if declare -f "$1" > /dev/null; then
  init_env # initialize standard environment, reads version.properties, build.yml , etc..
  "$@" #call function with arguments verbatim
else # could be that nothing passed or what was passed is invalid
  [ "$1" ] && echo "'$1' is not a known function name" >&2 && exit 1
fi
