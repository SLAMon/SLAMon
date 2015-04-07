class TaskHandler(object):
    """
    TaskHandler is a decorator to register a callable as a task handler for the agent.
    """

    _handlers = {}

    def __init__(self, name: str, version: int):
        self.name = name
        self.version = version

    def __call__(self, handler):
        TaskHandler.register(handler, self.name, self.version)
        return handler

    @classmethod
    def register(cls, callable, name: str, version: int):
        """
        Utility to register any callable as task handler.
        :param callable: Callable taking one positional argument (input data) and returning dictionary of output params.
        :param name: Name of the task type
        :param version: Version of the supported task type
        :return:
        """
        import logging

        logging.getLogger("TaskHandler").debug("Registering handler: %s", name)
        TaskHandler._handlers[name] = {
            'version': version,
            'function': callable
        }

    @classmethod
    def create(cls, name: str, version: int):
        """
        Get corresponding handler for task.

        :param name: the name of the task
        :param version: the version of the task
        :return: handler is suitable is found, None otherwise
        """
        handler = TaskHandler._handlers[name]
        if handler['version'] == version:
            return handler['function']

    @classmethod
    def list_all(cls):
        """
        List all registered task handlers as dictionary consisting of task names as keys and dictionaries as values.

        :return: dictionary of registered task handlers
        """
        return {name: {'version': data['version']} for name, data in TaskHandler._handlers.items()}
