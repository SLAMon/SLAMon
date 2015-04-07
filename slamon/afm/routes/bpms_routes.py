from slamon.afm.tables import Task
from slamon.afm.afm_app import app
from slamon.slamon_logging import getSLAMonLogger
from sqlalchemy.orm.exc import NoResultFound
from sqlalchemy.exc import IntegrityError, ProgrammingError
from bottle import request, HTTPError
from slamon.afm.database import create_session
import jsonschema
import json

logger = getSLAMonLogger(__name__)

post_task_schema = {
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
        }
    },
    'required': ['task_id', 'task_type', 'task_version', 'test_id'],
    'additionalProperties': False
}


@app.post('/task')
@app.post('/task/')
def post_task():
    data = request.json

    if data is None:
        raise HTTPError(400)

    try:
        jsonschema.validate(data, post_task_schema)
    except jsonschema.ValidationError:
        raise HTTPError(400)

    session = create_session()

    task_uuid = str(data['task_id'])
    task_type = str(data['task_type'])
    task_test_id = (data['test_id'])
    task_data = ""

    task = Task(
        uuid=task_uuid,
        type=task_type,
        version=int(data['task_version']),
        test_id=task_test_id
    )

    if 'task_data' in data:
        task_data = json.dumps(data['task_data'])
        task.data = task_data

    try:
        session.add(task)
    except IntegrityError:
        session.rollback()
        raise HTTPError(400)

    try:
        session.commit()
    except (IntegrityError, ProgrammingError):
        session.rollback()
        logger.error("Failed to commit database changes for BPMS task POST")
        raise HTTPError(400)
    finally:
        session.close()

    logger.info("Task posted by BPMS - Task's type: {}, test process id: {}, uuid: {}, parameters: {}"
                .format(task_type, task_test_id, task_uuid, task_data))


@app.get('/task/<task_uuid:re:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}>')
@app.get('/task/<task_uuid:re:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}>/')
def get_task(task_uuid: str):
    """
    Gets information about single task with uuid task_uuid
    :param task_uuid: uuid of the task
    :return: dict in following format
    {
        'task_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',  # UUID of the task (str)
        'test_id': 'de305d54-75b4-431b-adb2-eb6b9e546013',  # UUID of the test (str)
        'task_type': 'wait',                                # type of the task (str)
        'task_version': 1,                                  # Version number of the task
        'task_data': {},                                    # Dict containing data passed to the task (if any)
        'task_completed': '31-03-2015:12:12:12',            # Time when task was completed (if completed)
        'task_result': {},                                  # Dict containing task's results (if completed)
        'task_failed': '31-03-2015:12:12:12',               # Time when task failed (if failed)
        'task_error': 'Something went wrong'                # Error that caused task to fail (if failed)
    }
    """
    try:
        session = create_session()
    except:
        raise HTTPError(500)

    try:
        query = session.query(Task)
        task = query.filter(Task.uuid == str(task_uuid)).one()
    except NoResultFound:
        raise HTTPError(404)
    finally:
        session.close()

    task_desc = {
        'task_id': task.uuid,
        'test_id': task.test_id,
        'task_type': task.type,
        'task_version': task.version
    }

    if task.data is not None:
        task_desc['task_data'] = json.loads(task.data)

    if task.failed:
        task_desc['task_failed'] = str(task.failed)
        task_desc['task_error'] = str(task.error)
    elif task.completed:
        task_desc['task_completed'] = str(task.completed)
        task_desc['task_result'] = json.loads(task.result_data)

    return task_desc
