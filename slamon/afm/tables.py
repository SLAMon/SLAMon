from sqlalchemy import Column, Integer, CHAR, DateTime, String, ForeignKey, PrimaryKeyConstraint, Unicode
from sqlalchemy.orm import relationship
from datetime import datetime
from slamon.afm.database import Base


class Agent(Base):
    __tablename__ = 'agents'

    uuid = Column('uuid', CHAR(36), primary_key=True, nullable=False)
    name = Column('name', Unicode, nullable=False)
    last_seen = Column('last_seen', DateTime, default=datetime.utcnow)


class AgentCapability(Base):
    __tablename__ = 'agent_capabilities'

    agent_uuid = Column('agent_uuid', CHAR(36), ForeignKey('agents.uuid'))
    agent = relationship(Agent, backref="capabilities")

    type = Column('type', String)
    version = Column('version', Integer)

    __table_args__ = (PrimaryKeyConstraint(agent_uuid, type, version),)


class Task(Base):
    __tablename__ = 'tasks'

    uuid = Column('uuid', CHAR(36), primary_key=True)
    test_id = Column('test_id', CHAR(36), nullable=False)
    type = Column('type', String)
    version = Column('version', Integer)

    # Data that goes to agent with the task
    data = Column('data', String)  # TODO - use json blob with psql
    # Data that was returned from agent
    result_data = Column('result_data', String)  # TODO - use json blob with psql

    # Agent that has been assigned to take care of the task - NULL if not claimed yet
    assigned_agent_uuid = Column('assigned_agent_uuid', CHAR(36), ForeignKey('agents.uuid'))
    assigned_agent = relationship(Agent, backref="tasks")

    # When was the task added
    created = Column('created', DateTime, default=datetime.utcnow, nullable=False)
    # When was the task claimed by agent - NULL if not claimed yet
    claimed = Column('claimed', DateTime, nullable=True)
    # When was the task completed by agent - NULL if not completed yet
    completed = Column('completed', DateTime, nullable=True)
    # When did the task fail - NULL if hasn't failed yet, additional info in error-field
    failed = Column('started', DateTime, nullable=True)
    # Error message that should only be present if failed is set
    error = Column('error', Unicode, nullable=True)
