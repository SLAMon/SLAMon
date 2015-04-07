from slamon.agent.handlers import *
import unittest
import responses

from slamon.agent.handlers import *
from slamon.agent.handlers import TaskHandler


def register_1():
    from slamon.agent.handlers import TaskHandler

    @TaskHandler('test-1', 1)
    def test_handler(input):
        return input


def register_2():
    import slamon.agent

    @slamon.agent.handlers.TaskHandler('test-2', 1)
    def test_handler(input):
        return input


class TaskHandlerTests(unittest.TestCase):
    def test_dynamic_handler_registration(self):
        builtin_count = len(TaskHandler.list_all())
        self.assertGreaterEqual(
            builtin_count, 1,
            'There should be at least single built in tasks defined.'
        )
        register_1()
        register_2()
        self.assertEqual(
            len(TaskHandler.list_all()), builtin_count + 2,
            'There should be more than two new tasks defined.'
        )

    @responses.activate
    def test_url_http_status_valid(self):
        responses.add(responses.GET, 'http://test.url.whatever', body="")
        responses.add(responses.GET, 'http://test.url.something_else', status=404, body="")

        assert(url_http_status_task_handler.url_http_status_task_handler(
            input_params={'url': 'http://test.url.whatever'})['status'] == 200)
        assert(url_http_status_task_handler.url_http_status_task_handler(
            input_params={'url': 'http://test.url.something_else'})['status'] == 404)

    def test_url_http_status_invalid(self):
        with self.assertRaises(Exception):
            url_http_status_task_handler(input_params={'url': 'wtf.derp'})
