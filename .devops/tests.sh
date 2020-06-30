#!/usr/bin/env bash
set -e

mkdir tmp

touch tmp/io.auklet.core.UtilTest.testDeleteQuietly tmp/AukletAuth

gradle test jacocoTestReport

rm -Rf tmp