from slamon.afm.settings import Settings
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, scoped_session
from sqlalchemy import create_engine
import os

engine = None
session_maker = sessionmaker()
create_session = scoped_session(session_maker)
Base = declarative_base()


def init_connection(unittest=False):
    global engine, session_maker, create_session
    if 'OPENSHIFT_POSTGRESQL_DB_URL' in os.environ:
        engine = create_engine(os.environ['OPENSHIFT_POSTGRESQL_DB_URL'])
    elif not unittest:
        engine = create_engine('postgresql+psycopg2://' + Settings.database_user + ':' + Settings.database_password +
                               '@localhost/' + Settings.database_name)
    else:
        engine = create_engine('postgresql+psycopg2://' + Settings.test_database_user + ':' +
                               Settings.test_database_password + '@localhost/' + Settings.test_database_name)
    create_session.remove()
    Base.metadata.bind = engine
    session_maker.configure(bind=engine)

    return engine
