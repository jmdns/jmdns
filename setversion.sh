#!/bin/sh

mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=$1
cd p2
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=$1
cd ..
