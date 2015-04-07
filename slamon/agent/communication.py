import logging


class TemporaryError(Exception):
    """
    Exception class to indicate there was recoverable, temporary error
    while communicating with the AFM.
    """
    def __init__(self, what):
        self.what = what

    def __str__(self):
        return str(self.what)


class FatalError(Exception):
    """
    Exception class to indicate there was unrecoverable error, most likely
    due to discontinued legacy protocol support.
    """
    def __init__(self, what):
        self.what = what

    def __str__(self):
        return str(self.what)


class Communicator(object):
    """
    Class to handle afm communication and to provide
    simple error handling interface for the agent application.
    """

    def __init__(self, afm_url):
        self.logger = logging.getLogger("Communicator")
        self.afm_url = afm_url

    def _post(self, path, json):
        """
        Utility to simplify requests to afm and handle (translate) exceptions
        :param path:
        :param json:
        :return:
        """
        import requests

        try:
            r = requests.post(
                self.afm_url + path,
                json=json
            )
            if 400 <= r.status_code < 500:
                raise FatalError('AFM responded with client error status: {0}'.format(r.status_code))
            elif 500 <= r.status_code < 600:
                raise TemporaryError('AFM responded with server error status: {0}'.format(r.status_code))
            return r
        except requests.RequestException as e:
            raise TemporaryError(e)

    def request_tasks(self, **kwargs):
        """
        Request tasks from the afm.

        :param kwargs: Named parameters to pass in task request JSON.
        :return: parsed response JSON
        """

        request_data = {
            "protocol": 1
        }
        request_data.update(kwargs)

        logging.getLogger('HTTP').debug("Requesting for tasks: %s", request_data)
        r = self._post('/tasks', json=request_data)

        try:
            response_data = r.json()
            return response_data
        except Exception as e:
            raise FatalError('Failed to parse response JSON: {0}'.format(e))

    def post_result(self, task_id, task_data=None, task_error=None):
        """
        Post task result to the afm.
        Will handle temporary errors and retry post attempts until succeeds.

        :param task_id: id of the related task
        :param task_data: result data of the task if execution succeeded
        :param task_error: error message when task execution failed
        """

        request_data = {
            "protocol": 1,
            "task_id": task_id
        }
        if task_data:
            request_data["task_data"] = task_data
        elif task_error:
            request_data["task_error"] = task_error

        logging.getLogger('HTTP').debug("Posting results for task '%s': %s", task_id, request_data)

        uploaded = False
        while not uploaded:
            try:
                uploaded = self._post('/tasks/response', json=request_data).status_code == 200
            except TemporaryError as e:
                self.logger.warning("Temporary error while posting results: %s", e)
