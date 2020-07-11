#!/usr/bin/env bash
cd ../../../../../
pp=${PWD##*/}
cd ..
rm -r out 2>/dev/null
mkdir out
cd out/
mkdir production
cd production/
mkdir Bank
cd Bank
cd ../../../
cd $pp
cd ru/ifmo/rain/akimov/bank
javac -classpath ../../../../../../lib/junit-platform-console-standalone-1.6.2.jar -d ../../../../../../out/production/Bank/ *.java
