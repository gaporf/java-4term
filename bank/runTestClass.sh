#!/usr/bin/env bash
./compile.sh
cd ../../../../../../out/production/Bank/
cp ../../../lib/junit-platform-console-standalone-1.6.2.jar .
jar xf junit-platform-console-standalone-1.6.2.jar
java ru.ifmo.rain.akimov.bank.RunTests