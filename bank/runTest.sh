#!/usr/bin/env bash
./compile.sh
cd ../../../../../../out/production/Bank/
java -jar ../../../lib/junit-platform-console-standalone-1.6.2.jar -cp . -c ru.ifmo.rain.akimov.bank.BankTests