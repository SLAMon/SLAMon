from slamon.afm.routes import status_routes  # Shows as unused but is actually required for routes
from slamon.afm.afm_app import app
from slamon.afm.database import init_connection
from webtest import TestApp
import unittest
import os


class TestStatusSQLProblem(unittest.TestCase):
    """
    TestCase that doesn't actually setup proper database connection to make sure that /status works as expected
    """
    def tearDown(self):
        if 'OPENSHIFT_POSTGRESQL_DB_URL' in os.environ:
            del os.environ['OPENSHIFT_POSTGRESQL_DB_URL']

    @staticmethod
    def test_status_broken_session():
        test_app = TestApp(app)
        os.environ['OPENSHIFT_POSTGRESQL_DB_URL'] = 'postgresql+psycopg2://slamon:slamon@localhost/whatever'
        init_connection()

        assert test_app.get('/status', expect_errors=True).status_int == 500
