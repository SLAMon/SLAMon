#!/usr/bin/env python

from distutils.core import setup

setup(
    name='slamon',
    version='0.0.1',
    packages=[
        'slamon',
        'slamon.agent',
        'slamon.agent.handlers',
    ],
    requires=[
        #'sqlalchemy',
        #'bottle',
        'jsonschema',
        #'webtest',
        #'mock',
        'responses',
        #'nose',
        'python_dateutil',
        'requests'
    ],
    entry_points={
        'console_scripts': [
            'slamon-agent = slamon.agent.agent:main'
        ]
    }
)
