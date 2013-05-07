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


#this 

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


class TestSspnn(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/sspnn')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_s1(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_s2(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_n1(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-4'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_n2(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-5'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

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
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_fail_second_of_two(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3,role-2'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
        logger.debug('repose stopped')


class TestP(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/p')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
        logger.debug('repose stopped')


class TestF(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/f')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
        logger.debug('repose stopped')


class TestMssfsffpnn(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/mssfsffpnn')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_s1(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_s2(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_f1(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_s3(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-4'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_f2(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-5'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_f3(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-6'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-7'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_n1(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-8'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_n2(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-9'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    # the following test_* methods check how it responds to multiple roles in
    # different orders

    def test_multi_fffp(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3,role-5,role-6,role-7'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_multi_fff(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3,role-5,role-6'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_multi_pn(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-7,role-8'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_multi_order(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-7,role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
        logger.debug('repose stopped')


class TestMp(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/mp')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
        logger.debug('repose stopped')


class TestMf(unittest.TestCase):
    def setUp(self):
        logger.debug('')

        # set the common config files, like system model and container
        apply_configs(folder='configs/common')

        # set the validator and wadl file for this specific pattern
        apply_configs(folder='configs/mf')

        self.repose = start_repose()

    def test_unlisted_role(self):
        # role-0 is not mentioned in the validator.cfg.xlm file
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def test_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertIn(mc.received_response.code, ['403', '404', '405' ])
        self.assertEqual(len(mc.handlings), 0)

    def tearDown(self):
        logger.debug('stopping repose')
        self.repose.stop()
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




