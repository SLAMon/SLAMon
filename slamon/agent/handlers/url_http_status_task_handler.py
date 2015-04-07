from slamon.agent.handlers import TaskHandler
import requests


@TaskHandler('url_http_status', 1)
def url_http_status_task_handler(input_params):
    """
    Task for checking web page availability and returning status code if page is available
    """
    url = input_params['url']

    response = requests.get(url)
    return {'status': response.status_code}
