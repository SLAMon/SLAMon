from slamon.afm.routes.testing import testing_routes  # Shows as unused but is actually required for routes
from slamon.afm.afm_app import app
from slamon.afm.tests.agent_routes_tests import AFMTest
from webtest import TestApp
import jsonschema


class TestDevRoutes(AFMTest):
    task_list_response_schema = {
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
                        'task_type': {
                            'type': 'string'
                        },
                        'task_version': {
                            'type': 'integer'
                        },
                        'task_data': {
                            'type': 'string'
                        }
                    },
                    'required': ['task_id', 'task_type', 'task_version']
                }
            }
        },
        'required': ['tasks'],
        'additionalProperties': False
    }

    @staticmethod
    def test_post_task_non_json():
        test_app = TestApp(app)
        assert test_app.post('/testing/tasks', expect_errors=True).status_int == 400
        assert test_app.post('/testing/tasks/', expect_errors=True).status_int == 400

    @staticmethod
    def test_post_task_empty():
        test_app = TestApp(app)

        assert test_app.post_json('/testing/tasks', {}, expect_errors=True).status_int == 400
        assert test_app.post_json('/testing/tasks/', {}, expect_errors=True).status_int == 400

    @staticmethod
    def test_post_task_invalid():
        test_app = TestApp(app)

        # Invalid type
        assert test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 5,
            'task_version': 1
        }, expect_errors=True).status_int == 400

        # Invalid uuid
        assert test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e541',
            'task_type': 'test-task-1',
            'task_version': 1
        }, expect_errors=True).status_int == 400

        # Invalid version
        assert test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'test-task-1',
            'task_version': 'test version'
        }, expect_errors=True).status_int == 400

    @staticmethod
    def test_post_task():
        test_app = TestApp(app)

        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1
        })

        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546014',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'test-task-1',
            'task_version': 1
        })

    @staticmethod
    def test_post_task_with_data():
        test_app = TestApp(app)

        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1,
            'task_data': {}
        })

        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546014',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'test-task-1',
            'task_version': 1,
            'task_data': {'test': 'value'}
        })

    @staticmethod
    def test_post_task_duplicate():
        test_app = TestApp(app)
        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1
        })

        # Try to post 1st task again
        assert test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'test-task-1',
            'task_version': 1
        }, expect_errors=True).status_int == 400

    @staticmethod
    def test_get_tasks():
        test_app = TestApp(app)
        test_app.post_json('/testing/tasks', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1
        })

        resp = test_app.get('/testing/tasks')
        jsonschema.validate(resp.json, TestDevRoutes.task_list_response_schema)
