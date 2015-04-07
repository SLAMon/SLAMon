SLAMon Agent
============

Prerequisites
-------------

Required libraries for running the agent:

    $ pip install requests
    $ pip install python-dateutils

Required libraries for running the tests:

    $ pip install responses
    
Installing the agent
--------------------

setup.py in project root is currently set up installing only the agent. To easily install 
the agent with all required dependencies and the *slamon-agent* command line utility, use e.g. pip:

    $ pip install path/to/SLAmon

Running the agent
-----------------

Currently there is two ways for running the agent: a command line script and embedding agent in your own script.

### Running the agent using command line script ###

*slamon-agent* command line utility is the simplest way running the agent. AFM Url, concurrent executor count and
modules to load handlers from are defined as command line arguments as follows:

    $ slamon-agent --help
    usage: slamon-agent-script.py [-h] -u URL [-l LOAD] [-w DEFAULT_WAIT]
                                  [-x NUM_EXECUTORS]

    optional arguments:
      -h, --help            show this help message and exit
      -u URL, --url URL     Coordinator URL
      -l LOAD, --load LOAD  Load handlers from specified module
      -w DEFAULT_WAIT, --default-wait DEFAULT_WAIT
                            Seconds to wait before reconnection after connection
                            failure.
      -x NUM_EXECUTORS, --num-executors NUM_EXECUTORS
                            Number of concurrent task executors.

### Running the agent from python script ###

```python
from slamon.agent.handlers import *
from slamon.agent import Agent
agent = Agent('http://localhost:8080')
agent.run()
```

Note that when running agent in a script, you need to explicitly import
the modules you want to load task handlers from:

```python
from my_custom_handler_package import *
import my_custom_handler_module

from slamon.agent import Agent
agent = Agent('http://localhost:8080')
agent.run()
```


Developing task handlers
------------------------

Developing task handler is attempted to make as easy as possible. The agent
core application will take care of agent - AFM communication, so that
the task handler can be AFM agnostic and purely focus on just
executing the task.
 
Task handlers are simple functions that take input data as dictionary parameter,
do their work synchronously and return response data as a dictionary.

Task handlers are registered using `@TaskHandler` decorator.

```python
from slamon.agent.handlers import TaskHandler

@TaskHandler("wait", 1)
def wait_task_handler(input_params):
   import time, random
   timeout = float(input_params['time']) - 0.5 + random.random()
   return {'time': timeout}
```