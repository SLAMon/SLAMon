#!/bin/bash

cd SLAMon
if [ -d "env" ]
then
    rm -rf env
fi
virtualenv env
. env/bin/activate
pip install -r requirements_afm.txt
pip install -r requirements_agent.txt
export PYTHONPATH=`pwd`
nosetests
EXIT_CODE=$?
deactivate
rm -r env
cd ..
exit $EXIT_CODE
