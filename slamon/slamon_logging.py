import logging
import os, os.path


__logfile_path__ = "slamon/log/slamon_logfile.log"

if 'OPENSHIFT_DATA_DIR' in os.environ:
    __logfile_path__ = os.path.join(os.environ['OPENSHIFT_DATA_DIR'], "slamon_logfile.log")


def getSLAMonLogger(name):
    # create logger
    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)

    # create file handler and set level to debug
    file_handler = logging.FileHandler(__logfile_path__, encoding="UTF-8")
    file_handler.setLevel(logging.DEBUG)

    # create formatter
    formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s - %(message)s")

    # add formatter to file_handler
    file_handler.setFormatter(formatter)

    # add file_handler to logger
    logger.addHandler(file_handler)
    return logger
