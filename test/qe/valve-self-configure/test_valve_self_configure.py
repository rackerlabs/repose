#!/usr/bin/env python

from narwhal import repose
import requests
import unittest2 as unittest
from narwhal import conf
from narwhal import pathutil
import xmlrunner as _xmlrunner
import logging
import time
import argparse
import os

logger = logging.getLogger(__name__)


target_hostname = '50.57.189.15'
target_port = 8080
config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deploy_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')
stop_port = 7777


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
    conf.process_folder_contents(folder='configs/%s' % config_set_name, verbose=False,
                                 dest_path=config_dir, params=params)


class TestMultiClusterMultiNode(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port11 = 18888
        self.port12 = 18889
        self.port21 = 18890
        self.port22 = 18891

        pathutil.clear_folder(config_dir)
        self.params = {
            'proto': 'http',
            'target_hostname': target_hostname,
            'target_port': target_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
            'port11' : self.port11,
            'port12' : self.port12,
            'port21' : self.port21,
            'port22' : self.port22,
        }
        apply_config_set('valve-self-common', params=self.params)
        apply_config_set('container-no-port', params=self.params)
        apply_config_set('two-clusters-two-nodes-each', params=self.params)
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(45)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
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

        url = 'http://localhost:%i/' % (self.port11)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port12)
        self.make_request_and_assert_connection_fails(url)

        url = 'http://localhost:%i/' % (self.port21)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port22)
        self.make_request_and_assert_connection_fails(url)


class TestRuntimeSysmodChanges(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port1 = 11111
        self.port2 = 22222
        self.port3 = 33333

        pathutil.clear_folder(config_dir)
        params = {
            'proto': 'http',
            'target_hostname': target_hostname,
            'target_port': target_port,
            'sysmod_port' : self.port1,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('container-no-port', params=params)
        apply_config_set('single-node-with-proto', params=params)
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(25)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
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
            'node1host' : 'localhost',
            'node2host' : 'localhost',
            'node1port' : self.port1,
            'node2port' : self.port2,
        }
        apply_config_set('two-nodes', params=params)
        time.sleep(25)

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
            'sysmod_port' : self.port2,
        }
        apply_config_set('single-node-with-proto', params=params)
        time.sleep(25)

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
            'node1host' : 'localhost',
            'node2host' : 'localhost',
            'node3host' : 'example.com',
            'node1port' : self.port1,
            'node2port' : self.port2,
            'node3port' : self.port3,
        }
        apply_config_set('three-nodes', params=params)
        time.sleep(25)

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
            'node1host' : 'example.com',
            'node2host' : 'localhost',
            'node3host' : 'localhost',
            'node1port' : self.port1,
            'node2port' : self.port2,
            'node3port' : self.port3,
        }
        apply_config_set('three-nodes', params=params)
        time.sleep(25)

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

        self.port = 11111

        pathutil.clear_folder(config_dir)
        params = {
            'target_hostname': target_hostname,
            'target_port': target_port,
            'host' : 'example.com',
            'port' : self.port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
        }
        apply_config_set('valve-self-common', params=params)
        apply_config_set('container-no-port', params=params)
        apply_config_set('one-node', params=params)
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(25)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
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
            'host' : 'localhost',
            'port' : self.port,
        }
        apply_config_set('one-node', params=params)
        time.sleep(25)

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_status_code(url, 200)


class TestStartWithZeroNodes(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.port = 11111

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
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(25)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
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
            'host' : 'localhost',
            'port' : self.port,
        }
        apply_config_set('one-node', params=params)
        time.sleep(25)

        url = 'http://localhost:%i/' % (self.port)
        self.make_request_and_assert_status_code(url, 200)


class TestPortsOnCommandLineBase:
    def setUp(self):
        logger.debug('setUp')

        self.init_params()

        pathutil.clear_folder(config_dir)
        self.params = {
            'proto': self.proto,
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'cmd_line_port': self.cmd_line_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=self.params)
        apply_config_set('single-node-with-proto', params=self.params)
        apply_config_set('container-no-port', params=self.params)
        self.repose = self.start_repose()
        time.sleep(25)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
            time.sleep(5)

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port specified at the command line
        url = '%s://localhost:%i/' % (self.params['proto'], self.cmd_line_port)
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(status_code, 200)


class TestPortsOnCommandLineHttpSame(TestPortsOnCommandLineBase,
                                     unittest.TestCase):
    def start_repose(self):
        return repose.ReposeValve(config_dir=config_dir,
                                  port=self.cmd_line_port,
                                  stop_port=stop_port)

    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.cmd_line_port = 8888


class TestPortsOnCommandLineHttpsSame(TestPortsOnCommandLineBase,
                                      unittest.TestCase):
    def start_repose(self):
        return repose.ReposeValve(config_dir=config_dir,
                                  https_port=self.cmd_line_port,
                                  stop_port=stop_port)

    def init_params(self):
        self.proto = 'https'
        self.sysmod_port = 8888
        self.cmd_line_port = 8888


class TestPortsOnCommandLineHttpDiff(TestPortsOnCommandLineBase,
                                     unittest.TestCase):
    def start_repose(self):
        return repose.ReposeValve(config_dir=config_dir,
                                  port=self.cmd_line_port,
                                  stop_port=stop_port)

    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.cmd_line_port = 8889

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port specified at the command line
        url = '%s://localhost:%i/' % (self.params['proto'], self.cmd_line_port)
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)


class TestPortsOnCommandLineNone(TestPortsOnCommandLineBase,
                                 unittest.TestCase):
    def start_repose(self):
        return repose.ReposeValve(config_dir=config_dir,
                                  stop_port=stop_port)

    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.cmd_line_port = 8889

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port specified at the command line
        url = '%s://localhost:%i/' % (self.params['proto'], self.cmd_line_port)
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)


class TestPortsInContainerBase:
    def setUp(self):
        logger.debug('setUp')

        self.init_params()

        pathutil.clear_folder(config_dir)
        self.params = {
            'proto': self.proto,
            'sysmod_port': self.sysmod_port,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': self.con_port,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('valve-self-common', params=self.params)
        apply_config_set('single-node-with-proto', params=self.params)
        apply_config_set(self.main_config_set_name, params=self.params)
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(25)

    def tearDown(self):
        logger.debug('tearDown')
        if self.repose is not None:
            self.repose.stop()
            time.sleep(5)

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port in the container
        url = '%s://localhost:%i/' % (self.params['proto'], self.con_port)
        logger.debug('runTest: con url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: con status_code = %i' % status_code)
        self.assertEqual(status_code, 200)


class TestPortsInContainerHttpSame(TestPortsInContainerBase,
                                   unittest.TestCase):
    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.con_port = 8888
        self.main_config_set_name = 'container-with-port'


class TestPortsInContainerHttpsSame(TestPortsInContainerBase,
                                    unittest.TestCase):
    def init_params(self):
        self.proto = 'https'
        self.sysmod_port = 8888
        self.con_port = 8888
        self.main_config_set_name = 'container-with-port'


class TestPortsInContainerHttpDiff(TestPortsInContainerBase,
                                   unittest.TestCase):
    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.con_port = 8889
        self.main_config_set_name = 'container-with-port'

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port in the container
        url = '%s://localhost:%i/' % (self.params['proto'], self.con_port)
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)


class TestPortsInContainerNone(TestPortsInContainerBase, unittest.TestCase):
    def init_params(self):
        self.proto = 'http'
        self.sysmod_port = 8888
        self.con_port = 8889
        self.main_config_set_name = 'container-no-port'

    def runTest(self):
        logger.debug('runTest')

        # test port in the system model
        url = '%s://localhost:%i/' % (self.params['proto'], self.sysmod_port)
        logger.debug('runTest: sysmod url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('runTest: sysmod status_code = %i' % status_code)
        self.assertEqual(status_code, 200)

        # test port in the container
        url = '%s://localhost:%i/' % (self.params['proto'], self.con_port)
        logger.debug('runTest: con url = %s' % url)
        self.assertRaises(requests.ConnectionError, get_status_code_from_url,
                          url)


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

    test_runner = _xmlrunner.XMLTestRunner(output='test-reports')

    unittest.main(argv=[''], testRunner=test_runner)



if __name__ == '__main__':
    run()
