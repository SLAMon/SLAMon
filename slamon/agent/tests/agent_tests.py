import unittest
import logging
import json
import uuid
from unittest.mock import Mock
from unittest.mock import patch

import responses
import dateutil.parser

from slamon.agent.communication import FatalError
from slamon.agent.communication import Communicator
from slamon.agent.agent import Agent
from slamon.agent.handlers import TaskHandler
from slamon.agent import timeutil


logging.basicConfig(
    format='%(thread)d:%(levelname)s:%(message)s',
    level=logging.DEBUG
)


class AgentTests(unittest.TestCase):
    def _run_agent_with_responses_callback(self, callback):
        from slamon.agent.agent import Agent

        responses.add_callback(
            responses.POST, 'https://localhost/tasks',
            callback=callback,
            content_type='application/json'
        )

        agent = Agent('https://localhost', default_wait=0)
        agent.run()

    @responses.activate
    def test_exit_on_error(self):

        class CountResponses(object):

            def __init__(self):
                self.counter = 3

            def __call__(self, request):
                self.counter -= 1
                assert self.counter >= 0, 'Agent should exit after HTTP 400 response!'
                if self.counter == 2:
                    return (200, {}, json.dumps({
                        "return_time": "2012-04-23T18:25:43.511Z"
                    }))
                if self.counter == 1:
                    return 500, {}, None
                else:
                    return 400, {}, None

        with self.assertRaises(FatalError):
            self._run_agent_with_responses_callback(CountResponses())

        self.assertEqual(len(responses.calls), 3, 'Agent should have requested for tasks three times!')

    @responses.activate
    def test_required_fields_in_request(self):

        mock_callback = Mock(side_effect=FatalError('Exception to exit the agent ASAP.'))
        with self.assertRaises(FatalError):
            self._run_agent_with_responses_callback(mock_callback)
        self.assertEqual(mock_callback.call_count, 1, 'Agent should have requested for tasks once!')

        request_data = json.loads(mock_callback.call_args[0][0].body)
        self.assertIsInstance(request_data['protocol'], int)
        self.assertIsInstance(request_data['agent_id'], str)
        self.assertIsInstance(request_data['agent_name'], str)
        self.assertIsInstance(request_data['agent_time'], str)
        self.assertIsInstance(request_data['agent_capabilities'], dict)
        self.assertIsInstance(request_data['max_tasks'], int)

        self.assertEqual(len(request_data['agent_id']), 36)
        self.assertIsNotNone(dateutil.parser.parse(request_data['agent_time']).tzinfo)
        for name, data in request_data['agent_capabilities']:
            self.assertIsInstance(data['version'], int)

    def test_runs_task(self):

        class RequestTasksMock(object):
            def __init__(self):
                self.called = False

            def __call__(self, *args, **kwargs):
                if self.called:
                    raise FatalError('Exit agent')
                self.called = True
                return {
                    'return_time': timeutil.format(timeutil.now()),
                    'tasks': [{
                        'task_type': 'test',
                        'task_version': 1,
                        'task_id': uuid.uuid4(),
                        'task_data': {}
                    }]
                }

        with patch.object(Communicator, 'request_tasks', side_effect=RequestTasksMock()):
            mock_handler = Mock(return_value={})
            TaskHandler.register(mock_handler, 'test', 1)

            with self.assertRaises(FatalError):
                Agent('https://localhost', default_wait=0).run()

        self.assertEqual(
            mock_handler.call_count, 1,
            'Agent should have executed task once'
        )
