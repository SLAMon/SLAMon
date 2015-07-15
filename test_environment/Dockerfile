FROM ubuntu:trusty

ENV SLAMON_DIR /SLAMon
ENV SLAMON_VENV_DIR /venv-slamon
ENV POSTGRES_DOMAIN postgres

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install software-properties-common
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
RUN add-apt-repository ppa:webupd8team/java && apt-get -y update && \
        apt-get -y install oracle-java8-installer oracle-java8-set-default
RUN apt-get -y update && \
        apt-get -y install python3-pip postgresql-client libpq-dev gradle
RUN pip3 install --upgrade nose mock coverage webtest bottle SQLAlchemy jsonschema dateutils psycopg2 requests responses
ENV DEBIAN_FRONTEND newt

# Install scripts
ADD docker_entrypoint.sh /bin/docker_entrypoint.sh
RUN chmod +x /bin/docker_entrypoint.sh
RUN mkdir ${SLAMON_DIR}
