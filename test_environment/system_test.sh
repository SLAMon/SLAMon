#!/bin/bash

cd SLAMon/java
mvn clean install
mvn test -Dtest=SystemTests -pl system_tests/
EXIT_CODE=$?
mvn clean
cd ../..
exit $EXIT_CODE
