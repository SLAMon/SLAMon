FROM ubuntu:trusty

ENV SLAMON_DIR /SLAMon
ENV SLAMON_VENV_DIR /venv-slamon
ENV POSTGRES_DOMAIN postgres

RUN apt-get -y update && apt-get -y upgrade && \
        DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends software-properties-common
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
RUN add-apt-repository ppa:webupd8team/java && apt-get -y update && \
        DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends oracle-java8-installer oracle-java8-set-default \
                                                          postgresql-client libpq-dev
RUN add-apt-repository ppa:webupd8team/java && apt-get -y update && \
        DEBIAN_FRONTEND=noninteractive apt-get -y install python3-pip gradle
RUN pip3 install --upgrade nose mock coverage webtest bottle SQLAlchemy jsonschema dateutils psycopg2 requests responses

# Android SDK
RUN dpkg --add-architecture i386
RUN apt-get -y update && \
        DEBIAN_FRONTEND=noninteractive apt-get -y install --no-install-recommends libncurses5:i386 libstdc++6:i386 zlib1g:i386

# Install scripts
ADD docker_entrypoint.sh /bin/docker_entrypoint.sh
RUN chmod +x /bin/docker_entrypoint.sh
RUN mkdir ${SLAMON_DIR}
