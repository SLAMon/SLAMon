import logging
import uuid
import time
import argparse
import importlib

from slamon.agent import timeutil
from slamon.agent.executor import Executor
from slamon.agent.communication import TemporaryError
from slamon.agent.communication import Communicator
from slamon.agent.handlers import TaskHandler


class Agent(object):
    """
    Agent class presents an instance of agent application.
    """

    def __init__(self, afm_url, default_wait=60, max_tasks=2, name='Python Agent 1.0', agent_uuid=None):

        self.afm = Communicator(afm_url)
        self.max_tasks = max_tasks
        self.default_wait = default_wait
        self.name = name
        self.uuid = agent_uuid if agent_uuid else str(uuid.uuid1())
        self._run = True

    def exit(self):
        """
        Signal agent to exit. After issuing exit, agent will not make further task requests,
        but will wait until all currently processed tasks finish.
        """
        self._run = False

    def run(self):
        """
        The "main function" of the agent, looping the claim & execute tasks flow.
        """

        with Executor(self.max_tasks) as executor:
            while self._run:

                wait_time = self.default_wait
                min_wait_time = 1

                # request for tasks
                try:
                    task_response = self.afm.request_tasks(
                        agent_id=self.uuid,
                        agent_name=self.name,
                        agent_time=timeutil.format(timeutil.now()),
                        agent_capabilities=TaskHandler.list_all(),
                        max_tasks=executor.available_executors()
                    )
                    if 'tasks' in task_response:
                        for task_data in task_response['tasks']:
                            executor.submit_task(task_data,
                                                 lambda *args, **kwargs: self.afm.post_result(*args, **kwargs))
                    if 'return_time' in task_response:
                        return_time = timeutil.parse(task_response['return_time'])
                        wait_time = max(min_wait_time, (return_time - timeutil.now()).total_seconds())
                except TemporaryError as e:
                    logging.getLogger("Agent").error("An error occurred while claiming tasks: %s", e)

                time.sleep(wait_time)


def _import_module(module_name, package=None):
    """
    Recursively load modules to search for task handlers.
    """
    m = importlib.import_module(module_name, package=package)
    if hasattr(m, '__all__'):
        for sub_module_name in m.__all__:
            _import_module('.' + sub_module_name, module_name)


def main():
    """
    Entry point for the agent script.
    """
    logging.basicConfig(level=logging.DEBUG)

    parser = argparse.ArgumentParser()
    parser.add_argument('-u', '--url', action='store', required=True,
                        help='Coordinator URL')
    parser.add_argument('-l', '--load', action='append',
                        help='Load handlers from specified module')
    parser.add_argument('-w', '--default-wait', type=int, default=60,
                        help='Seconds to wait before reconnection after connection failure.')
    parser.add_argument('-x', '--num-executors', type=int, default=2,
                        help='Number of concurrent task executors.')
    args = parser.parse_args()

    # import defined modules to load defined handlers
    if args.load:
        for module in args.load:
            _import_module(module)

    agent = Agent(
        args.url,
        default_wait=args.default_wait,
        max_tasks=args.num_executors
    )
    agent.run()
    return 0


if __name__ == "__main__":
    main()
