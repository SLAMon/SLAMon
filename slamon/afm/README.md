SLAMon Agent Fleet Manager (AFM)
==================
$SLAMON_ROOT refers to the directory where the root of this repository lies

# Requirements
* python 3.4
* virtualenv

# Setting up
File slamon/afm/settings.py contains AFM settings in following format:
```
class Settings:
    port = 8080  # Port for the server

    database_name = 'slamon'  # Name of the psql database
    database_user = 'afm'  # Username to use for psql connection
    database_password = 'changeme'  # Password to use for psql connection
```

## Creating postgresql database
```
psql
postgres=# CREATE DATABASE slamon;
postgres=# CREATE DATABASE slamon_tests;
postgres=# CREATE USER afm WITH PASSWORD 'changeme';
postgres=# GRANT ALL PRIVILEGES ON DATABASE slamon TO afm;
postgres=# GRANT ALL PRIVILEGES ON DATABASE slamon_tests TO afm;
\q
```

To create needed tables:
```
cd $SLAMON_ROOT
python ./slamon/afm/admin.py --create-tables
```

To delete tables:
```
cd $SLAMON_ROOT
python ./slamon/afm/admin.py --drop-tables
```

## Creating python virtualenv and installing needed packages
```
cd $SLAMON_ROOT
virtualenv env
. env/bin/active
pip install -r requirements_afm.txt
```

# Running
## Running afm
After entering the virtual environment type in a terminal following:
```
cd $SLAMON_ROOT
export PYTHONPATH=`pwd`
python ./slamon/afm/afm.py
```
### Running tests
In virtual environment:
```
cd $SLAMON_ROOT
nosetests
```
or (if coverage report is also wanted)
```
cd $SLAMON_ROOT
nosetests --with-coverage --cover-package=slamon
```