SLAMon
======

What is SLAMon
--------------
Service Level Agreement Monitoring (SLAMon) is a tool for monitoring live business processes and their live
performance as seen by the customer using the business process. SLAMon gathers performance data from the whole business
process instead of separate tasks inside the process. The business process owner defines the business process test flow
and the data gathered from the flow, so that the data can be aggregated to usable statistics.

SLAMon can be used for example testing whether a cloud service's process time meets the service level agreement, or
whether a given business process in a cloud works as it is defined to work.


### The current features of SLAMon

Currently SLAMon provides the following features (2015-04-07)

- Agent implementation for Linux (Python) and Android platforms
- SDK for creating new task handlers for the Agent implementations
- Agent Fleet Manager (AFM) that receives tasks from Business Process Management Suite and gives received tasks to
polling Agents
- JBoss Business Process Management Suite (jBPMS) implementation for sending tasks to AFM to be executed by Agents
- Push notifications for alerting about test process situation to the Android device using JBoss Unified Push


Testing
-------
SLAMon has both unit tests and system tests which can easily be run on vagrant using Vagrantfile provided with the project.

### Setting up test environment

Setting up test environment requires:
- Vagrant
  - Has been tested against 1.7.2
  - Refer to http://docs.vagrantup.com/v2/installation/ for installation
- Virtualbox
  - VMWare or others might work but only virtualbox has been tested so far

Test environment can be created with following:

    cd SLAMon/test_environment
    vagrant up

Updating system and installing necessary packages will take some time so grab a cup of coffee and relax.
The vagrantfile handles installation of all software and libraries needed for the tests to run.

After basic installation, changes can be applied to the vagrant instance using:

    vagrant provision

Connection to the machine can be established with:

    vagrant ssh

### Running unit tests

Inside vagrant machine:

    ./unit_test.sh

### Running system tests

Inside vagrant machine:

    ./system_test.sh
