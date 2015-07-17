#!/bin/sh
set -e
if [ ! -d "$HOME/python3.4/bin" ]; then
    wget https://www.python.org/ftp/python/3.4.3/Python-3.4.3.tgz;
    tar -xzf Python-3.4.3.tgz;
    echo -n "Building Python3.4 ... ";
    cd Python-3.4.3 && ./configure --silent --prefix=$HOME/python3.4 && make --silent && make --silent install;
    echo "Done!";
else
    echo "Using cached python3.4.";
fi

ls -la $HOME/python3.4/bin
