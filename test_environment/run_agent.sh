#!/bin/bash

if [ -d "agent_env" ]
then
	. agent_env/bin/activate
	export PYTHONPATH=`pwd`
	trap 'kill -TERM $PID' TERM INT # Java's process.destroy doesn't kill subprocess properly so trap signals and propagate
	python slamon/agent/agent.py &
	PID=$!
	wait $PID
	trap - TERM INT
	wait $PID
	deactivate
else
	exit -1
fi
