#!/usr/bin/env python

"""
Multiple Roles Tests
--------------------

Description:

    B-49024
    Repose RBAC Config Schema Changes

    "AIM of story is to have a single validator apply to a group of roles - for
    example: observer & widget:observer will have the same validator and same
    capabilities. (The validator config will be changed to support multiple
    roles within one config, where the roles have the same capabilities for
    RBAC. )"

    The @role attribute will be modified so that it supports a space-separated
    list of roles, instead of just a single role.

Notation:

    When a validator designation (f4, f5, or p) is followed by a pair of
    braces { } which contain a comma-separated list of tokens, the list
    indicates the valid roles for that validator object. For example: 'p{1,2}'
    translates into '<validator @role="role-1 role-2" ...' in the config. When
    a validator designation is not followed by such a list, then it is assumed
    that it uses the normal role in a sequential pattern.

    NOTE: This new notation doesn't fit into typically filesystem namespaces.
    We may have to escape names on the commandline, e.g. "p\{1\,2\}"

There are a number of tests we could use to check this behavior. For example:

    f4{1,2}p{2,3}f5{1,3}\1 -> f4
    f4{1,2}p{2,3}f5{1,3}\2 -> p
    f4{1,2}p{2,3}f5{1,3}\3 -> p
    f4{1,2}p{2,3}f5{1,3}\1,2 -> f4
    f4{1,2}p{2,3}f5{1,3}\1,3 -> f4
    f4{1,2}p{2,3}f5{1,3}\2,3 -> f4
    f4{1,2}p{2,3}f5{1,3}\1,2,3 -> f4

    mf4{1,2}p{2,3}f5{1,3}\1 -> f5
    mf4{1,2}p{2,3}f5{1,3}\2 -> p
    mf4{1,2}p{2,3}f5{1,3}\3 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,2 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,3 -> f5
    mf4{1,2}p{2,3}f5{1,3}\2,3 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,2,3 -> p

However, that would be excessive. To ensure that the validator supports
multiple roles, we need only set up a few simple situtations. We define a
single validator object with two roles.

    p{1,2}\0 -> f3
    p{1,2}\1 -> p
    p{1,2}\2 -> p

Sending a request without any roles should result in a 403, confirming that
it's all working normally, without any default. Sending each role on its own
should result in the validator matching.

We also need to make sure that it's checking each validator in turn against the
list of incoming roles, instead of each incoming role in turn against the whole
list of validators. In pseudocode:

good:
    for validator in validators:
      for role in incoming_roles:
        if role in validator.roles:
          # do something

bad:
    for role in incoming_roles:
      for validator in validators:
        if role in validator.roles:
          # do something

This requires only one request:

    p{2}f4{1,2}\1,2 -> p

If the filter were looping through incoming roles first, then the 2 would match
the f4, when the 1 should match the p.
"""


import unittest2 as unittest
import xmlrunner
from narwhal import repose
from narwhal import conf
from narwhal import pathutil
import deproxy
import requests
import logging
import argparse

logger = logging.getLogger(__name__)

deproxy_object = None
deproxy_port = None
port_base = 9999
port_port = None


def get_next_open_port(start=None):
    global port_port
    if start is not None:
        port_port = start
    elif port_port is None:
        port_port = port_base

    while port_port < 65535:
        try:
            requests.get('http://localhost:%i' % port_port)
        except requests.exceptions.ConnectionError as e:
            port_port += 1
            return port_port - 1
        except:
            pass
        port_port += 1
    raise Exception('Ran out of ports')


def setUpModule():
    logger.debug('setting up')

    # Set up folder hierarchy
    pathutil.create_folder('etc/repose')
    pathutil.create_folder('var/repose')
    pathutil.create_folder('var/log/repose')

    # start a single deproxy for all tests
    global deproxy_port
    global deproxy_object
    deproxy_port = get_next_open_port()
    if deproxy_object is None:
        deproxy_object = deproxy.Deproxy()
        deproxy_object.add_endpoint(('localhost', deproxy_port))


class TestMultipleRoles(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
        }

        cls.url = 'http://localhost:{0}/resource'.format(repose_port)

        # set the common config files, like system model and container
        conf.process_folder_contents(folder='configs/common',
                                     dest_path='etc/repose', params=params)

        # set the specific config files, i.e. validator.cfg.xml
        conf.process_folder_contents(folder='configs/p{1,2}',
                                     dest_path='etc/repose', params=params)

        cls.repose = repose.ReposeValve(config_dir='etc/repose',
                                        stop_port=stop_port,
                                        wait_on_start=True, port=repose_port)

    def test_neither_role(self):
        r""" p{1,2}\0 -> f3 """
        mc = deproxy_object.make_request(url=self.url,
                                         headers={'X-Roles': 'role-0'})
        self.assertEqual(mc.received_response.code, '403')
        self.assertEqual(len(mc.handlings), 0)

    def test_first_role(self):
        r""" p{1,2}\1 -> p """
        mc = deproxy_object.make_request(url=self.url,
                                         headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_second_role(self):
        r""" p{1,2}\2 -> p """
        mc = deproxy_object.make_request(url=self.url,
                                         headers={'X-Roles': 'role-2'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    def test_both_roles(self):
        r""" p{1,2}\1,2 -> p """
        mc = deproxy_object.make_request(url=self.url,
                                         headers={'X-Roles': 'role-1,role-2'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestRoleOrder(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
        }

        cls.url = 'http://localhost:{0}/resource'.format(repose_port)

        # set the common config files, like system model and container
        conf.process_folder_contents(folder='configs/common',
                                     dest_path='etc/repose', params=params)

        # set the specific config files, i.e. validator.cfg.xml
        conf.process_folder_contents(folder='configs/p{2}f4{1,2}',
                                     dest_path='etc/repose', params=params)

        cls.repose = repose.ReposeValve(config_dir='etc/repose',
                                        stop_port=stop_port,
                                        wait_on_start=True, port=repose_port)

    def test_role_order(self):
        r""" p{2}f4{1,2}\1,2 -> p """
        mc = deproxy_object.make_request(url=self.url,
                                         headers={'X-Roles': 'role-1,role-2'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


def tearDownModule():
    logger.debug('tearing down')
    if deproxy_object is not None:
        logger.debug('shutting down deproxy')
        deproxy_object.shutdown_all_endpoints()
        logger.debug('deproxy shut down')


def run():
    global port_base

    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', action='store_true',
                        help='Print the log.')
    parser.add_argument('--port-base', help='The port number to start looking '
                        'for open ports. The default is %i.' % port_base,
                        default=port_base, type=int)
    args = parser.parse_args()

    if args.print_log:
        logging.basicConfig(level=logging.DEBUG,
                            format=('%(asctime)s %(levelname)s:%(name)s:'
                                    '%(funcName)s:'
                                    '%(filename)s(%(lineno)d):'
                                    '%(threadName)s(%(thread)d):%(message)s'))

    port_base = args.port_base

    test_runner = xmlrunner.XMLTestRunner(output='test-reports')

    unittest.main(argv=[''], testRunner=test_runner)

if __name__ == '__main__':
    run()
