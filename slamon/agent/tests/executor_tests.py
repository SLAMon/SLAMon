import unittest
import logging


logging.basicConfig(
    format='%(thread)d:%(levelname)s:%(message)s',
    level=logging.DEBUG
)


class ExecutorTests(unittest.TestCase):
    def test_unknown_handler(self):
        from slamon.agent.executor import Executor
        from mock import Mock, ANY

        mock_callback = Mock()

        with Executor() as executor:
            executor.submit_task(
                {
                    "task_id": "123e4567-e89b-12d3-a456-426655440000",
                    "task_type": "non-existing-task",
                    "task_version": 1,
                    "task_data": {}
                },
                mock_callback
            )

        mock_callback.assert_called_once_with(
            "123e4567-e89b-12d3-a456-426655440000",
            task_error=ANY
        )

    def test_raising_handler(self):
        from slamon.agent.executor import Executor
        from slamon.agent.handlers import TaskHandler
        from mock import Mock, ANY

        mock_handler = Mock(side_effect=Exception)
        mock_callback = Mock()

        @TaskHandler('raising-task', 1)
        def raising_handler(input_data):
            mock_handler()

        with Executor() as executor:
            executor.submit_task(
                {
                    "task_id": "123e4567-e89b-12d3-a456-426655440000",
                    "task_type": "raising-task",
                    "task_version": 1,
                    "task_data": {}
                },
                mock_callback
            )

        mock_handler.assert_any_call()
        mock_callback.assert_called_once_with(
            "123e4567-e89b-12d3-a456-426655440000",
            task_error=ANY
        )

    def test_simple_handler(self):
        from slamon.agent.executor import Executor
        from slamon.agent.handlers import TaskHandler
        from mock import Mock

        mock_handler = Mock()
        mock_callback = Mock()

        @TaskHandler('test-task', 1)
        def test_handler(input_data):
            mock_handler()
            return input_data

        data = {'data': 1337}

        with Executor() as executor:
            executor.submit_task(
                {
                    "task_id": "123e4567-e89b-12d3-a456-426655440000",
                    "task_type": "test-task",
                    "task_version": 1,
                    "task_data": data
                },
                mock_callback
            )

        mock_handler.assert_any_call()
        mock_callback.assert_called_once_with(
            "123e4567-e89b-12d3-a456-426655440000",
            task_data=data
        )
