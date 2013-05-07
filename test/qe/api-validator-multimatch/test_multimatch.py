#!/usr/bin/env python

"""
This is a test of the API Validator component, it's multimatch feature in
particular. We constructed a set of patterns

 -> Not considered (N)
 -> Considered (C)      -> Skipped (S)
 -> Considered (C)      -> Tried (T)    -> Passed (P)
 -> Considered (C)      -> Tried (T)    -> Failed (F)

transitions from one state to another

multimatch disabled: SSPNN, P, F
multimatch enabled: SSFSFFPNN, P, F

As an optimization, we create a separate validator.cfg.xml file and WADL files
for each pattern, and run them simultaneously in six separate clusters within
the system model.
"""

from narwhal import repose
import unittest2 as unittest
from narwhal import conf
from narwhal import pathutil
import xmlrunner as xmlrunner
import logging
import time
import argparse
import os
import deproxy
import itertools

logger = logging.getLogger(__name__)


config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deployment_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')
repose_port = 5555
stop_port = 7777
deproxy_port = 10101
d = None
url = None
params = {
    'target_hostname': 'localhost',
    'target_port': deproxy_port,
    'port': repose_port,
    'repose_port': repose_port,
}


def setUpModule():
    # Set up folder hierarchy
    logger.debug('setUpModule')
    pathutil.create_folder(deployment_dir)
    pathutil.create_folder(os.path.dirname(log_file))

    global d
    if d is None:
        d = deproxy.Deproxy()
        d.add_endpoint(('localhost', deproxy_port))

    global url
    url = 'http://localhost:%i/resource' % repose_port


def tearDownModule():
    logger.debug('')
    if d is not None:
        logger.debug('shutting down deproxy')
        d.shutdown_all_endpoints()
        logger.debug('deproxy shut down')


def apply_configs(folder):
    conf.process_folder_contents(folder=folder, dest_path='etc/repose',
                                 params=params)


def start_repose():
    return repose.ReposeValve(config_dir='etc/repose', stop_port=stop_port,
                              wait_on_start=True, port=repose_port)


def configure_and_start_repose(folder):
    # set the common config files, like system model and container
    apply_configs(folder='configs/common')
    # set the pattern-specific config files, i.e. validator.cfg.xml
    apply_configs(folder=folder)
    return start_repose()


class TestSspnn(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/sspnn')

    def test_sspnn(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    # the following test_* methods check how it responds to multiple roles in
    # different orders

    def test_pass_first_of_two(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3,role-4'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_pass_second_of_two(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-4,role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_fail_first_of_two(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2,role-3'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    def test_fail_second_of_two(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3,role-2'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestPAndS(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/p')

    def test_s(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertEqual(mc.received_response.code, '403')
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestF(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/f')

    def test_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestSfn(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/sfn')

    def test_sfn(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMssfsffpnn(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/mssfsffpnn')

    def test_mssfsffpnn(self):
        mc = d.make_request(url=url, headers={'X-Roles':
                                              'role-3,role-5,role-6,role-7'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_mssfsffsss(self):
        mc = d.make_request(url=url, headers={'X-Roles':
                                              'role-3,role-5,role-6'})
        self.assertEqual(mc.received_response.code, '405')
        self.assertEqual(len(mc.handlings), 0)

    def test_msssssspnn(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-7,role-8'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_mssfssspnn_order(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-7,role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMpAndMs(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/mp')

    def test_s(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertEqual(mc.received_response.code, '403')
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMf(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/mf')

    def test_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


def run():
    global deproxy_port
    global stop_port
    global repose_port

    parser = argparse.ArgumentParser()
    parser.add_argument('--repose-port', help='The port Repose will listen on '
                        'for requests. The default is %i.' % repose_port,
                        default=repose_port, type=int)
    parser.add_argument('--stop-port', help='The port Repose will listen on '
                        'for the stop command. The default is %i.' % stop_port,
                        default=stop_port, type=int)
    parser.add_argument('--deproxy-port', help='The port Deproxy will listen '
                        'on for requests forwarded from Repose. The default '
                        'is %i.' % deproxy_port, default=deproxy_port,
                        type=int)
    parser.add_argument('--print-log', action='store_true',
                        help='Print the log.')
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG,
                            format=('%(asctime)s %(levelname)s:%(name)s:'
                                    '%(funcName)s:'
                                    '%(filename)s(%(lineno)d):'
                                    '%(threadName)s(%(thread)d):%(message)s'))

    deproxy_port = args.deproxy_port
    repose_port = args.repose_port
    stop_port = args.stop_port

    test_runner = xmlrunner.XMLTestRunner(output='test-reports')

    try:
        setUpModule()
        unittest.main(argv=[''], testRunner=test_runner)
    finally:
        tearDownModule()

if __name__ == '__main__':
    run()
