#!/usr/bin/env python

import repose
import requests
import unittest
import conf
import pathutil
import xmlrunner as _xmlrunner
import logging
import time

logger = logging.getLogger(__name__)


target_hostname = '50.57.189.15'
target_port = 8080
config_dir = 'etc/repose'
deploy_dir = 'var/repose'
artifact_dir = 'usr/share/repose/filters'
log_file = 'var/log/repose/current.log'
stop_port = 7777


def setUpModule():
    # Set up folder hierarchy and install repose JAR/EARs if needed
    logger.debug('setUpModule')
    pass


def get_status_code_from_url(url, timeout=None):
    logger.debug('get_status_code_from_url(url="%s")' % url)
    return requests.get(url, timeout=timeout).status_code


class TestPortsInContainerHttpSame(unittest.TestCase):
    def setUp(self):
        logger.debug('TestPortsInContainerHttpSame.setUp')
        pathutil.clear_folder(config_dir)
        self.sysmod_port = 8888
        params = {
            'proto': 'http',
            'sysmod_port': 8888,
            'target_hostname': target_hostname,
            'target_port': target_port,
            'con_port': 8888,
            'deploy_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        conf.process_config_set('valve-self-common',
                                destination_path=config_dir, params=params)
        conf.process_config_set('valve-self-1-common',
                                destination_path=config_dir, params=params)
        conf.process_config_set('valve-self-1-with-con-port',
                                destination_path=config_dir, params=params)
        self.repose = repose.ReposeValve(config_dir=config_dir,
                                         stop_port=stop_port)
        time.sleep(20)

    def tearDown(self):
        logger.debug('TestPortsInContainerHttpSame.tearDown')
        if self.repose is not None:
            self.repose.stop()

    def runTest(self):
        logger.debug('TestPortsInContainerHttpSame.runTest')
        url = 'http://localhost:%i/' % self.sysmod_port
        logger.debug('TestPortsInContainerHttpSame.runTest: url = %s' % url)
        status_code = get_status_code_from_url(url)
        logger.debug('TestPortsInContainerHttpSame.runTest: status_code = %i' %
                     status_code)
        self.assertEqual(status_code, 200)


class TestPortsInContainerHttpsSame(unittest.TestCase):
    pass


class TestPortsInContainerHttpDiff(unittest.TestCase):
    pass


class TestPortsInContainerNone(unittest.TestCase):
    pass


def run():
    logger.debug('run')
    setUpModule()

    suite = unittest.TestSuite()

    loader = unittest.TestLoader()
    load_tests = loader.loadTestsFromTestCase
    suite.addTest(load_tests(TestPortsInContainerHttpSame))
    suite.addTest(load_tests(TestPortsInContainerHttpsSame))
    suite.addTest(load_tests(TestPortsInContainerHttpDiff))
    suite.addTest(load_tests(TestPortsInContainerNone))

    testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

    result = testRunner.run(suite)


if __name__ == '__main__':
    run()