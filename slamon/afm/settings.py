class Settings:
    """
    Contains basic settings for SLAMon Agent Fleet Manager
    """
    port = 8080  # Port for the server

    database_name = 'slamon'  # Name of the psql database
    database_user = 'afm'  # Username to use for psql connection
    database_password = 'changeme'  # Password to use for psql connection

    test_database_name = 'slamon_tests'
    test_database_user = 'afm'
    test_database_password = 'changeme'

    testing_urls_available = True

    agent_return_time = 5  # Time what agent should wait before trying to request for tasks again (seconds)
    agent_active_threshold = 300  # Time after which agent will be considered as inactive (seconds)