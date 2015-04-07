from bottle import request, HTTPError, static_file
from sqlalchemy.exc import IntegrityError, ProgrammingError
from slamon.afm.tables import Agent, Task
from slamon.afm.afm_app import app
from slamon.afm.database import create_session
import logging
import jsonschema
import os.path
import json


logger = logging.getLogger('testing')


@app.get('/testing/agents')
@app.get('/testing/agents/')
def dev_get_agents():
    try:
        session = create_session()
    except:
        raise HTTPError(500)

    try:
        query = session.query(Agent)
        agents = query.all()
    except Exception as e:
        logger.exception(e)
        session.close()
        raise HTTPError(500)

    agents_array = []
    for agent in agents:
        agent_json = {
            'agent_id': agent.uuid,
            'agent_name': agent.name,
            'last_seen': str(agent.last_seen)
        }

        if agent.capabilities:
            agent_json['agent_capabilities'] = {}
            for capability in agent.capabilities:
                agent_json['agent_capabilities'][capability.type] = {
                    'version': capability.version
                }

        agents_array.append(agent_json)

    session.close()
    return {'agents': agents_array}


@app.get('/testing/tasks')
@app.get('/testing/tasks/')
def dev_get_tasks():
    try:
        session = create_session()
    except:
        raise HTTPError(500)

    try:
        query = session.query(Task)
        tasks = query.all()
    except Exception as e:
        logger.exception(e)
        raise HTTPError(500)
    finally:
        session.close()

    tasks_array = []
    for task in tasks:
        task_desc = {
            'task_id': task.uuid,
            'task_type': task.type,
            'task_version': task.version,
            'test_id': task.test_id
        }

        if task.data is not None:
            task_desc['task_data'] = json.loads(task.data)
        if task.failed:
            task_desc['task_failed'] = str(task.failed)
            task_desc['task_error'] = str(task.error)
        elif task.completed:
            task_desc['task_completed'] = str(task.completed)
            task_desc['task_result'] = str(task.result_data)

        tasks_array.append(task_desc)

    return {'tasks': tasks_array}

dev_post_tasks_schema = {
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
            'type': 'object'
        },
        'test_id': {
            'type': 'string',
            'pattern': '^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$'
        },
    },
    'required': ['task_id', 'task_type', 'task_version', 'test_id'],
    'additionalProperties': False
}


@app.post('/testing/tasks')
@app.post('/testing/tasks/')
def dev_post_tasks():
    data = request.json
    if data is None:
        raise HTTPError(400)

    try:
        jsonschema.validate(data, dev_post_tasks_schema)
    except jsonschema.ValidationError:
        raise HTTPError(400)

    try:
        session = create_session()
    except:
        raise HTTPError(500)

    task = Task(
        uuid=str(data['task_id']),
        type=data['task_type'],
        version=data['task_version'],
        test_id=data['test_id'])

    if 'task_data' in data:
        task.data = json.dumps(data['task_data'])

    try:
        session.add(task)
    except IntegrityError:
        session.rollback()
        session.close()
        raise HTTPError(400)

    try:
        session.commit()
    except (IntegrityError, ProgrammingError):
        session.rollback()
        raise HTTPError(400)
    finally:
        session.close()


@app.get('/testing')
@app.get('/testing/')
def dev_testing_index():
    return static_file('testing.html', root=os.path.dirname(__file__))
