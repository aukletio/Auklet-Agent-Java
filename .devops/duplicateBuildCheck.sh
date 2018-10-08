#!/bin/bash
if [[ ! -f ~/.localCircleBuild && ! -f ~/.prCircleBuild ]]; then
  set +e
  dpkg -l awscli > /dev/null 2>&1
  RESULT=$?
  set -e
  if [[ "$RESULT" != "0" ]]; then
    echo 'Installing AWS CLI...'
    sudo apt -y install awscli > /dev/null 2>&1
  fi
  BUILD_STATUS_PATH="s3://$BUILD_STATUS_BUCKET/$CIRCLE_PROJECT_USERNAME/$CIRCLE_PROJECT_REPONAME/$CIRCLE_SHA1"
  if [[ "$1" == 'done' ]]; then
    echo 'DONE' | aws s3 cp - $BUILD_STATUS_PATH
  else
    ACTIVE_BUILD_NUM=$(aws s3 cp $BUILD_STATUS_PATH - 2>/dev/null)
    if [[ "$ACTIVE_BUILD_NUM" == '' ]]; then
      echo $CIRCLE_BUILD_NUM | aws s3 cp - $BUILD_STATUS_PATH
    elif [[ "$ACTIVE_BUILD_NUM" == 'DONE' ]]; then
      echo 'WARNING: a CircleCI build has already completed for this commit hash. This build will now abort.'
      exit 1
    elif (( $CIRCLE_BUILD_NUM < $ACTIVE_BUILD_NUM )); then
      echo 'WARNING: there is a newer CircleCI build for this commit hash. This build will now abort.'
      exit 1
    else
      echo $CIRCLE_BUILD_NUM | aws s3 cp - $BUILD_STATUS_PATH
    fi
  fi
else
  echo 'This is a local/PR CircleCI build; skipping duplicate build check.'
fi