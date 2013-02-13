#!/usr/bin/env python

import repose
import requests
import unittest
import conf
import pathutil
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
    conf.process_config_set(config_set_name, verbose=False,
                            destination_path=config_dir, params=params)


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


def run():
    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', help="Print the log to STDERR.",
                        action='store_true')
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG)

    logger.debug('run')
    setUpModule()

    suite = unittest.TestSuite()

    loader = unittest.TestLoader()
    load_tests = loader.loadTestsFromTestCase
    suite.addTest(load_tests(TestPortsOnCommandLineHttpSame))
    suite.addTest(load_tests(TestPortsOnCommandLineHttpsSame))
    suite.addTest(load_tests(TestPortsOnCommandLineHttpDiff))
    suite.addTest(load_tests(TestPortsOnCommandLineNone))

    testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

    result = testRunner.run(suite)


if __name__ == '__main__':
    run()
