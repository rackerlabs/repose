#!/usr/bin/env python

from narwhal import repose
from narwhal import conf
import requests
import time
import sys
import unittest2 as unittest
import xmlrunner as _xmlrunner
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


def create_mock():
    # create a simple HTTP server that returns a 200 response to all requests
    logger.debug('create_mock')
    global mock_service
    if mock_service is not None:
        return

    mock_service = deproxy.Deproxy()
    mock_service.add_endpoint(('localhost', mock_port))


def destroy_mock():
    if mock_service is None:
        return
    mock_service.shutdown_all_endpoints()


def setUpModule():
    logger.debug('setUpModule')
    repose_conf_folder = 'etc/repose'
    create_folder(repose_conf_folder)
    clear_folder(repose_conf_folder)
    create_folder('var/log/repose')
    create_folder('var/repose')

    # the valve jar should be 'usr/share/repose/repose-valve.jar'
    # the filter ears should be in 'usr/share/repose/filters/'

    create_mock()
    logger.debug('setUpModule complete')


def tearDownModule():
    destroy_mock()


def clear_folder(folder_name):
    for _name in os.listdir(folder_name):
        name = os.path.join(folder_name, _name)
        if os.path.isdir(name):
            delete_folder(name)
        else:
            os.remove(name)


def create_folder(folder_name):
    if not os.path.exists(folder_name):
        os.makedirs(folder_name)


class TestConfigLoadingReloading(unittest.TestCase):
    def setUp(self):
        if self.__class__.__name__ in ['TestConfigLoadingReloading',
                                       'TestNonStartingOnBadConfig']:
            self.skipTest('Abstract base class')
            return
        logger.debug('setUp (%s)' % self.__class__.__name__)
        name = self.get_name()
        self.config_good = 'configs/%s-good/.config-set.xml' % name
        self.config_bad = 'configs/%s-bad/.config-set.xml' % name
        self.config_common = 'configs/%s-common/.config-set.xml' % name
        self.wait_on_start = True

    def get_status_code_from_url(self, url):
        return requests.get(url, timeout=request_timeout).status_code

    def get_good_response(self):
        return 200

    def get_bad_response(self):
        return 503

    def _test_start(self, config_folder, config_sets, params, expected_result):
        logger.debug('_test_start (%s)' % self.__class__.__name__)
        r = None
        try:
            create_folder(repose_config_folder)
            clear_folder(repose_config_folder)
            for config_set in config_sets:
                conf.process_config_set(config_set,
                                        params=params,
                                        destination_path=config_folder,
                                        verbose=False)
            r = repose.ReposeValve(config_folder,
                                   stop_port=repose_stop_port,
                                   port=repose_port,
                                   wait_on_start=self.wait_on_start)
            if not self.wait_on_start:
                time.sleep(sleep_time)

            try:
                expected_code = int(expected_result)
            except TypeError:
                self.assertRaises(expected_result, requests.get,
                                  repose_url)
            else:
                actual_code = self.get_status_code_from_url(repose_url)
                self.assertEquals(actual_code, expected_code)
        finally:
            if r:
                r.stop()

    def _test_transition(self, config_params, starting_config_sets,
                         expected_response_on_start, transition_config_sets,
                         expected_response_on_transition):
        logger.debug('_test_transition (%s)' % self.__class__.__name__)
        r = None
        try:
            create_folder(repose_config_folder)
            clear_folder(repose_config_folder)
            for config_set in starting_config_sets:
                conf.process_config_set(config_set,
                                        params=config_params,
                                        destination_path=repose_config_folder,
                                        verbose=False)
            r = repose.ReposeValve(repose_config_folder,
                                   stop_port=repose_stop_port,
                                   port=repose_port,
                                   wait_on_start=self.wait_on_start)
            if not self.wait_on_start:
                time.sleep(sleep_time)

            try:
                expected_code = int(expected_response_on_start)
            except TypeError:
                self.assertRaises(expected_response_on_start, requests.get,
                                  repose_url)
            else:
                actual_code = self.get_status_code_from_url(repose_url)
                self.assertEquals(actual_code, expected_code)

            for config_set in transition_config_sets:
                conf.process_config_set(config_set,
                                        params=config_params,
                                        destination_path=repose_config_folder,
                                        verbose=False)
            time.sleep(sleep_time)

            try:
                expected_code = int(expected_response_on_transition)
            except TypeError:
                self.assertRaises(expected_response_on_transition,
                                  requests.get, repose_url)
            else:
                actual_code = self.get_status_code_from_url(repose_url)
                self.assertEquals(actual_code, expected_code)
        finally:
            if r:
                r.stop()

    def test_start_good(self):
        logger.debug('test_start_good (%s)' % self.__class__.__name__)
        self._test_start(repose_config_folder,
                         [self.config_common, self.config_good, ],
                         config_params, self.get_good_response())

    def test_start_bad(self):
        logger.debug('test_start_bad (%s)' % self.__class__.__name__)
        self._test_start(repose_config_folder,
                         [self.config_common, self.config_bad, ],
                         config_params, self.get_bad_response())

    def test_good_to_bad(self):
        logger.debug('test_good_to_bad (%s)' % self.__class__.__name__)
        self._test_transition(
            config_params=config_params,
            starting_config_sets=[
                self.config_common,
                self.config_good,
            ],
            expected_response_on_start=self.get_good_response(),
            transition_config_sets=[self.config_bad],
            expected_response_on_transition=self.get_good_response())

    def test_bad_to_good(self):
        logger.debug('test_bad_to_good (%s)' % self.__class__.__name__)
        self._test_transition(
            config_params=config_params,
            starting_config_sets=[
                self.config_common,
                self.config_bad,
            ],
            expected_response_on_start=self.get_bad_response(),
            transition_config_sets=[self.config_good],
            expected_response_on_transition=self.get_good_response())


class TestNonStartingOnBadConfig(TestConfigLoadingReloading):
    def test_start_bad(self):
        logger.debug('test_start_bad (%s)' % self.__class__.__name__)
        self.wait_on_start = False
        self._test_start(repose_config_folder,
                         [self.config_common, self.config_bad],
                         config_params, requests.ConnectionError)

    def test_bad_to_good(self):
        logger.debug('test_bad_to_good (%s)' % self.__class__.__name__)
        self.wait_on_start = False
        self._test_transition(
            config_params=config_params,
            starting_config_sets=[
                self.config_common,
                self.config_bad,
            ],
            expected_response_on_start=requests.ConnectionError,
            transition_config_sets=[self.config_good],
            expected_response_on_transition=self.get_good_response())


class TestClientAuthNConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'client-auth-n'


class TestContainerConfig(TestNonStartingOnBadConfig):
    def get_name(self):
        return 'container'


class TestDistDatastoreConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'dist-datastore'


class TestHeaderIdentityConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'header-identity'


class TestHttpLoggingConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'http-logging'


class TestIpIdentityConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'ip-identity'


class TestOpenstackAuthorizationConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'openstack-authorization'

    def get_good_response(self):
        return 401


class TestRateLimitingConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'rate-limiting'


class TestResponseMessagingConfig(TestConfigLoadingReloading):
    def test_start_missing(self):
        logger.debug('test_start_missing')
        r = None
        try:
            create_folder(repose_config_folder)
            clear_folder(repose_config_folder)
            conf.process_config_set(self.config_common,
                                    params=config_params,
                                    destination_path=repose_config_folder,
                                    verbose=False)

            r = repose.ReposeValve(repose_config_folder,
                                   stop_port=repose_stop_port,
                                   port=repose_port,
                                   wait_on_start=self.wait_on_start)
            if not self.wait_on_start:
                time.sleep(sleep_time)

            self.assertEquals(self.get_status_code_from_url(repose_url),
                              self.get_good_response())
        finally:
            if r:
                r.stop()

    def get_name(self):
        return 'response-messaging'


class TestSystemModelConfig(TestNonStartingOnBadConfig):
    def get_name(self):
        return 'system-model'


class TestTranslationConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'translation'


class TestUriIdentityConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'uri-identity'


class TestVersioningConfig(TestConfigLoadingReloading):
    def get_name(self):
        return 'versioning'


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

    test_runner = _xmlrunner.XMLTestRunner(output='test-reports')

    try:
        setUpModule()
        unittest.main(argv=[''], testRunner=test_runner)
    finally:
        tearDownModule()


if __name__ == '__main__':
    run()
