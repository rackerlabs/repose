#!/usr/bin/env python

"""


qvalue tests
------------

Description:
    "Take into account qvalue. So don't match against all X-Roles values but
    only those with highest qvalue. If the highest qvalue is 0.9, and two
    values have that qvalue, use those two to compare against @role, and no
    others."

Notation:
    When a role designation is followed by a 'q' and a number, that indicates
    the qvalue that the associated header value will have when sent to Repose.
    For example, '1q0.5' translates into 'X-Roles: role-1; q=0.5'. If no 'q'
    and no number are given, then no 'q=' will be added to the header, and the
    qvalue will assume the HTTP default of 1


We want to ensure that the filter is using the role(s) with highest qvalue, and
discarding all others. To do this, we define a few validator configs and send
requests with roles of various qvalues:

    f4f5p\1q0.1,3q0.9 -> p
    mf4p\1q0.9,2q0.1 -> f4

If qvalues were ignored, then the first test would result in an f4, and the
second would result in a pass.

We also want to check that the filter is picking all roles that share the
highest qvalue, instead of just the first one it finds.

    f4f5p\3q0.9,2q0.1,1q0.9 -> f4

Both role-1 and role-3 will have q=0.9, and role-2 should be discarded. If the
filter just uses the first role-3 value, then it will result in a pass. If it
uses both role-1 and role-3, then it will result in an f4.

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


class TestSingleMatchQvalue(unittest.TestCase):
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
        conf.process_folder_contents(folder='configs/f4f5p',
                                     dest_path='etc/repose', params=params)

        cls.repose = repose.ReposeValve(config_dir='etc/repose',
                                        stop_port=stop_port,
                                        wait_on_start=True, port=repose_port)

    def test_single_match_qvalue(self):
        r""" f4f5p\1q0.1,3q0.9 -> p """
        headers = {'X-Roles': 'role-1; q=0.1, role-3; q=0.9'}
        mc = deproxy_object.make_request(url=self.url, headers=headers)
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMultiMatchQvalue(unittest.TestCase):
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
        conf.process_folder_contents(folder='configs/mf4p',
                                     dest_path='etc/repose', params=params)

        cls.repose = repose.ReposeValve(config_dir='etc/repose',
                                        stop_port=stop_port,
                                        wait_on_start=True, port=repose_port)

    def test_multi_match_qvalue(self):
        r""" mf4p\1q0.9,2q0.1 -> f4 """
        headers = {'X-Roles': 'role-1; q=0.9, role-2; q=0.1'}
        mc = deproxy_object.make_request(url=self.url, headers=headers)
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestUseAllRolesWithSameHighQValue(unittest.TestCase):
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
        conf.process_folder_contents(folder='configs/f4f5p',
                                     dest_path='etc/repose', params=params)

        cls.repose = repose.ReposeValve(config_dir='etc/repose',
                                        stop_port=stop_port,
                                        wait_on_start=True, port=repose_port)

    def test_use_all_roles_with_the_same_high_qvalue(self):
        r"""
        f4f5p\3q0.9,2q0.1,1q0.9 -> f4

        If more than one role has the highest qvalue, use all of them.
        """
        headers = {'X-Roles': 'role-3; q=0.9, role-2; q=0.1, role-1; q=0.9'}
        mc = deproxy_object.make_request(url=self.url, headers=headers)
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

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
