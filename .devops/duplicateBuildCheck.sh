#!/bin/bash
set -e
if [[ ! -f ~/.localCircleBuild && ! -f ~/.prCircleBuild ]]; then
  touch $ACTIVE_BUILD_FILE
  ACTIVE_BUILD_NUM=$(cat $ACTIVE_BUILD_FILE)
  if [[ "$ACTIVE_BUILD_NUM" == '' ]]; then
    echo $CIRCLE_BUILD_NUM > $ACTIVE_BUILD_FILE
  elif (( $CIRCLE_BUILD_NUM < $ACTIVE_BUILD_NUM )); then
    echo 'WARNING: there is a newer CircleCI build for this commit hash. This build will now abort.'
    exit 1
  else
    echo $CIRCLE_BUILD_NUM > $ACTIVE_BUILD_FILE
  fi
fi
