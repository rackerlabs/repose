#!/usr/bin/env python

"""
In order to determine if a client is authorized to make a prticular call to the
origin serivce, the Client Authorization Filter makes a number of calls to an
Identity service. If the Identity service returns an unexpected error, then
Repose should return a 500 error to the client, indicating that it cannot
process the original request due to a server-side error. The exception is if
the Identity service returns a 404 error in response to a client token
validation request from Repose, which is a correct and normal operation, in
which case Repose will return a 400-level client response to the client.

Identity Response       400   401   402   403   404   405   413   429   500   501   502   503
-----------------       ---   ---   ---   ---   ---   ---   ---   ---   ---   ---   ---   ---
Get Admin Token         500   500   500   500   500   500   500   500   500   500   500   500
Validate Client Token   500   500   500   500   401   500   500   500   500   500   500   500
Get Groups              500   500   500   500   500   500   500   500   500   500   500   500
Get Endpoints           500   500   500   500   500   500   500   500   500   500   500   500

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


def make_response(code, message=None, headers=None, body=None):
    """ Kludge to get around a bug in deproxy caused by a typo. """
    if message is None:
        if code in deproxy.messages_by_response_code:
            message = deproxy.messages_by_response_code[code]
        elif int(code) in deproxy.messages_by_response_code:
            message = deproxy.messages_by_response_code[int(code)]
        else:
            message = ''
    return deproxy.Response(code, message, headers, body)


class FakeIdentityService(object):
    def __init__(self, port, origin_service_port):
        self.port = port
        self.origin_service_port = origin_service_port
        self.error_code = 500
        self.error_message = None
        self.get_admin_token_broken = False
        self.get_groups_broken = False
        self.get_endpoints_broken = False
        self.validate_client_token_broken = False

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

        message = None

        if request.method == 'POST':
            # get admin token
            if self.get_admin_token_broken:
                return make_response(self.error_code, self.error_message)

            params = {
                'expires': t.strftime('%Y-%m-%dT%H:%M:%S%z'),
                'userid': admin_userid,
                'username': admin_username,
                'tenant': admin_tenant,
                'token': admin_token
            }
            code = 200
            if xml:
                template = self.identity_success_xml_template
            else:
                template = self.identity_success_json_template
            pass
        elif request.method == 'GET' and 'tokens' not in request.path:
            if self.get_groups_broken:
                return make_response(self.error_code, self.error_message)

            # getting groups
            if xml:
                template = self.groups_xml_template
            else:
                template = self.groups_json_template
            params = {}
            code = 200
        elif request.method == 'GET' and 'endpoints' in request.path:
            if self.get_endpoints_broken:
                return make_response(self.error_code, self.error_message)

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
        else:
            if self.validate_client_token_broken:
                return make_response(self.error_code, self.error_message)

            # validating a client token
            params = {
                'expires': t.strftime('%Y-%m-%dT%H:%M:%S%z'),
                'userid': client_userid,
                'username': client_username,
                'tenant': client_tenant,
                'token': client_token,
            }

            code = 200
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
        return make_response(code=code, message=message, headers=headers,
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
    def setUp(self):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()
        identity_port = get_next_open_port()
        deproxy_port = get_next_open_port()

        logger.info('repose port: {0}'.format(repose_port))
        logger.info('stop port: {0}'.format(stop_port))
        logger.info('identity port: {0}'.format(identity_port))
        logger.info('origin port: {0}'.format(deproxy_port))

        self.deproxy = deproxy.Deproxy()

        self.origin_endpoint = (
            self.deproxy.add_endpoint(deproxy_port, 'origin service',
                                       default_handler=always(606)))

        self.identity = FakeIdentityService(port=identity_port,
                                            origin_service_port=deproxy_port)
        self.identity_endpoint = (
            self.deproxy.add_endpoint(identity_port, 'identity service',
                                       default_handler=self.identity.handler))

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
            'identity_port': identity_port,
        }

        self.url = 'http://localhost:{0}/resource'.format(repose_port)

        # configure Repose
        conf.process_folder_contents(folder='configs/common',
                                     dest_path='etc/repose', params=params)
        conf.process_folder_contents(folder='configs/auth-z',
                                     dest_path='etc/repose', params=params)

        # start Valve
        self.valve = valve.Valve(config_dir='etc/repose', stop_port=stop_port,
                                  wait_on_start=True, port=repose_port,
                                  insecure=True)

    def test_200(self):
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        #pprint(todict(mc), width=200)
        self.assertEqual(mc.received_response.code, '606')

    def test_400_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)

    def test_400_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_400_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_400_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 400
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_401_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 401
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_401_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 401
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_401_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 401
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_401_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 401
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_402_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 402
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_402_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 402
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_402_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 402
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_402_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 402
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_403_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 403
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_403_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 403
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_403_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 403
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_403_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 403
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_404_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_404_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_404_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_404_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 404
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(401, mc.received_response.code)
    
    def test_405_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_405_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_405_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_405_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 405
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_413_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 413
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_413_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 413
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_413_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 413
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_413_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 413
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_429_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 429
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_429_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 429
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_429_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 429
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_429_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 429
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_500_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_500_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_500_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_500_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 500
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_501_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_501_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_501_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_501_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 501
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_502_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_502_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_502_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_502_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 502
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_503_on_get_admin_token(self):
        self.identity.get_admin_token_broken = True
        self.identity.error_code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_503_on_get_groups(self):
        self.identity.get_groups_broken = True
        self.identity.error_code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_503_on_get_endpoints(self):
        self.identity.get_endpoints_broken = True
        self.identity.error_code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)
    
    def test_503_on_validate_client_token(self):
        self.identity.validate_client_token_broken = True
        self.identity.error_code = 503
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(500, mc.received_response.code)


    def tearDown(self):
        if self.valve is not None:
            self.valve.stop()
        if self.deproxy is not None:
            self.deproxy.shutdown_all_endpoints()


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
