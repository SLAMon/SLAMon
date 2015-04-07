#!/bin/bash

if [ -d "afm_env" ]
then
	. afm_env/bin/activate
	export PYTHONPATH=`pwd`
	trap 'kill -TERM $PID' TERM INT # Java's process.destroy doesn't kill subprocess so trap TERM and INT to propagate it properly
	python slamon/afm/afm.py &
	PID=$!
	wait $PID
	trap - TERM INT
	wait $PID
	deactivate
else
	exit -1
fi
