from slamon.afm.database import Base, init_connection, engine
from slamon.afm.tables import Agent, AgentCapability, Task


class AFMTest(object):
    @classmethod
    def create_tables(cls):
        Base.metadata.create_all(engine)

    @classmethod
    def drop_tables(cls):
        Base.metadata.drop_all(engine)

    def setup(self):
        init_connection(unittest=True)

        self.create_tables()

    def teardown(self):
        self.drop_tables()