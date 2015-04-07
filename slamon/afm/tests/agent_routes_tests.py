from slamon.afm.routes import agent_routes  # Shows as unused but is actually required for routes
from slamon.afm.afm_app import app
from slamon.afm.tables import Agent, AgentCapability, Task
from slamon.afm.database import create_session
from slamon.afm.tests.afm_test import AFMTest
from datetime import datetime
from webtest import TestApp
import jsonschema


class TestPolling(AFMTest):
    task_request_response_schema = {
        'type': 'object',
        'properties': {
            'tasks': {
                'type': 'array',
                'items': {
                    'type': 'object',
                    'properties': {
                        'task_id': {
                            'type': 'string',
                            'pattern': '^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$'
                        },
                        'task_type': {'type': 'string'},
                        'task_version': {'type': 'integer'},
                        'task_data': {'type': 'object'}
                    },
                    'required': ['task_id', 'task_type', 'task_version', 'task_data'],
                    'additionalProperties': False
                }
            },
            'return_time': {
                'type': 'string'
            }
        },
        'required': ['tasks', 'return_time'],
        'additionalProperties': False
    }

    @staticmethod
    def test_poll_tasks_non_json():
        test_app = TestApp(app)

        # Test a non-JSON request
        assert test_app.post('/tasks', expect_errors=True).status_int == 400

    @staticmethod
    def test_poll_tasks_empty():
        test_app = TestApp(app)

        assert test_app.post_json('/tasks', {}, expect_errors=True).status_int == 400
        assert test_app.post_json('/tasks/', {}, expect_errors=True).status_int == 400

    @staticmethod
    def test_poll_tasks():
        test_app = TestApp(app)

        resp = test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 5
        })

        jsonschema.validate(resp.json, TestPolling.task_request_response_schema)

        test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 5
        })

    @staticmethod
    def test_poll_task_capability_change():
        test_app = TestApp(app)

        test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 5
        })

        test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2},
                'task-type-3': {'version': 3},
                'task-type-4': {'version': 4}
            },
            'max_tasks': 5
        })

        session = create_session()
        agent = session.query(Agent).filter(Agent.uuid == 'de305d54-75b4-431b-adb2-eb6b9e546013').one()

        try:
            session.query(AgentCapability).filter(AgentCapability.agent_uuid == agent.uuid).\
                filter(AgentCapability.type == 'task-type-1').one()
            session.query(AgentCapability).filter(AgentCapability.agent_uuid == agent.uuid).\
                filter(AgentCapability.type == 'task-type-2').one()
            session.query(AgentCapability).filter(AgentCapability.agent_uuid == agent.uuid).\
                filter(AgentCapability.type == 'task-type-3').one()
            session.query(AgentCapability).filter(AgentCapability.agent_uuid == agent.uuid).\
                filter(AgentCapability.type == 'task-type-4').one()
        finally:
            session.close()

    @staticmethod
    def test_poll_tasks_invalid_data():
        test_app = TestApp(app)

        # Invalid protocol
        assert test_app.post_json('/tasks', {
            'protocol': 'invalid',
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 1
        }, expect_errors=True).status_int == 400

        # Another invalid protocol - so far only protocol version 1 is supported
        assert test_app.post_json('/tasks', {
            'protocol': 5,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 1
        }, expect_errors=True).status_int == 400

        # Invalid agent_id
        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'invalid_agent',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 1
        }, expect_errors=True).status_int == 400

        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 'many_tasks'
        }, expect_errors=True).status_int == 400

        # Extra fields
        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 'many_tasks',
            'extra_field': 1234
        }, expect_errors=True).status_int == 400

        # Extra fields
        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18',
                'somewhere': 'else'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 'many_tasks'
        }, expect_errors=True).status_int == 400

    @staticmethod
    def test_poll_tasks_missing_data():
        test_app = TestApp(app)

        # Missing max_tasks
        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            }
        }, expect_errors=True).status_int == 400

        # Missing agent_id
        assert test_app.post_json('/tasks', {
            'protocol': 1,
            'agent_name': 'Agent 007',
            'agent_location': {
                'country': 'FI',
                'region': '18'
            },
            'agent_time': '2012-04-23T18:25:43.511Z',
            'agent_capabilities': {
                'task-type-1': {'version': 1},
                'task-type-2': {'version': 2}
            },
            'max_tasks': 5
        }, expect_errors=True).status_int == 400


class TestPushing(AFMTest):
    @staticmethod
    def test_push_response_non_json():
        test_app = TestApp(app)

        assert test_app.post('/tasks/response', expect_errors=True).status_int == 400
        assert test_app.post('/tasks/response/', expect_errors=True).status_int == 400

    @staticmethod
    def test_push_response_empty():
        test_app = TestApp(app)

        assert test_app.post_json('/tasks/response', {}, expect_errors=True).status_int == 400
        assert test_app.post_json('/tasks/response/', {}, expect_errors=True).status_int == 400

    @staticmethod
    def test_push_response():
        test_app = TestApp(app)
        session = create_session()
        task = Task()
        task.uuid = 'de305d54-75b4-431b-adb2-eb6b9e546013'
        task.test_id = 'de305d54-75b4-431b-adb2-eb6b9e546013'
        task.claimed = datetime.utcnow()
        session.add(task)
        session.commit()
        test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_data': {
                'key': 'value',
                'another_key': 5
            }
        })

        session = create_session()
        task = session.query(Task).filter(Task.uuid == 'de305d54-75b4-431b-adb2-eb6b9e546013').one()

        assert task.completed is not None
        assert task.result_data is not None
        assert task.failed is None
        assert task.error is None

        task.completed = None
        task.result_data = None
        session.commit()
        test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_error': 'Something went terribly wrong'
        })

        session = create_session()
        task = session.query(Task).filter(Task.uuid == 'de305d54-75b4-431b-adb2-eb6b9e546013').one()

        assert task.completed is None
        assert task.result_data is None
        assert task.failed is not None
        assert task.error is not None

        session.close()

    @staticmethod
    def test_push_response_invalid():
        test_app = TestApp(app)

        # Invalid task id
        assert test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 5,
            'task_data': {
                'key': 'value',
                'another_key': 5
            }
        }, expect_errors=True).status_int == 400

        # Missing data and error
        assert test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546012'
        }, expect_errors=True).status_int == 400

        # Wrong type for error
        assert test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_error': 5
        }, expect_errors=True).status_int == 400

        # Task that doesn't exist
        assert test_app.post_json('/tasks/response', {
            'protocol': 1,
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_data': {
                'key': 'value',
                'another_key': 5
            }
        }, expect_errors=True).status_int == 400
