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
deproxy_port_base = 9999
headers = {'X-PP-User': 'user'}

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


class TestSimpleLimitGroup(unittest.TestCase):
    """Rate limiting is configured for 5 requests per second, of any HTTP
    method. Making 5 requests in succession should succeed with 200's, and
    a sixth request should go over the limit, resulting in a 413. The
    failing request should _not_ be sent to the origin service."""

    long_message = True

    def setUp(self):
        logger.debug('setUp')

        deproxy_port = deproxy_port_base - 0

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
        apply_config_set('configs/one-node/.config-set.xml', params=params)
        self.valve = repose.ReposeValve(config_dir=config_dir,
                                        stop_port=stop_port, port=repose_port,
                                        wait_on_start=True)
        time.sleep(startup_wait_time)

    def test_a_simple_limit(self):
        logger.debug('test_a_simple_limit')

        url = 'http://localhost:%i/' % repose_port
        logger.debug('url = %s' % url)

        for i in xrange(5):
            logger.debug('%i\'th request, should pass' % i)
            mc = self.deproxy.make_request(method='GET', url=url,
                                           headers=headers)
            self.assertEqual(mc.received_response.code, '200')
            self.assertEqual(len(mc.handlings), 1)

        # the sixth request will not go through
        logger.debug('last request, should bounce')
        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '413')
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


class TestMultipleMethodsForTheSameLimitGroup(unittest.TestCase):
    """Rate limiting is configured for 5 requests per second, of any HTTP
    method. Making requests of different methods should still count towards
    the limit in the usual way, and the sixth request should fail."""

    long_message = True

    def setUp(self):
        logger.debug('setUp')

        deproxy_port = deproxy_port_base - 1

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
        apply_config_set('configs/one-node/.config-set.xml', params=params)
        self.valve = repose.ReposeValve(config_dir=config_dir,
                                        stop_port=stop_port, port=repose_port,
                                        wait_on_start=True)
        time.sleep(startup_wait_time)

    def test_different_methods(self):
        url = 'http://localhost:%i/' % repose_port

        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='POST', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='PUT', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='DELETE', url=url,
                                       headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='HEAD', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '413')
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


class TestLimitsResetAfterTime(unittest.TestCase):
    """Rate limiting is configured for 5 requests per second, of any HTTP
    method. Making 5 requests in succession should succeed with 200's, and
    a sixth request should go over the limit, resulting in a 413. If we
    sleep for 1 second after that, limits should reset."""

    long_message = True

    def setUp(self):
        logger.debug('setUp')

        deproxy_port = deproxy_port_base - 2

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
        apply_config_set('configs/one-node/.config-set.xml', params=params)
        self.valve = repose.ReposeValve(config_dir=config_dir,
                                        stop_port=stop_port, port=repose_port,
                                        wait_on_start=True)
        time.sleep(startup_wait_time)

    def test_reset(self):
        logger.debug('test_reset')

        url = 'http://localhost:%i/' % repose_port
        logger.debug('url = %s' % url)

        for i in xrange(5):
            mc = self.deproxy.make_request(method='GET', url=url,
                                           headers=headers)
            self.assertEqual(mc.received_response.code, '200')
            self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '413')
        self.assertEqual(len(mc.handlings), 0)

        #sleep for one minute to allow counts to reset
        time.sleep(60)

        for i in xrange(5):
            mc = self.deproxy.make_request(method='GET', url=url,
                                           headers=headers)
            self.assertEqual(mc.received_response.code, '200')
            self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '413')
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


class TestMultipleNodes(unittest.TestCase):
    """Rate limit info should be shared among multiple nodes that are
    configured to use the distributed datastore. Requests over the prescribed
    limit should be rejected, no matter which node serviced them."""

    long_message = True

    def setUp(self):
        logger.debug('setUp')

        deproxy_port = deproxy_port_base - 3

        self.deproxy = deproxy.Deproxy()
        self.end_point = self.deproxy.add_endpoint(('localhost', deproxy_port))

        pathutil.clear_folder(config_dir)
        params = {
            'port1': repose_port,
            'port2': repose_port + 1,
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'deployment_dir': deployment_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file
        }
        apply_config_set('configs/two-nodes/.config-set.xml', params=params)
        self.valve = repose.ReposeValve(config_dir=config_dir,
                                        stop_port=stop_port)
        repose.wait_for_node_to_start(port=repose_port)
        repose.wait_for_node_to_start(port=repose_port + 1)

        time.sleep(startup_wait_time)

    def test_multiple_node(self):
        logger.debug('test_reset')

        url1 = 'http://localhost:%i/' % repose_port
        url2 = 'http://localhost:%i/' % (repose_port + 1)
        logger.debug('url1 = %s' % url1)
        logger.debug('url2 = %s' % url2)

        for i in xrange(5):
            mc = self.deproxy.make_request(method='GET', url=url1,
                                           headers=headers)
            self.assertEqual(mc.received_response.code, '200')
            self.assertEqual(len(mc.handlings), 1)

        mc = self.deproxy.make_request(method='GET', url=url2, headers=headers)
        self.assertEqual(mc.received_response.code, '413')
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


available_test_cases = [
    v for v in globals().values()
        if type(v) == type and issubclass(v, unittest.TestCase)]


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
