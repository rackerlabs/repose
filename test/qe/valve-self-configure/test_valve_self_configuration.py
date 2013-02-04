#!/usr/bin/env python

import repose
import requests
import unittest
import conf
import pathutil
import xmlrunner as _xmlrunner


target_hostname = '50.'
target_port = 8080
config_dir = 'etc/repose'
deploy_dir = 'var/repose'
artifact_dir = 'usr/share/repose/filters'
log_file = 'var/log/repose/current.log'
stop_port = 7777


def setUpModule():
    # Set up folder hierarchy and install repose JAR/EARs if needed
    pass


def get_status_code_from_url(url, timeout=None):
    return requests.get(url, timeout=timeout).status_code


class TestPortsInContainerHttpSame(unittest.TestCase):
    def setUp(self):
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

    def tearDown(self):
        if self.repose is not None:
            self.repose.stop()

    def runTest(self):
        self.assertEqual(get_status_code_from_url('http://localhost:%i/' % self.sysmod_port))


class TestPortsInContainerHttpsSame(unittest.TestCase):
    pass


class TestPortsInContainerHttpDiff(unittest.TestCase):
    pass


class TestPortsInContainerNone(unittest.TestCase):
    pass


def run():
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