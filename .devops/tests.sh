#!/usr/bin/env bash

mkdir tmp

touch tmp/io.auklet.core.UtilTest.testDeleteQuietly tmp/AukletAuth

gradle test jacocoTestReport

rm -Rf tmp