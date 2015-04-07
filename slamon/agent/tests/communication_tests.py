import unittest
import logging
import json
import responses

from slamon.agent.communication import Communicator, FatalError, TemporaryError

logging.basicConfig(
    format='%(thread)d:%(levelname)s:%(message)s',
    level=logging.DEBUG
)


def _add_responses(url='https://localhost', body="", status=200):
    responses.add(
        responses.POST, '{0}/tasks'.format(url),
        body=body,
        status=status,
        content_type='application/json'
    )
    responses.add(
        responses.POST, '{0}/tasks/response'.format(url),
        body=body,
        status=status,
        content_type='application/json'
    )


class CommunicatorTests(unittest.TestCase):
    def setUp(self):
        self.communicator = Communicator('https://localhost')

    @responses.activate
    def test_server_error(self):
        _add_responses(status=500)
        with self.assertRaises(TemporaryError):
            self.communicator.request_tasks()

    @responses.activate
    def test_client_error(self):
        _add_responses(status=400)
        with self.assertRaises(FatalError):
            self.communicator.request_tasks()

    @responses.activate
    def test_json_error(self):
        _add_responses(body="[asd")
        with self.assertRaises(FatalError):
            self.communicator.request_tasks()

    @responses.activate
    def test_connection_error(self):
        _add_responses(url='https://localhost2/tasks')
        with self.assertRaises(TemporaryError):
            self.communicator.request_tasks()

    @responses.activate
    def test_valid_json(self):
        _add_responses(
            body=json.dumps({
                "tasks": [
                    {
                        "task_id": "123e4567-e89b-12d3-a456-426655440000",
                        "task_type": "wait",
                        "task_version": 1,
                        "task_data": {
                            "time": "1"
                        }
                    }
                ],
                "return_time": "2012-04-23T18:25:43.511Z"
            })
        )
        self.assertIsInstance(self.communicator.request_tasks(), dict)

    @responses.activate
    def test_result_retries(self):
        class CountResponses(object):
            def __init__(self):
                self.counter = 0

            def __call__(self, request):
                self.counter += 1
                assert self.counter <= 2, 'Agent should stop retrying after OK response!'
                if self.counter < 2:
                    return 500, {}, None
                else:
                    return 200, {}, None

        responses.add_callback(
            responses.POST, 'https://localhost/tasks/response',
            callback=CountResponses(),
            content_type='application/json'
        )

        self.communicator.post_result('task-id', task_data={})
        self.assertEqual(len(responses.calls), 2, 'Agent should retry result upload on temporary error!')
