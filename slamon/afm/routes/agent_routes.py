from slamon.afm.tables import Agent, AgentCapability, Task
from slamon.afm.afm_app import app
from slamon.afm.database import create_session
from slamon.afm.settings import Settings
from slamon.slamon_logging import getSLAMonLogger
from datetime import datetime, timedelta
from bottle import request, HTTPError
from sqlalchemy.orm.exc import NoResultFound
from sqlalchemy import and_
from dateutil import tz
import jsonschema
import json

logger = getSLAMonLogger(__name__)

task_request_schema = {
    'type': 'object',
    'properties': {
        'protocol': {
            'type': 'integer',
            'minimum': 1
        },
        'agent_id': {
            'type': 'string',
            'pattern': '^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$'
        },
        'agent_name': {'type': 'string'},
        'agent_location': {
            'type': 'object',
            'properties': {
                'country': {
                    'type': 'string',
                    'minLength': 2,
                    'maxLength': 2
                },
                'region': {
                    'type': 'string',
                    'minLength': 2,
                    'maxLength': 4
                },
                'latitude': {
                    'type': 'number'
                },
                'longitude': {
                    'type': 'number'
                }
            },
            'required': ['country', 'region'],
            'additionalProperties': False
        },
        'agent_capabilities': {
            'type': 'object',
            'patternProperties': {
                '^.+$': {
                    'type': 'object',
                    'properties': {
                        'version': {'type': 'integer'}
                    }
                }
            }
        },
        'agent_time': {
            'type': 'string'
        },
        'max_tasks': {
            'type': 'integer'
        }
    },
    'required': ['protocol', 'agent_id', 'agent_name', 'agent_time', 'agent_capabilities', 'max_tasks'],
    'additionalProperties': False
}


@app.post('/tasks')
@app.post('/tasks/')
def request_tasks():
    data = request.json

    if data is None:
        raise HTTPError(400)

    try:
        jsonschema.validate(data, task_request_schema)
    except jsonschema.ValidationError:
        raise HTTPError(400)

    protocol = int(data['protocol'])
    agent_uuid = str(data['agent_id'])
    agent_name = str(data['agent_name'])
    agent_time = data['agent_time']
    agent_capabilities = data['agent_capabilities']
    max_tasks = int(data['max_tasks'])
    agent_location = data['agent_location'] if 'agent_location' in data else None

    # Only protocol 1 supported for now
    if protocol != 1:
        raise HTTPError(400)

    try:
        session = create_session()
    except:
        logger.error("Failed to create database session for task request")
        raise HTTPError(500)

    try:
        query = session.query(Agent)
        agent = query.filter(Agent.uuid == agent_uuid).one()

        session.query(AgentCapability).filter(AgentCapability.agent_uuid == agent.uuid).delete()
    except NoResultFound:
        agent = Agent(uuid=agent_uuid, name=agent_name)
        session.add(agent)

    for agent_capability, agent_capability_info in agent_capabilities.items():
        capability = AgentCapability(type=agent_capability,
                                     version=int(agent_capability_info['version']),
                                     agent=agent)
        session.add(capability)

    # Find all non-claimed tasks that the agent is able to handle and assign them to the agent
    query = session.query(AgentCapability, Task).filter(Task.assigned_agent_uuid.is_(None)).\
        filter(AgentCapability.agent_uuid == agent.uuid).\
        filter(and_(AgentCapability.type == Task.type, AgentCapability.version == Task.version))

    tasks = []

    # Assign available tasks to the agent and mark them as being in process
    for _, task in query[0:max_tasks]:
        task.assigned_agent_uuid = agent.uuid
        task.claimed = datetime.utcnow()
        tasks.append({
            'task_id': task.uuid,
            'task_type': task.type,
            'task_version': task.version,
            'task_data': json.loads(task.data)
        })

    agent.last_seen = datetime.utcnow()
    try:
        session.commit()
    except Exception:
        session.rollback()
        logger.error("Failed to commit database changes for task request")
        raise HTTPError(500)
    finally:
        session.close()

    logger.info("Agent requested tasks - Agent's name and uuid: {}, {} - Agent was given following tasks: {}"
                .format(agent_name, agent_uuid, tasks))

    return {
        "tasks": tasks,
        "return_time": (datetime.now(tz.tzlocal()) + timedelta(0, Settings.agent_return_time)).isoformat()
    }


task_response_schema = {
    'type': 'object',
    'oneOf': [
        {
            'type': 'object',
            'properties': {
                'protocol': {
                    'type': 'integer'
                },
                'task_id': {
                    'type': 'string',
                    'pattern': '^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$'
                },
                'task_data': {
                    'type': 'object'
                }
            },
            'additionalProperties': False,
            'required': ['protocol', 'task_id', 'task_data']
        },
        {
            'type': 'object',
            'properties': {
                'protocol': {
                    'type': 'integer'
                },
                'task_id': {
                    'type': 'string',
                    'pattern': '^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$'
                },
                'task_error': {
                    'type': 'string'
                }
            },
            'additionalProperties': False,
            'required': ['protocol', 'task_id', 'task_error']
        }
    ]
}


@app.post('/tasks/response')
@app.post('/tasks/response/')
def post_tasks():
    data = request.json

    if data is None:
        logger.error("No JSON content in task response request!")
        raise HTTPError(400)

    try:
        jsonschema.validate(data, task_response_schema)
    except jsonschema.ValidationError as e:
        logger.error("Invalid JSON in task reponse: {0}".format(e))
        raise HTTPError(400)

    protocol = int(data['protocol'])
    task_id = str(data['task_id'])

    # Only protocol 1 supported for now
    if protocol != 1:
        logger.error("Invalid protocol in task response: {0}".format(protocol))
        raise HTTPError(400)

    try:
        session = create_session()
    except:
        logger.error("Failed to create database session for task result POST")
        raise HTTPError(500)

    try:
        task = session.query(Task).filter(Task.uuid == str(task_id)).one()

        if task.claimed is None or (task.completed is not None or task.failed is not None):
            logger.error("Incomplete task posted!")
            session.close()
            raise HTTPError(400)

        result = ""
        if 'task_data' in data:
            task.result_data = json.dumps(data['task_data'])
            task.completed = datetime.utcnow()
            result = json.dumps(task.result_data)
        elif 'task_error' in data:
            task.error = data['task_error']
            task.failed = datetime.utcnow()
            result = task.error

        session.add(task)
    except NoResultFound:
        logger.error("No matching task in for task response!")
        session.close()
        raise HTTPError(400)

    try:
        session.commit()
    except Exception:
        session.rollback()
        logger.error("Failed to commit database changes for task result POST")
        raise HTTPError(500)
    finally:
        session.close()

    logger.info("An agent returned task with results - uuid: {}, end results: {}".format(task_id, result))
