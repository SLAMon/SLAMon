#!/bin/bash

cd SLAMon/java
mvn clean install
mvn test -Dtest=SystemTests -pl system_tests/
mvn clean
cd ../..

