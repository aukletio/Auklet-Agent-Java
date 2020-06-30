#!/bin/bash
set -e
CIRCLE_LOCAL_BUILD=$1

if [[ "$CIRCLE_LOCAL_BUILD" == 'false' ]]; then
  curl -L https://codeclimate.com/downloads/test-reporter/test-reporter-latest-linux-amd64 > cc-test-reporter
  chmod +x cc-test-reporter
  ./cc-test-reporter before-build
fi

bash .devops/tests.sh

if [[ "$CIRCLE_LOCAL_BUILD" == 'false' ]]; then
  # The test reporter will throw a HTTP 409 error if we rebuild in circle because
  # a test report was already posted for that commit. The below check mitigates this.
  export CC_TEST_REPORTER_ID=c6075c2dabb85c477addbae6616cf72773be68603af742860f40894d0b103c2e
  # Set -e is disabled momentarily to be able to output the error message to log.txt file.
  set +e
  JACOCO_SOURCE_PATH=src/main/java \
    ./cc-test-reporter format-coverage ./build/reports/jacoco/test/jacocoTestReport.xml --input-type jacoco
  ./cc-test-reporter upload-coverage 2>&1 | tee exit_message.txt
  result=$?
  set -e
  # Then we check the third line and see if it contains the known error message
  # and print an error message of our own but let the build succeed.
  if [ "$(echo `sed -n '2p' exit_message.txt` | cut -d ' ' -f1-5)" = "HTTP 409: A test report" ]; then
    echo "A test report has already been created for this commit; this build will proceed without updating test coverage data in Code Climate."
    exit 0
  else
    exit $result
  fi
fi