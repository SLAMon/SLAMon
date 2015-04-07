from slamon.afm.routes import bpms_routes  # Shows as unused but is actually required for routes
from slamon.afm.afm_app import app
from slamon.afm.tables import Task
from slamon.afm.tests.afm_test import AFMTest
from slamon.afm.database import create_session
from datetime import datetime
from webtest import TestApp
import json


class TestBMPSRoutes(AFMTest):
    @staticmethod
    def test_post_task():
        test_app = TestApp(app)

        test_app.post_json('/task', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1,
            'task_data': {
                'wait_time': 3600
            }
        })

        # Test without data
        test_app.post_json('/task', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546014',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1
        })

    @staticmethod
    def test_post_task_invalid():
        test_app = TestApp(app)

        assert test_app.post_json('/task', expect_errors=True).status_int == 400

        assert test_app.post_json('/task', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 'invalid_version',
            'task_data': {
                'wait_time': 3600
            }
        }, expect_errors=True).status_int == 400

        assert test_app.post_json('/task', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013_not_valid',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1,
            'task_data': {
                'wait_time': 3600
            }
        }, expect_errors=True).status_int == 400

        assert test_app.post_json('/task', {
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1,
            'task_data': {
                'wait_time': 3600
            }
        }, expect_errors=True).status_int == 400

    @staticmethod
    def test_post_task_duplicate():
        test_app = TestApp(app)

        session = create_session()
        task1 = Task()
        task1.uuid = 'de305d54-75b4-431b-adb2-eb6b9e546018'
        task1.test_id = 'de305d54-75b4-431b-adb2-eb6b9e546018'
        task1.claimed = datetime.utcnow()
        task1.data = json.dumps({'wait_time': 123})
        session.add(task1)
        session.commit()
        session.close()

        assert test_app.post_json('/task', {
            'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546018',
            'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',
            'task_type': 'wait',
            'task_version': 1
        }, expect_errors=True).status_int == 400

    @staticmethod
    def test_pull_task():
        test_app = TestApp(app)

        session = create_session()
        task1 = Task()
        task1.uuid = 'de305d54-75b4-431b-adb2-eb6b9e546018'
        task1.test_id = 'de305d54-75b4-431b-adb2-eb6b9e546018'
        task1.claimed = datetime.utcnow()
        task1.data = json.dumps({'wait_time': 123})
        session.add(task1)

        task2 = Task()
        task2.uuid = 'de305d54-75b4-431b-adb2-eb6b9e546019'
        task2.test_id = 'de305d54-75b4-431b-adb2-eb6b9e546019'
        task2.claimed = datetime.utcnow()
        task2.completed = datetime.utcnow()
        task2.result_data = json.dumps({'result': 'epic success'})
        session.add(task2)

        task3 = Task()
        task3.uuid = 'de305d54-75b4-431b-adb2-eb6b9e546020'
        task3.test_id = 'de305d54-75b4-431b-adb2-eb6b9e546020'
        task3.claimed = datetime.utcnow()
        task3.failed = datetime.utcnow()
        task3.error = 'unknown error'
        session.add(task3)

        session.commit()

        test_app.get('/task/de305d54-75b4-431b-adb2-eb6b9e546018')
        test_app.get('/task/de305d54-75b4-431b-adb2-eb6b9e546019')
        test_app.get('/task/de305d54-75b4-431b-adb2-eb6b9e546020')

    @staticmethod
    def test_pull_task_invalid():
        test_app = TestApp(app)

        assert test_app.get('/task/de305d54-75b4-431b-adb2-eb6b9e546013', expect_errors=True).status_int == 404
