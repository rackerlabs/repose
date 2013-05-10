#!/usr/bin/env python

from narwhal import repose
import unittest
from narwhal import conf
from narwhal import pathutil
import xmlrunner as _xmlrunner
import logging
import time
import argparse
import os
import deproxy

logger = logging.getLogger(__name__)

config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deployment_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')
repose_port = 8888
stop_port = 7777
deproxy_port = 9999
headers = {}

startup_wait_time = 15


def setUpModule():
    # Set up folder hierarchy
    logger.debug('setUpModule')
    pathutil.create_folder(config_dir)
    pathutil.create_folder(deployment_dir)
    pathutil.create_folder(os.path.dirname(log_file))

config_verbose = False


def apply_config_set(config_set_name, params=None):
    if params is None:
        params = {}
    conf.process_config_set(config_set_name, verbose=config_verbose,
                            destination_path=config_dir, params=params)


class TestCompression(unittest.TestCase):

    def setUp(self):
        logger.debug('setUp')

        self.deproxy = deproxy.Deproxy()
        self.end_point = self.deproxy.add_endpoint(('localhost', deproxy_port))

        pathutil.clear_folder(config_dir)
        params = {
            'port': repose_port,
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'deployment_dir': deployment_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('configs/.config-set.xml', params=params)
        self.valve = repose.ReposeValve(config_dir=config_dir,
                                        stop_port=stop_port)
      
	time.sleep(startup_wait_time)

    def test_compression_with_gzip(self):
        logger.debug('test_compression_with_gzip')
        url = 'http://localhost:%i/' % repose_port
        logger.debug('url = %s' % url)

        time.sleep(1)

        mc = self.deproxy.make_request(method='GET', url=url,
                                           headers=headers)
        self.assertEqual(mc.received_response.code, '200', msg=mc)
        self.assertEqual(len(mc.handlings), 1, msg=mc)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()



available_test_cases = [
    TestCompression
]


def run():
    test_case_map = dict()
    for tc_class in available_test_cases:
        test_case_map[tc_class.__name__] = tc_class

    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', help="Print the log to STDERR.",
                        action='store_true')
    parser.add_argument('--test-case', action='append',
                        help="Which test case to run. Can be specififed "
                        "multiple times. 'all' is the default, and runs all "
                        "available test cases",
                        choices=['all'] + test_case_map.keys(),
                        type=str)
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG,
                            format=('%(asctime)s %(levelname)s:%(name)s:'
                                    '%(funcName)s:'
                                    '%(filename)s(%(lineno)d):'
                                    '%(threadName)s(%(thread)d):%(message)s'))
        global config_verbose
        config_verbose = True

    if args.test_case is None:
        args.test_case = ['all']

    test_cases = []
    test_cases_set = set()
    for tc in args.test_case:
        if tc == 'all':
            test_cases = available_test_cases
            break
        if tc not in test_cases_set:
            test_cases_set.add(tc)
            test_cases.append(test_case_map[tc])

    logger.debug('run')
    setUpModule()

    suite = unittest.TestSuite()

    loader = unittest.TestLoader()
    load_tests = loader.loadTestsFromTestCase

    for test_case in test_cases:
        suite.addTest(load_tests(test_case))

    testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

    result = testRunner.run(suite)


if __name__ == '__main__':
    run()
