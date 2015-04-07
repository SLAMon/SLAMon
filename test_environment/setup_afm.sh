#!/bin/bash

virtualenv afm_env
. afm_env/bin/activate
pip install -r requirements_afm.txt
export PYTHONPATH=`pwd`
python ./slamon/afm/admin.py --drop-tables
python ./slamon/afm/admin.py --create-tables
touch setup_done
deactivate
