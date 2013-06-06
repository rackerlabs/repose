#!/usr/bin/env python
import unittest2 as unittest
import xmlrunner
from narwhal import valve
from narwhal import conf
from narwhal import pathutil
from narwhal.download_repose import ReposeMavenConnector
import zlib
import deproxy
import requests
import logging
import argparse
import xml.etree.ElementTree as ET
from xml.parsers.expat import ExpatError
import sys
import os
import glob
import string
import base64
import datetime
from pprint import pprint
import re
import time

logger = logging.getLogger(__name__)


config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deployment_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')
repose_port = 8888
stop_port = 7777
deproxy_port_base = 9999

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


class TestCatalogService(unittest.TestCase):
    """
    Tests will check whether catalog service exists
    """

    long_message = True

    def setUp(self):
        logger.debug('setUp')

        deproxy_port = deproxy_port_base - 0

        self.deproxy = deproxy.Deproxy()
        self.end_point = self.deproxy.add_endpoint(deproxy_port,'localhost')

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
        self.valve = valve.Valve(config_dir=config_dir,
                                        stop_port=stop_port, port=repose_port,
                                        wait_on_start=True,insecure=True)
        time.sleep(startup_wait_time)


    def get_auth_token(self):
        #authentication key
        url = 'https://staging.identity.api.rackspacecloud.com/v2.0/tokens'
        request_body = '<auth xmlns="http://docs.openstack.org/identity/api/v2.0"><passwordCredentials username="repose" password="PY3VI3d3rVkqtwCj"/></auth>'
        mc = self.deproxy.make_request(method='POST',url=url,headers={'Accept':'application/xml','Content-Type':'application/xml'},request_body=request_body)
        try:
            body = zlib.decompress(bytes(mc.received_response.body),15+32)
            print body
            tree = ET.fromstring(body)
            token = None
            catalog = None
           # print ET.tostringlist(tree.find("{http://docs.openstack.org/identity/api/v2.0}serviceCatalog").getchildren(), encoding='utf-8', method='xml')
            for child_of_root in tree:
                if child_of_root.tag == "{http://docs.openstack.org/identity/api/v2.0}token":
                    token = child_of_root.attrib["id"]
                if child_of_root.tag == "{http://docs.openstack.org/identity/api/v2.0}serviceCatalog":
                    print child_of_root.tag
                    print child_of_root.text
                    catalog = child_of_root.text
            return (token, catalog)
        except ExpatError:
            logger.debug( 'Unable to parse response body for the authenticated call')

    def test_catalog_exists(self):
        logger.debug('test_catalog_exists')
        (auth_token, catalog) = self.get_auth_token()
        url = 'http://localhost:%i/' % repose_port
        logger.debug('url = %s' % url)
        logger.debug(auth_token)
        headers = {'X-Auth-Token': auth_token}

        time.sleep(1)

        logger.debug('make request')
        mc = self.deproxy.make_request(method='GET', url=url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)
        self.assertIsNotNone(mc.handlings[0].request.headers['x-catalog'])

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

    testRunner = xmlrunner.XMLTestRunner(output='test-reports')

    result = testRunner.run(suite)


if __name__ == '__main__':
    run()
