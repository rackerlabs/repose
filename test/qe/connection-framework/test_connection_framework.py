#!/usr/bin/env python

from narwhal import valve
import requests
import unittest2 as unittest
from narwhal import conf
from narwhal import pathutil
import xmlrunner as _xmlrunner
import logging
import time
import argparse
import os
import deproxy

logger = logging.getLogger(__name__)


target_hostname = '50.57.189.15'
target_port = 8080
config_dir = pathutil.join(os.getcwd(), 'etc/repose')
deploy_dir = pathutil.join(os.getcwd(), 'var/repose')
artifact_dir = pathutil.join(os.getcwd(), 'usr/share/repose/filters')
log_file = pathutil.join(os.getcwd(), 'var/log/repose/current.log')
stop_port = 7777
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
    logger.debug('setUpModule')
    pathutil.create_folder(config_dir)
    pathutil.create_folder(deploy_dir)
    pathutil.create_folder(os.path.dirname(log_file))


def apply_configs(folder, params=None):
    if params is None:
        params = {}
    conf.process_folder_contents(folder=folder, verbose=False,
                                 dest_path=config_dir, params=params)


class TestJersey(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.deproxy_port = get_next_open_port()

        logger.debug('repose port: %s' % self.repose_port)
        logger.debug('stop port: %s' % self.stop_port)
        logger.debug('deproxy port: %s' % self.deproxy_port)

        self.deproxy = deproxy.Deproxy()
        self.deproxy.add_endpoint(('localhost', self.deproxy_port))

        pathutil.clear_folder(config_dir)
        self.params = {
            'proto': 'http',
            'target_hostname': 'localhost',
            'target_port': self.deproxy_port,
            'deployment_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
            'port': self.repose_port,
        }
        apply_configs('configs', params=self.params)

        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=self.stop_port,
                                 port=self.repose_port, wait_on_start=True,
                                 conn_fw='jersey')

    def test_no_accept_header(self):

        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        # Repose will add an accept header in order to get around the bug in
        # jersey, so here we test for empty rather than non-existent
        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'], '')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'], '')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_empty_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': ''}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'], '')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'], '')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_star_star_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': '*/*'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             '*/*')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             '*/*')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_type_star_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': 'text/*'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             'text/*')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             'text/*')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_type_subtype_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': 'text/plain'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             'text/plain')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             'text/plain')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


class TestApache(unittest.TestCase):
    def setUp(self):
        logger.debug('setUp')

        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.deproxy_port = get_next_open_port()

        logger.debug('repose port: %s' % self.repose_port)
        logger.debug('stop port: %s' % self.stop_port)
        logger.debug('deproxy port: %s' % self.deproxy_port)

        self.deproxy = deproxy.Deproxy()
        self.deproxy.add_endpoint(('localhost', self.deproxy_port))

        pathutil.clear_folder(config_dir)
        self.params = {
            'proto': 'http',
            'target_hostname': 'localhost',
            'target_port': self.deproxy_port,
            'deployment_dir': deploy_dir,
            'artifact_dir': artifact_dir,
            'log_file': log_file,
            'port': self.repose_port,
        }
        apply_configs('configs', params=self.params)

        self.valve = valve.Valve(config_dir=config_dir,
                                 stop_port=self.stop_port,
                                 port=self.repose_port, wait_on_start=True,
                                 conn_fw='apache')

    def test_no_accept_header(self):

        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        self.assertNotIn('Accept', mc.handlings[0].request.headers)
        self.assertNotIn('accept', mc.handlings[0].request.headers)

    @unittest.expectedFailure
    def test_empty_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': ''}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'], '')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'], '')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_star_star_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': '*/*'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             '*/*')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             '*/*')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_type_star_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': 'text/*'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             'text/*')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             'text/*')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def test_type_subtype_accept_header(self):
        url = 'http://localhost:{0}/'.format(self.repose_port)
        host_header = 'localhost:{0}'.format(self.repose_port)
        headers = {'Host': host_header, 'User-Agent': deproxy.version_string,
                   'Accept': 'text/plain'}

        logger.debug('making a request')
        mc = self.deproxy.make_request(url=url, headers=headers,
                                       add_default_headers=False)

        if 'Accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['Accept'],
                             'text/plain')
        elif 'accept' in mc.handlings[0].request.headers:
            self.assertEqual(mc.handlings[0].request.headers['accept'],
                             'text/plain')
        else:
            self.assertIn('Accept', mc.handlings[0].request.headers)

    def tearDown(self):
        logger.debug('tearDown')
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


def run():
    global port_base

    parser = argparse.ArgumentParser()
    parser.add_argument('--print-log', help="Print the log to STDERR.",
                        action='store_true')
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

    test_runner = _xmlrunner.XMLTestRunner(output='test-reports')

    unittest.main(argv=[''], testRunner=test_runner)

if __name__ == '__main__':
    run()
