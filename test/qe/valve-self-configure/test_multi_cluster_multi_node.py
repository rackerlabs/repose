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

        url = 'http://localhost:%i/' % (self.port11)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port12)
        self.make_request_and_assert_connection_fails(url)

        url = 'http://localhost:%i/' % (self.port21)
        self.make_request_and_assert_status_code(url, 200)

        url = 'http://localhost:%i/' % (self.port22)
        self.make_request_and_assert_connection_fails(url)



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
    suite.addTest(load_tests(TestMultiClusterMultiNode))

    testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

    result = testRunner.run(suite)


if __name__ == '__main__':
    run()
