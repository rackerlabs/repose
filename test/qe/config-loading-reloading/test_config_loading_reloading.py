#!/usr/bin/env python

"""

Stories
-------
  - B-39898 "Behavior loading and reloading of configs"
  - B-43371 "Allow Missing RMS Config"

Specification
-------------
This story formalizes the expected behavior of Repose in response to invalid
configuration files. In short, the following behaviors are required:

  1. When Repose is started with one or more invalid configs, then it should
     return 503's for all incoming requests.
  2. After Repose is started with invalid configs, if valid configs are loaded
     at run-time, then Repose should return whatever response is appropriate
     given the new configs.
  3. If Repose is already running with good configs, and the files are replaced
     with bad/invalid configs, then Repose should log an error and refuse to
     load the new configs, instead behaving as though nothing has changed.
  4. This behavior must be tested for each config file that Repose uses.
  5. Because the container config and the system model config are both required
     for Repose to be able to listen on a port and return 503's, bad versions
     of them won't be able to return anything. Therefore, if either of those
     config files is bad, it shouldn't be possible to make an HTTP connection
     in the first place.

Additionally, for the RMS config file:

  1. When there is a missing configuration that is not required by default or
     by the filter Repose should start without missing configuration file error
     and should pick it up and work if the missing configuration that is good
     is added later and throw validation error if there added missing
     configuration has any mal-formed XML.

For the purposes of this test, "bad" or "invalid" refers to mal-formed XML.
Future tests will need to be created to check configs against the XSD schemas
defined for them. The "good" versions of the configs will all include the
Repose mock server as the only available destination in the system model,
ensuring a 200 response code for valid states.

The configuration files to be tested are:

  - system-model.cfg.xml
  - container.cfg.xml
  - response-messaging.cfg.xml
  - rate-limiting.cfg.xml
  - versioning.cfg.xml
  - translation.cfg.xml
  - client-auth-n.cfg.xml
  - openstack-authorization.cfg.xml
  - dist-datastore.cfg.xml
  - http-logging.cfg.xml
  - uri-identity.cfg.xml
  - header-identity.cfg.xml
  - ip-identity.cfg.xml

For each of these, there will be one good version and one bad version. The bad
versions will be copies of the good versions with missing closing tags.
Additionally, for each filter config, a system model file will be created
specifying that filter, so that it is included when Repose runs. "Missing"
configs means that the configuration file does not exist in the Repose config
directory.

Procedure
---------
The tests will proceed as follows. For each config to be tested:

  1. Repose will be started with the "bad" version of the configs. An HTTP
     requests will be sent to Repose, and a 503 error code will be expected
     (except for the system model or container configs, in which case no
     connection will allowed).
  2. Next, the configs will be changed to the "good" version. An HTTP requests
     will be sent to Repose, and a 200 success code will be expected.
  3. Next, the configs will be changed back to the "bad" version. An HTTP
     request will be sent to Repose, and a 200 success code will be expected.
  4. Repose will be shut down
  5. Repose will be started with the "good" version of the configs. An HTTP
     request will be sent to Repose, and a 200 success code will be expected.

In addition to the RMS config alone:

  1. Repose will be started with the "missing" config that is not required. An
     HTTP request will be sent to Repose, and a 200 success code will be
     expected.


Test cases for B-29898
    +---------------------------------+----------------+-------------------------+
    | Component                       | Transition     | Expected Result         |
    +=================================+================+=========================+
    | system-model.cfg.xml            | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | system-model.cfg.xml            | Start Bad      | Can't connect to Repose |
    +---------------------------------+----------------+-------------------------+
    | system-model.cfg.xml            | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | system-model.cfg.xml            | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | container.cfg.xml               | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | container.cfg.xml               | Start Bad      | Can't Connect to Repose |
    +---------------------------------+----------------+-------------------------+
    | container.cfg.xml               | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | container.cfg.xml               | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | response-messaging.cfg.xml      | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | response-messaging.cfg.xml      | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | response-messaging.cfg.xml      | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | response-messaging.cfg.xml      | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | rate-limiting.cfg.xml           | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | rate-limiting.cfg.xml           | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | rate-limiting.cfg.xml           | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | rate-limiting.cfg.xml           | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | versioning.cfg.xml              | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | versioning.cfg.xml              | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | versioning.cfg.xml              | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | versioning.cfg.xml              | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | translation.cfg.xml             | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | translation.cfg.xml             | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | translation.cfg.xml             | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | translation.cfg.xml             | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | client-auth-n.cfg.xml           | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | client-auth-n.cfg.xml           | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | client-auth-n.cfg.xml           | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | client-auth-n.cfg.xml           | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | openstack-authorization.cfg.xml | Start Good     | 401                     |
    +---------------------------------+----------------+-------------------------+
    | openstack-authorization.cfg.xml | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | openstack-authorization.cfg.xml | Good to Bad    | 401                     |
    +---------------------------------+----------------+-------------------------+
    | openstack-authorization.cfg.xml | Bad to Good    | 401                     |
    +---------------------------------+----------------+-------------------------+
    | dist-datastore.cfg.xml          | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | dist-datastore.cfg.xml          | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | dist-datastore.cfg.xml          | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | dist-datastore.cfg.xml          | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | http-logging.cfg.xml            | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | http-logging.cfg.xml            | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | http-logging.cfg.xml            | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | http-logging.cfg.xml            | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | uri-identity.cfg.xml            | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | uri-identity.cfg.xml            | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | uri-identity.cfg.xml            | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | uri-identity.cfg.xml            | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | header-identity.cfg.xml         | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | header-identity.cfg.xml         | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | header-identity.cfg.xml         | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | header-identity.cfg.xml         | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | ip-identity.cfg.xml             | Start Good     | 200                     |
    +---------------------------------+----------------+-------------------------+
    | ip-identity.cfg.xml             | Start Bad      | 503                     |
    +---------------------------------+----------------+-------------------------+
    | ip-identity.cfg.xml             | Good to Bad    | 200                     |
    +---------------------------------+----------------+-------------------------+
    | ip-identity.cfg.xml             | Bad to Good    | 200                     |
    +---------------------------------+----------------+-------------------------+


Test cases for B-43371
    +---------------------------------+----------------+-------------------------+
    | Component                       | Transition     | Expected Result         |
    +=================================+================+=========================+
    | response-messaging.cfg.xml      | Start Missing  | 200                     |
    +---------------------------------+----------------+-------------------------+


Items not tested
----------------
As mentioned before, this test will not test whether or or how Repose handles
config files that fail XSD validation.

These tests do not cover the following components' configuration files:

  - API Validation filter
  - Content Normalization filter
  - Destination Router
  - Header Identity Mapping filter
  - Header Normalization
  - Rackspace Auth 1.1 Content Identity filter
  - Root Context Router
  - Service Authentication filter
  - URI Normalization filter


"""


from narwhal import valve
from narwhal import conf
from narwhal import get_next_open_port
import requests
import time
import sys
import unittest2 as unittest
import xmlrunner
import logging
import SocketServer
import BaseHTTPServer
import threading
import os
import os.path
import argparse
import deproxy


logger = logging.getLogger(__name__)

mock_port = 8894
mock_service = None
mock_url = 'http://localhost:%i/' % mock_port

repose_config_folder = 'etc/repose'
repose_port = 8893
repose_stop_port = 9893
repose_url = 'http://localhost:%s' % repose_port
config_params = {'port': str(repose_port),
                 'target_hostname': 'localhost',
                 'target_port': mock_port}
sleep_time = 35
request_timeout = 30

def setUpModule():
    logger.debug('setUpModule')
    repose_conf_folder = 'etc/repose'
    create_folder(repose_conf_folder)
    clear_folder(repose_conf_folder)
    create_folder('var/log/repose')
    create_folder('var/repose')

    # the valve jar should be 'usr/share/repose/repose-valve.jar'
    # the filter ears should be in 'usr/share/repose/filters/'

    global mock_service
    if mock_service is not None:
        return

    mock_service = deproxy.Deproxy()
    mock_service.add_endpoint(mock_port)
    logger.debug('setUpModule complete')


def tearDownModule():
    if mock_service:
        mock_service.shutdown_all_endpoints()


def clear_folder(folder_name):
    for _name in os.listdir(folder_name):
        name = os.path.join(folder_name, _name)
        if os.path.isdir(name):
            clear_folder(name)
            os.rmdir(name)
        else:
            os.remove(name)


def create_folder(folder_name):
    if not os.path.exists(folder_name):
        os.makedirs(folder_name)


def get_status_code_from_url(url):
    return requests.get(url, timeout=request_timeout).status_code


##############################################################



# use execfile on an external generated file, so that we can easily generate
# tests cases without having to worry about non-test-case code.
execfile('test_config_loading_reloading_gen.py')



##############################################################


class TestResponseMessagingStartMissing(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()
    def get_name(self):
        return 'response-messaging'



def run():
    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', action='store_true',
                        help='Print the log.')
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG,
                            format=('%(asctime)s %(levelname)s:%(name)s:'
                                    '%(funcName)s:'
                                    '%(filename)s(%(lineno)d):'
                                    '%(threadName)s(%(thread)d):%(message)s'))

    test_runner = xmlrunner.XMLTestRunner(output='test-reports')

    unittest.main(argv=[''], testRunner=test_runner, verbosity=2)


if __name__ == '__main__':
    run()
