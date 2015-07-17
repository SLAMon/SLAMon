#!/bin/bash -e
bash ${SLAMON_DIR}/test_environment/setup_database.sh
cd ${SLAMON_DIR} && nosetests
cd ${SLAMON_DIR} && ./gradlew test -Dtest=SystemTests -p java/system_tests/
