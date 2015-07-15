#!/bin/sh
psql -h postgres -U postgres -c "DROP DATABASE IF EXISTS slamon;" && \
psql -h postgres -U postgres -c "DROP DATABASE IF EXISTS slamon_tests;" && \
psql -h postgres -U postgres -c "DROP USER IF EXISTS afm;" && \
psql -h postgres -U postgres -c "CREATE DATABASE slamon;" && \
psql -h postgres -U postgres -c "CREATE DATABASE slamon_tests;" && \
psql -h postgres -U postgres -c "CREATE USER afm WITH PASSWORD 'changeme';" && \
psql -h postgres -U postgres -c "GRANT ALL ON DATABASE slamon TO afm;" && \
psql -h postgres -U postgres -c "GRANT ALL ON DATABASE slamon_tests TO afm;"
