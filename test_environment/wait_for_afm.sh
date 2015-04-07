#!/bin/bash

until $(curl --output /dev/null --silent --head --fail http://localhost:8080/testing)
do
    sleep 0.5
done
