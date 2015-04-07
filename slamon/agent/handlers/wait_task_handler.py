from slamon.agent.handlers import TaskHandler


@TaskHandler("wait", 1)
def wait_task_handler(input_params):
    """
    Agent for prototyping. Will wait for approximately the specified
    timeout, with slight random factor.
    """
    import time
    import random

    timeout = float(input_params['time']) - 0.5 + random.random()
    time.sleep(timeout)
    return {'time': timeout}
