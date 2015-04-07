#!/usr/bin/env python

from slamon.afm import afm_app
from slamon.afm.settings import Settings
from slamon.afm.database import init_connection
from bottle import run
import logging

LOGGING = {
    'version': 1,
    'disable_existing_loggers': True,
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'formatter': 'verbose'
        }
    },
    'loggers': {
        'tasks': {
            'handlers': ['console'],
            'propagate': True,
            'level': 'INFO',
        },
        'testing': {
            'handlers': ['console'],
            'propagate': True,
            'level': 'DEBUG'
        }
    }
}

if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)

    from slamon.afm.routes import agent_routes, bpms_routes, status_routes

    if Settings.testing_urls_available:
            from slamon.afm.routes.testing import testing_routes

    init_connection()
    run(afm_app.app, host='0.0.0.0', port=Settings.port, debug=True)
