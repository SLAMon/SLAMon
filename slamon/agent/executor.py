from concurrent.futures import ThreadPoolExecutor
import logging
import threading

from slamon.agent.handlers import TaskHandler


class Executor(object):
    """
    Task executor pool to run task handlers asynchronously.
    To provide clean shutdown, Executor is a python context manager and
    performs shutdown routines on context exit.

    Example:
        Executing tasks in existing list ``task_list`` with two concurrent executors:

            from slamon.agent.executor import Executor

            def result_callback(task_id, task_data=None, task_error=none):
                if task_data:
                    print('Task {0} finished with data: {1}'.format(task_id, task_data))
                else:
                    print('Task {0} finished with error: {1}'.format(task_id, task_error))

            with Executor(max_executors=2) as executor:
                for task in task_list:
                    executor.submit_task(task, result_callback)

    """

    def __init__(self, max_executors=2):
        self._max_executors = max_executors
        self._active_executors = 0
        self._lock = threading.Lock()
        self._logger = logging.getLogger('Executor')

    def __enter__(self):
        """
        Context manager entry function to start thread pool.
        """
        self._logger.debug('Starting executor with %d threads', self._max_executors)
        self._thread_pool = ThreadPoolExecutor(self._max_executors)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """
        Context manager exit function to shutdown thread pool.
        """
        self._logger.debug('Stopping executor')
        self._thread_pool.shutdown()
        self._thread_pool = None

    def _run_task(self, task, callback):
        try:
            self._logger.debug('Creating task handler for task %s', task['task_id'])
            handler = TaskHandler.create(task['task_type'], task['task_version'])

            self._logger.debug('Executing task handler for task %s', task['task_id'])
            result = handler(task['task_data'])

            self._logger.debug('Task executed successfully: %s', task['task_id'])
            callback(task['task_id'], task_data=result)

        except Exception as e:
            self._logger.warning('Execution of task %s raised an exception: %s', task['task_id'], str(e))
            callback(task['task_id'], task_error=str(e))

        finally:
            with self._lock:
                self._active_executors -= 1

    def submit_task(self, task, callback):
        """
        Queue task for asynchronous execution. The callback will receive
        task id as first positional parameter and either task_data or task_error
        named parameter describing the output.

        :param task: dictionary describing the task
        :param callback: callable that will receive results
        """
        with self._lock:
            self._thread_pool.submit(lambda: self._run_task(task, callback))
            self._active_executors += 1

    def available_executors(self):
        """
        Get number of available executors. (max executors - active executors)

        :return: number of available executors
        """
        with self._lock:
            return max(self._max_executors - self._active_executors, 0)
