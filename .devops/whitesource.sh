#!/bin/bash
set -e

echo 'Downloading WhiteSource agent...'
WS_AGENT_URL='https://s3.amazonaws.com/unified-agent/wss-unified-agent-18.12.2.jar'
WS_AGENT='whitesource.jar'
curl -Ls $WS_AGENT_URL > $WS_AGENT

WS_CONFIG_SRC='whitesource.cfg'
WS_CONFIG='whitesource-final.cfg'
cp $WS_CONFIG_SRC $WS_CONFIG
echo "apiKey=$WHITESOURCE_ORG_TOKEN" >> $WS_CONFIG
echo "productToken=$WHITESOURCE_PRODUCT_TOKEN" >> $WS_CONFIG
VERSION=$(cat ~/.version)
echo "projectVersion=$VERSION" >> $WS_CONFIG

echo 'Starting WhiteSource agent...'
$JAVA_HOME/bin/java -jar $WS_AGENT -c $WS_CONFIG
RESULT=$?
# TODO
# Add failure logic where applicable.
# Success=0, Error=-1, Policy Violation=-2, Client Failure=-3, Connection Failure=-4

rm $WS_AGENT
rm $WS_CONFIG
