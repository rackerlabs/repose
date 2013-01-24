#!/usr/bin/env python

import repose
import conf
import pathutil
import requests
import time
import sys
import unittest
#import install_repose
import xmlrunner as _xmlrunner
import logging

logger = logging.getLogger(__name__)

target_hostname = 'localhost'
target_port = 8894
target_config_folder = 'etc/repose2'
target_repose = None

def create_target():
    # stand up a repose node with no filters and no destinations
    # it will simply return 200's for all requests

    global target_repose
    if target_repose is not None: return
    logger.debug('Creating target Repose node')
    pathutil.create_folder(target_config_folder)
    conf.process_config_set(config_set_name='simple-node',
                            destination_path='etc/repose2',
                            params={
                                'port': str(target_port),
                                'deploydir': 'var/repose',
                                'artifactdir': 'usr/share/repose/filters',
                                'logfile': 'var/log/repose/current2.log'},
                            verbose=False)
    target_repose = repose.ReposeValve(target_config_folder, stop_port=9894)
    time.sleep(25)
    logger.debug('target node started (pid=%i)' % target_repose.proc.pid)


def destroy_target():
    global target_repose
    if target_repose is None: return
    logger.debug('Destorying target Repose node')
    target_repose.stop()


def setUpModule():
    logger.debug('setUpModule')
    repose_conf_folder = 'etc/repose'
    pathutil.delete_folder(repose_conf_folder)
    pathutil.create_folder(repose_conf_folder)
    pathutil.create_folder('usr/share/repose/filters')
    pathutil.create_folder('var/log/repose')
    pathutil.create_folder('var/repose')
    #install_repose.get_repose(valve_dest='usr/share/repose',
    #                          ear_dest='usr/share/repose/filters')
    create_target()
    logger.debug('setUpModule complete')


def tearDownModule():
    destroy_target()


class TestConfigLoadingReloading:
    def setUp(self):
        logger.debug('setUp (%s)' % self.__class__.__name__)
        name = self.get_name()
        config_base = 'config-load-test-'
        self.config_good = config_base + name + '-good'
        self.config_bad = config_base + name + '-bad'
        self.repose_config_folder = 'etc/repose'
        self.repose_port = 8893
        self.repose_stop_port = 9893
        self.repose_url = 'http://localhost:%s' % self.repose_port
        self.config_params = {'port': str(self.repose_port),
                              'target_hostname': target_hostname,
                              'target_port': target_port}
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

    def test_start_good(self):
        logger.debug('test_start_good')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_good,
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

    def test_start_bad(self):
        logger.debug('test_start_bad')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_bad,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_bad_response())
        finally:
            if r:
                r.stop()

    def test_good_to_bad(self):
        logger.debug('test_good_to_bad')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_good,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_good_response())
            conf.process_config_set(self.config_bad,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_good_response())
        finally:
            if r:
                r.stop()

    def test_bad_to_good(self):
        logger.debug('test_bad_to_good')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_bad,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_bad_response())
            conf.process_config_set(self.config_good,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_good_response())
        finally:
            if r:
                r.stop()


class TestNonStartingOnBadConfig(TestConfigLoadingReloading):
    def test_start_bad(self):
        logger.debug('test_start_bad (2)')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_bad,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            with self.assertRaises(requests.ConnectionError):
                response = requests.get(self.repose_url)
        finally:
            if r:
                r.stop()

    def test_bad_to_good(self):
        logger.debug('test_bad_to_good (2)')
        r = None
        try:
            pathutil.delete_folder(self.repose_config_folder)
            pathutil.create_folder(self.repose_config_folder)
            conf.process_config_set(self.config_bad,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            r = repose.ReposeValve(self.repose_config_folder,
                                   stop_port=self.repose_stop_port)
            time.sleep(self.sleep_time)
            self.assertRaises(requests.ConnectionError, requests.get,
                              self.repose_url)
            conf.process_config_set(self.config_good,
                                    params=self.config_params,
                                    destination_path=self.repose_config_folder,
                                    verbose=False)
            time.sleep(self.sleep_time)
            self.assertEquals(self.get_status_code_from_url(self.repose_url),
                              self.get_good_response())
        finally:
            if r:
                r.stop()


class TestClientAuthNConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'client-auth-n'


class TestContainerConfig(TestNonStartingOnBadConfig, unittest.TestCase):
    def get_name(self):
        return 'container'


class TestDistDatastoreConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'dist-datastore'


class TestHeaderIdentityConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'header-identity'


class TestHttpLoggingConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'http-logging'


class TestIpIdentityConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'ip-identity'


class TestOpenstackAuthorizationConfig(TestConfigLoadingReloading,
                                       unittest.TestCase):
    def get_name(self):
        return 'openstack-authorization'

    def get_good_response(self):
        return 401


class TestRateLimitingConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'rate-limiting'


class TestResponseMessagingConfig(TestConfigLoadingReloading,
                                  unittest.TestCase):
    def get_name(self):
        return 'response-messaging'


class TestSystemModelConfig(TestNonStartingOnBadConfig, unittest.TestCase):
    def get_name(self):
        return 'system-model'


class TestTranslationConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'translation'


class TestUriIdentityConfig(TestConfigLoadingReloading, unittest.TestCase):
    def get_name(self):
        return 'uri-identity'


class TestVersioningConfig(TestConfigLoadingReloading, unittest.TestCase):
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

        testRunner = _xmlrunner.XMLTestRunner(output='test-reports')

        result = testRunner.run(suite)

    finally:
        tearDownModule()


if __name__ == '__main__':
    run()
