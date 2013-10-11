#!/usr/bin/env python

import requests
import unittest2 as unittest
import xmlrunner
import logging
import time
import argparse
import os
from narwhal import conf
from narwhal import pathutil
from narwhal import valve
from narwhal import get_next_open_port

logger = logging.getLogger(__name__)


target_hostname = '50.57.189.15'
target_port = 8080
config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deploy_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')

# we sleep after starting repose because this isn't the normal kind of start up
# scenario. for example, if we start repose with a different port in the system
# model than on the command line, it is unclear which should be waited on.
sleep_duration = 35


def setUpModule():
    # Set up folder hierarchy and install repose JAR/EARs if needed
    logger.debug('setUpModule')
    pathutil.create_folder(config_dir)
    pathutil.create_folder(deploy_dir)
    pathutil.create_folder(os.path.dirname(log_file))
    pass


def get_status_code_from_url(url, timeout=None):
    logger.debug('get_status_code_from_url(url="%s")' % url)
    return requests.get(url, timeout=timeout, verify=False).status_code


def apply_config_set(config_set_name, params=None):
    if params is None:
        params = {}
    conf.process_folder_contents(folder='configs/%s' % config_set_name,
                                 verbose=False, dest_path=config_dir,
                                 params=params)




class TestRuntimeSysmodChanges(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port1 = get_next_open_port()
        self.port2 = get_next_open_port()
        self.port3 = get_next_open_port()
        self.stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'target_hostname': target_hostname,
            'target_port': target_port,
            'sysmod_port': self.port1,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('container-no-port', params=params)
        apply_config_set('single-node-with-proto', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=self.stop_port)
        time.sleep(sleep_duration)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)

    def make_request_and_assert_status_code(self, url, expected_status_code):
        logger.debug('asserting status code - url = %s' % url)
        logger.debug('expected status code = %i' % expected_status_code)
        status_code = get_status_code_from_url(url)
        logger.debug('received status code = %i' % status_code)
        self.assertEqual(expected_status_code, status_code)

    def make_request_and_assert_connection_fails(self, url):
        logger.debug('asserting connection fails - url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def runTest(self):
        logger.debug('runTest')

        #raw_input('Ready and waiting.')

        # test with only the first node
        url = 'http://localhost:%i/' % (self.port1)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port2)
        self.make_request_and_assert_connection_fails(url)

        url = 'http://localhost:%i/' % (self.port3)
        self.make_request_and_assert_connection_fails(url)

        # change the configs while it's running
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'node1host': 'localhost',
            'node2host': 'localhost',
            'node1port': self.port1,
            'node2port': self.port2,
        }
        apply_config_set('two-nodes', params=params)
        time.sleep(sleep_duration)

        # test with nodes one and two
        url = 'http://localhost:%i/' % (self.port1)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port2)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port3)
        self.make_request_and_assert_connection_fails(url)

        # change the configs
        params = {
            'proto': 'http',
            'target_hostname': target_hostname,
            'target_port': target_port,
            'sysmod_port': self.port2,
        }
        apply_config_set('single-node-with-proto', params=params)
        time.sleep(sleep_duration)

        # test with node two only
        url = 'http://localhost:%i/' % (self.port1)
        self.make_request_and_assert_connection_fails(url)

        url = 'http://localhost:%i/' % (self.port2)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port3)
        self.make_request_and_assert_connection_fails(url)

        # change the configs
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'node1host': 'localhost',
            'node2host': 'localhost',
            'node3host': 'example.com',
            'node1port': self.port1,
            'node2port': self.port2,
            'node3port': self.port3,
        }
        apply_config_set('three-nodes', params=params)
        time.sleep(sleep_duration)

        # test with all three nodes
        url = 'http://localhost:%i/' % (self.port1)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port2)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port3)
        self.make_request_and_assert_connection_fails(url)

        # change only hostnames
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'node1host': 'example.com',
            'node2host': 'localhost',
            'node3host': 'localhost',
            'node1port': self.port1,
            'node2port': self.port2,
            'node3port': self.port3,
        }
        apply_config_set('three-nodes', params=params)
        time.sleep(sleep_duration)

        # test with all three nodes
        url = 'http://localhost:%i/' % (self.port1)
        self.make_request_and_assert_connection_fails(url)

        url = 'http://localhost:%i/' % (self.port2)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port3)
        self.make_request_and_assert_status_code(url, 200)


class TestStartWithSingleNonLocalhostNode(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port = get_next_open_port()
        self.stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'host': 'example.com',
            'port': self.port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('container-no-port', params=params)
        apply_config_set('one-node', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=self.stop_port)
        time.sleep(sleep_duration)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)

    def make_request_and_assert_status_code(self, url, expected_status_code):
        logger.debug('asserting status code - url = %s' % url)
        logger.debug('expected status code = %i' % expected_status_code)
        status_code = get_status_code_from_url(url)
        logger.debug('received status code = %i' % status_code)
        self.assertEqual(expected_status_code, status_code)

    def make_request_and_assert_connection_fails(self, url):
        logger.debug('asserting connection fails - url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def runTest(self):
        logger.debug('runTest')

        #raw_input('Ready and waiting.')

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_connection_fails(url)

        # change the hostname to 'localhost'
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'host': 'localhost',
            'port': self.port,
        }
        apply_config_set('one-node', params=params)
        time.sleep(sleep_duration)

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_status_code(url, 200)


class TestStartWithZeroNodes(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port = get_next_open_port()
        self.stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('container-no-port', params=params)
        apply_config_set('zero-nodes', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=self.stop_port)
        time.sleep(sleep_duration)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)

    def make_request_and_assert_status_code(self, url, expected_status_code):
        logger.debug('asserting status code - url = %s' % url)
        logger.debug('expected status code = %i' % expected_status_code)
        status_code = get_status_code_from_url(url)
        logger.debug('received status code = %i' % status_code)
        self.assertEqual(expected_status_code, status_code)

    def make_request_and_assert_connection_fails(self, url):
        logger.debug('asserting connection fails - url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def runTest(self):
        logger.debug('runTest')

        #raw_input('Ready and waiting.')

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_connection_fails(url)

        # add a node for localhost
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'host': 'localhost',
            'port': self.port,
        }
        apply_config_set('one-node', params=params)
        time.sleep(sleep_duration)

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_status_code(url, 200)


class TestPortsOnCommandLineHttpSame(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.cmd_line_port = self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'cmd_line_port': self.cmd_line_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-no-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 port=self.cmd_line_port,
                                 stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_on_command_line_http_same(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port specified at the command line
        url = 'http://localhost:%i/' % self.cmd_line_port
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(200, status_code)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


class TestPortsOnCommandLineHttpsSame(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.cmd_line_port = self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'https',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'cmd_line_port': self.cmd_line_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-no-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 https_port=self.cmd_line_port,
                                 stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_on_command_line_https_same(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'https://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port specified at the command line
        url = 'https://localhost:%i/' % self.cmd_line_port
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(200, status_code)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


class TestPortsOnCommandLineHttpDiff(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.cmd_line_port = get_next_open_port()
        # self.cmd_line_port will be different from self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'cmd_line_port': self.cmd_line_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-no-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 port=self.cmd_line_port,
                                 stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_on_command_line_http_diff(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port specified at the command line
        url = 'http://localhost:%i/' % self.cmd_line_port
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


class TestPortsOnCommandLineNone(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.cmd_line_port = get_next_open_port()
        # self.cmd_line_port will be different from self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'cmd_line_port': self.cmd_line_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-no-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_on_command_line_none(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port specified at the command line
        url = 'http://localhost:%i/' % self.cmd_line_port
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


class TestPortsInContainerHttpSame(unittest.TestCase):

    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.con_port = self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': self.con_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-with-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir, stop_port=stop_port)
        time.sleep(sleep_duration)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)

    def test_ports_in_container_http_same(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port in the container
        url = 'http://localhost:%i/' % self.con_port
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(200, status_code)


class TestPortsInContainerHttpsSame(unittest.TestCase):

    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.con_port = self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'https',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': self.con_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-with-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir, stop_port=stop_port)
        time.sleep(sleep_duration)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)

    def test_ports_in_container_https_same(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'https://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port in the container
        url = 'https://localhost:%i/' % self.con_port
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(200, status_code)


class TestPortsInContainerHttpDiff(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.con_port = get_next_open_port()
        # self.con_port will be different from self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': self.con_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-with-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir, stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_in_container_http_diff(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port in the container
        url = 'http://localhost:%i/' % self.con_port
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


class TestPortsInContainerNone(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.sysmod_port = get_next_open_port()
        self.con_port = get_next_open_port()
        # self.con_port will be different from self.sysmod_port
        stop_port = get_next_open_port()

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': self.con_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('single-node-with-proto', params=params)
        apply_config_set('container-no-port', params=params)
        self.valve = valve.Valve(config_dir=config_dir, stop_port=stop_port)
        time.sleep(sleep_duration)

    def test_ports_in_container_none(self):
        logger.debug('runTest')

        # test port in the system model
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(200, status_code)

        # test port in the container
        url = 'http://localhost:%i/' % self.con_port
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
            time.sleep(5)


def run():

    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', help="Print the log to STDERR.",
                        action='store_true')
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG,
                            format=('%(asctime)s %(levelname)s:%(name)s:'
                                    '%(funcName)s:'
                                    '%(filename)s(%(lineno)d):'
                                    '%(threadName)s(%(thread)d):%(message)s'))

    test_runner = xmlrunner.XMLTestRunner(output='test-reports')

    unittest.main(argv=[''], testRunner=test_runner)


if __name__ == '__main__':
    run()
