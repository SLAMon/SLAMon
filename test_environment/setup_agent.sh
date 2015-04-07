#!/bin/bash

virtualenv agent_env
. agent_env/bin/activate
pip install -r requirements_agent.txt
deactivate
