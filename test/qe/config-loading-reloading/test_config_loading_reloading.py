#!/usr/bin/env python

from narwhal import repose
from narwhal import conf
import requests
import time
import sys
import unittest
#import install_repose
import xmlrunner as _xmlrunner
import logging
import SocketServer
import BaseHTTPServer
import threading
import os
import os.path


logger = logging.getLogger(__name__)

mock_port = 8894
mock_service = None
mock_url = 'http://localhost:%i/' % mock_port


class MockService(SocketServer.ThreadingMixIn, BaseHTTPServer.HTTPServer):
    pass


class MockServiceHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        return

    def log_message(self, format, *args):
        # override the log_message method to prevent the base class from
        # printing to stderr
        logger.info("%s - - [%s] %s\n" % (self.client_address[0],
                                          self.log_date_time_string(),
                                          format % args))


def mock_service_thread():
    logger.debug('thread created for mock service')
    mock_service.serve_forever()


def create_mock():
    # create a simple HTTP server that returns a 200 response to all requests
    logger.debug('create_mock')
    global mock_service
    if mock_service is not None:
        return

    # create the mock service object, but don't start it yet
    mock_service = MockService(('localhost', mock_port), MockServiceHandler)
    logger.debug('mock service created')

    # start the mock server on a separate thread, so that we can make
    # requests at the same time that i's serving them.
    t = threading.Thread(target=mock_service_thread)
    t.daemon = True
    t.start()

    requests.get(mock_url)
    logger.debug('mock service is serving requests.')


def destroy_mock():
    global mock_service
    if mock_service is None:
        return
    mock_service.shutdown()


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
        logger.debug('setUp (%s)' % self.__class__.__name__)
        name = self.get_name()
        config_base = 'configs/config-load-test-'
        self.config_good = config_base + name + '-good/.config-set.xml'
        self.config_bad = config_base + name + '-bad/.config-set.xml'
        self.config_common = config_base + name + '-common/.config-set.xml'
        self.repose_config_folder = 'etc/repose'
        self.repose_port = 8893
        self.repose_stop_port = 9893
        self.repose_url = 'http://localhost:%s' % self.repose_port
        self.config_params = {'port': str(self.repose_port),
                              'target_hostname': 'localhost',
                              'target_port': mock_port}
        self.sleep_time = 25

    def get_request_timeout(self):
        return 30

    def get_status_code_from_url(self, url):
        timeout = self.get_request_timeout()
        return requests.get(url, timeout=timeout).status_code

    def get_good_response(self):
        return 200

    def get_bad_response(self):
        return 503

    def _test_start(self, config_folder, config_sets, params, expected_result):
        r = None
        try:
            create_folder(self.repose_config_folder)
            clear_folder(self.repose_config_folder)
            for config_set in config_sets:
                conf.process_config_set(config_set,
                                        params=params,
                                        destination_path=config_folder,
                                        verbose=False)
            r = repose.ReposeValve(config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            try:
                expected_code = int(expected_result)
            except TypeError:
                self.assertRaises(expected_result, requests.get,
                                  self.repose_url)
            else:
                actual_code = self.get_status_code_from_url(self.repose_url)
                self.assertEquals(actual_code, expected_code)
        finally:
            if r:
                r.stop()

    def _test_transition(self,
                         config_params,
                         starting_config_sets,
                         expected_response_on_start,
                         transition_config_sets,
                         expected_response_on_transition):
        r = None
        try:
            create_folder(self.repose_config_folder)
            clear_folder(self.repose_config_folder)
            for config_set in starting_config_sets:
                conf.process_config_set(config_set,
                                        params=config_params,
                                        destination_path=
                                            self.repose_config_folder,
                                        verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)

            try:
                expected_code = int(expected_response_on_start)
            except TypeError:
                self.assertRaises(expected_response_on_start, requests.get,
                                  self.repose_url)
            else:
                actual_code = self.get_status_code_from_url(self.repose_url)
                self.assertEquals(actual_code, expected_code)

            for config_set in transition_config_sets:
                conf.process_config_set(config_set,
                                        params=self.config_params,
                                        destination_path=
                                            self.repose_config_folder,
                                        verbose=False)
            time.sleep(self.sleep_time)

            try:
                expected_code = int(expected_response_on_transition)
            except TypeError:
                self.assertRaises(expected_response_on_transition,
                                  requests.get, self.repose_url)
            else:
                actual_code = self.get_status_code_from_url(self.repose_url)
                self.assertEquals(actual_code, expected_code)
        finally:
            if r:
                r.stop()

    def test_start_good(self):
        self._test_start(self.repose_config_folder,
                         [self.config_common, self.config_good, ],
                         self.config_params, self.get_good_response())

    def test_start_bad(self):
        self._test_start(self.repose_config_folder,
                         [self.config_common, self.config_bad, ],
                         self.config_params, self.get_bad_response())

    def test_good_to_bad(self):
        self._test_transition(
            config_params=self.config_params,
            starting_config_sets=[
                self.config_common,
                self.config_good,
            ],
            expected_response_on_start=self.get_good_response(),
            transition_config_sets=[self.config_bad],
            expected_response_on_transition=self.get_good_response())

    def test_bad_to_good(self):
        self._test_transition(
            config_params=self.config_params,
            starting_config_sets=[
                self.config_common,
                self.config_bad,
            ],
            expected_response_on_start=self.get_bad_response(),
            transition_config_sets=[self.config_good],
            expected_response_on_transition=self.get_good_response())


class TestNonStartingOnBadConfig(TestConfigLoadingReloading):
    def test_start_bad(self):
        self._test_start(self.repose_config_folder,
                         [ self.config_common, self.config_bad, ],
                         self.config_params, requests.ConnectionError)

    def test_bad_to_good(self):
        self._test_transition(
            config_params=self.config_params,
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
            create_folder(self.repose_config_folder)
            clear_folder(self.repose_config_folder)
            conf.process_config_set(self.config_common,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)

            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
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
    try:
        setUpModule()

        suite = unittest.TestSuite()

        loader = unittest.TestLoader()
        load_tests = loader.loadTestsFromTestCase
        suite.addTest(load_tests(TestClientAuthNConfig))
        suite.addTest(load_tests(TestContainerConfig))
        suite.addTest(load_tests(TestDistDatastoreConfig))
        suite.addTest(load_tests(TestHeaderIdentityConfig))
        suite.addTest(load_tests(TestHttpLoggingConfig))
        suite.addTest(load_tests(TestIpIdentityConfig))
        suite.addTest(load_tests(TestOpenstackAuthorizationConfig))
        suite.addTest(load_tests(TestRateLimitingConfig))
        suite.addTest(load_tests(TestResponseMessagingConfig))
        suite.addTest(load_tests(TestSystemModelConfig))
        suite.addTest(load_tests(TestTranslationConfig))
        suite.addTest(load_tests(TestUriIdentityConfig))
        suite.addTest(load_tests(TestVersioningConfig))

        testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

        result = testRunner.run(suite)

    finally:
        tearDownModule()


if __name__ == '__main__':
    run()
