#!/usr/bin/env python

"""


"""

import unittest2 as unittest
import xmlrunner
from narwhal import valve
from narwhal import conf
from narwhal import pathutil
from narwhal.download_repose import ReposeMavenConnector
import deproxy
import requests
import logging
import argparse
import sys
import os
import glob
import string
import datetime
from pprint import pprint
import re
import time

logger = logging.getLogger(__name__)

client_token = 'this-is-the-token'
client_tenant = 'this-is-the-tenant'
client_username = 'username'
client_userid = 12345
admin_token = 'this-is-the-admin-token'
admin_tenant = 'this-is-the-admin-tenant'
admin_username = 'admin_username'
admin_userid = 67890

port_base = 11000
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


def always(code):
    def handler(request):
        if int(code) in deproxy.messages_by_response_code:
            message = deproxy.messages_by_response_code[int(code)]
        else:
            message = 'Something'
        logger.debug('Returning {0}'.format(code))
        return deproxy.Response(code=code, message=message)
    handler.__doc__ = 'Always return a {0} status code.'.format(code)
    return handler


class FakeIdentityService(object):
    def __init__(self, port, origin_service_port, code=200):
        self.port = port
        self.origin_service_port = origin_service_port
        self.code = code

        with open('identity-success.xml', 'r') as f:
            self.identity_success_xml_template = string.Template(f.read())
        with open('identity-success.json', 'r') as f:
            self.identity_success_json_template = string.Template(f.read())
        with open('identity-failure.xml', 'r') as f:
            self.identity_failure_xml_template = string.Template(f.read())
        with open('identity-failure.xml', 'r') as f:
            self.identity_failure_json_template = string.Template(f.read())
        with open('identity-endpoints.json', 'r') as f:
            self.identity_endpoints_json_template = string.Template(f.read())
        with open('identity-endpoints.xml', 'r') as f:
            self.identity_endpoints_xml_template = string.Template(f.read())

        self.groups_json_template = string.Template('''{
            "RAX-KSGRP:groups": [
                {
                    "id": "0",
                    "description": "Default Limits",
                    "name": "Default"
                }
            ]
        }''')

        self.groups_xml_template = string.Template(
            '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <groups xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0">
            <group id="0" name="Default">
                <description>Default Limits</description>
            </group>
        </groups>''')

    def handler(self, request):
        logger.debug('Handling a request')
        xml = False
        if 'Accept' in request.headers:
            for value in request.headers.find_all('Accept'):
                if 'application/xml' in value:
                    xml = True
                    break

        t = datetime.datetime.now() + datetime.timedelta(days=1)

        if request.method == 'POST':
            # get admin token
            params = {
                'expires': t.strftime('%Y-%m-%dT%H:%M:%S%z'),
                'userid': admin_userid,
                'username': admin_username,
                'tenant': admin_tenant,
                'token': admin_token
            }
            code = 200
            message = 'OK'
            if xml:
                template = self.identity_success_xml_template
            else:
                template = self.identity_success_json_template
            pass
        elif request.method == 'GET' and 'tokens' not in request.path:
            # getting groups
            if xml:
                template = self.groups_xml_template
            else:
                template = self.groups_json_template
            params = {}
            code = 200
            message = 'OK'
        elif request.method == 'GET' and 'endpoints' in request.path:
            if xml:
                template = self.identity_endpoints_xml_template
            else:
                template = self.identity_endpoints_json_template
            params = {
                'identity_port': self.port,
                'token': client_token,
                'expires': t.strftime('%Y-%m-%dT%H:%M:%S%z'),
                'userid': client_userid,
                'username': client_username,
                'tenant': client_tenant,
                'token': client_token,
                'origin_service_port': self.origin_service_port,
            }
            code = 200
            message = 'OK'
        else:
            # validating a client token
            params = {
                'expires': t.strftime('%Y-%m-%dT%H:%M:%S%z'),
                'userid': client_userid,
                'username': client_username,
                'tenant': client_tenant,
                'token': client_token,
            }

            code = int(self.code)
            message = deproxy.messages_by_response_code[code]
            if code == 200:
                if xml:
                    template = self.identity_success_xml_template
                else:
                    template = self.identity_success_json_template
            else:
                if xml:
                    template = self.identity_failure_xml_template
                else:
                    template = self.identity_failure_json_template

        #print '%s %s -> %s' % (request.method, request.path, code)

        body = template.safe_substitute(params)
        headers = {
            'Connection': 'close',
        }
        if xml:
            headers['Content-type'] = 'application/xml'
        else:
            headers['Content-type'] = 'application/json'
        return deproxy.Response(code=code, message=message, headers=headers,
                                body=body)


def setUpModule():
    logger.debug('setting up')

    # Set up folder hierarchy
    pathutil.create_folder('etc/repose')
    pathutil.create_folder('var/repose')
    pathutil.create_folder('var/log/repose')

    # Download Repose artifacts if needed
    snapshot = True
    if ('IS_RELEASE_BUILD' in os.environ and
            os.environ['IS_RELEASE_BUILD']):
        snapshot = False

    if not os.path.exists('usr/share/repose/repose-valve.jar'):
        rmc = ReposeMavenConnector()
        logger.debug('Downloading valve jar')
        rmc.get_repose(valve_dest='usr/share/repose/repose-valve.jar',
                       get_filter=False, get_ext_filter=False,
                       snapshot=snapshot)
    if not glob.glob('usr/share/repose/filter-bundle*.ear'):
        rmc = ReposeMavenConnector()
        logger.debug('Downloading filter bundle')
        rmc.get_repose(filter_dest='usr/share/repose/filter-bundle.ear',
                       get_valve=False, get_ext_filter=False,
                       snapshot=snapshot)


def todict(obj, classkey=None):
    if isinstance(obj, dict):
        for k in obj.keys():
            obj[k] = todict(obj[k], classkey)
        return obj
    elif isinstance(obj, deproxy.HeaderCollection):
        return [(todict(k, classkey), todict(v, classkey))
                    for k,v in obj.iteritems()]
    elif isinstance(obj, tuple):
        return tuple([todict(v, classkey) for v in obj])
    elif hasattr(obj, "__iter__"):
        return [todict(v, classkey) for v in obj]
    elif hasattr(obj, "__dict__"):
        data = dict([(key, todict(value, classkey))
            for key, value in obj.__dict__.iteritems()
            if not callable(value) and not key.startswith('_')])
        if classkey is not None and hasattr(obj, "__class__"):
            data[classkey] = obj.__class__.__name__
        return data
    else:
        return obj


class TestAuthorizationServiceErrors(unittest.TestCase):
    @classmethod
    def setUpClass(klass):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()
        identity_port = get_next_open_port()
        deproxy_port = get_next_open_port()

        logger.info('repose port: {0}'.format(repose_port))
        logger.info('stop port: {0}'.format(stop_port))
        logger.info('identity port: {0}'.format(identity_port))
        logger.info('origin port: {0}'.format(deproxy_port))

        klass.deproxy = deproxy.Deproxy()

        klass.origin_endpoint = (
            klass.deproxy.add_endpoint(deproxy_port, 'origin service',
                                       default_handler=always(606)))

        klass.identity = FakeIdentityService(port=identity_port,
                                            origin_service_port=deproxy_port)
        klass.identity_endpoint = (
            klass.deproxy.add_endpoint(identity_port, 'identity service',
                                       default_handler=klass.identity.handler))

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
            'identity_port': identity_port,
        }

        klass.url = 'http://localhost:{0}/resource'.format(repose_port)

        # configure Repose
        conf.process_folder_contents(folder='configs/common',
                                     dest_path='etc/repose', params=params)
        conf.process_folder_contents(folder='configs/auth-z',
                                     dest_path='etc/repose', params=params)

        # start Valve
        klass.valve = valve.Valve(config_dir='etc/repose', stop_port=stop_port,
                                  wait_on_start=True, port=repose_port,
                                  insecure=True)

    def test_200(self):
        self.identity.code = 200
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        #pprint(todict(mc), width=200)
        self.assertEqual(mc.received_response.code, '606')

    def test_400(self):
        self.identity.code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_404(self):
        self.identity.code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_405(self):
        self.identity.code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_500(self):
        self.identity.code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_501(self):
        self.identity.code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_502(self):
        self.identity.code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_503(self):
        self.identity.code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_504(self):
        self.identity.code = 504
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    @classmethod
    def tearDownClass(klass):
        if klass.valve is not None:
            klass.valve.stop()
        if klass.deproxy is not None:
            klass.deproxy.shutdown_all_endpoints()


class TestAuthenticationServiceErrors(unittest.TestCase):
    @classmethod
    def setUpClass(klass):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()
        identity_port = get_next_open_port()
        deproxy_port = get_next_open_port()

        logger.info('repose port: {0}'.format(repose_port))
        logger.info('stop port: {0}'.format(stop_port))
        logger.info('identity port: {0}'.format(identity_port))
        logger.info('origin port: {0}'.format(deproxy_port))

        klass.deproxy = deproxy.Deproxy()

        klass.origin_endpoint = (
            klass.deproxy.add_endpoint(deproxy_port, 'origin service',
                                       default_handler=always(606)))

        klass.identity = FakeIdentityService(port=identity_port)
        klass.identity_endpoint = (
            klass.deproxy.add_endpoint(identity_port, 'identity service',
                                       default_handler=klass.identity.handler))

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
            'identity_port': identity_port,
        }

        klass.url = 'http://localhost:{0}/resource'.format(repose_port)

        # configure Repose
        conf.process_folder_contents(folder='configs/common',
                                     dest_path='etc/repose', params=params)
        conf.process_folder_contents(folder='configs/auth-n',
                                     dest_path='etc/repose', params=params)

        # start Valve
        klass.valve = valve.Valve(config_dir='etc/repose', stop_port=stop_port,
                                  wait_on_start=True, port=repose_port,
                                  insecure=True)

    def test_200(self):
        self.identity.code = 200
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        logger.debug(repr(mc))
        self.assertEqual(mc.received_response.code, '606')

    def test_400(self):
        self.identity.code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_404(self):
        self.identity.code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_405(self):
        self.identity.code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_500(self):
        self.identity.code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_501(self):
        self.identity.code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_502(self):
        self.identity.code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_503(self):
        self.identity.code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    def test_504(self):
        self.identity.code = 504
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertRegexpMatches(mc.received_response.code, r'5\d\d')

    @classmethod
    def tearDownClass(klass):
        if klass.valve is not None:
            klass.valve.stop()
        if klass.deproxy is not None:
            klass.deproxy.shutdown_all_endpoints()


def tearDownModule():
    logger.debug('tearing down')
    pass


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
