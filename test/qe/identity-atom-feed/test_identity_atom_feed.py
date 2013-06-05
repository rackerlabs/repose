#!/usr/bin/env python

"""

B-48277
Use the Identity Atom Feed to Clear Deleted, Disabled, and Revoked Tokens from
    Cache
https://www15.v1host.com/RACKSPCE/story.mvc/Summary?oidToken=Story%3A639030

Test Plan

This test will allow us to ensure that Repose is properly responding to events
in the identity feed. It will not require creation of test accounts, or even
interaction with the staging or prod identity system. It is entirely
self-contained, and uses Deproxy as fake service endpoint to return
pre-determined responses to Repose.

Still neeed to sort out:
    how to configure the atom-reading part (e.g. tell it the url of the feed)
    The exact format of the responses that the fake services will return
        fake identity - token is good, token is bad
        fake atom - empty feed, feed with one entry, maybe empty feed again

This test involves 5 objects:
    Client
    Repose
    Origin service
    Identity service
    Atom (Hopper) service

Deproxy will take the place of the client and all three services

configure repose (in no particular order):
    auth against the identity service
    look for identity updates from the atom service
    forward requests to origin service

create endpoints
    default responses:
    fake identity service returns positive response to auth requests
    fake atom service returns empty feed, with appropriate next and/or previous
        links
    origin service returns 200

procedure:

request #1 - send a request to repose with some token for a fake user
    fake identity will say token is good
    repose will cache the token
    repose will forward the request to the origin service
    assert fake identity received one request (probably orphaned, that is,
        without a Deproxy-Request-Id header)
    assert origin service received one request

request #2 - send a request with the same token as above
    repose will find the token in the cache, and thus not talk to identity
    repose will forward the request to origin service
    assert fake identity received zero requests
    assert origin service received one request

tell fake atom feed to start showing a single atom entry that invalidates the
    token

repose will, at some point, read the updated feed and should invalidate the
    token and remove it from the cache
upon receiving the request for the feed from repose and returning the
    single entry, fake atom service should switch itself to displaying an empty
    feed

request #3 - send a request with the same token
    repose won't have it in the cache, so it will ask fake identity
    fake identity will return a negative response (404, I think)
    repose will return an error (401, I think) and not forward the request to
        the origin service
    assert fake identity received one request
    assert origin service received zero requests
    assert response code from repose is 401


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
import string
import datetime
from pprint import pprint
import re

logger = logging.getLogger(__name__)

with open('identity-success.xml', 'r') as f:
    identity_success_xml_template = string.Template(f.read())
with open('identity-success.json', 'r') as f:
    identity_success_json_template = string.Template(f.read())
with open('identity-failure.xml', 'r') as f:
    identity_failure_xml_template = string.Template(f.read())
with open('identity-failure.xml', 'r') as f:
    identity_failure_json_template = string.Template(f.read())

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
    if not os.path.exists('usr/share/repose/filter-bundle.ear'):
        rmc = ReposeMavenConnector()
        logger.debug('Downloading filter bundle')
        rmc.get_repose(filter_dest='usr/share/repose/filter-bundle.ear',
                       get_valve=False, get_ext_filter=False,
                       snapshot=snapshot)


class FakeIdentityService(object):
    def __init__(self):
        self.ok = True
        self.validations = 0

    def handler(self, request):
        xml = False
        if 'Accept' in request.headers:
            for value in request.headers.find_all('Accept'):
                if 'application/xml' in value:
                    xml = True
                    break

        t = datetime.datetime.now() + datetime.timedelta(days=1)
        params = {
            'expires': ('%04d-%02d-%02dT%02d:%02d:%02d.000-05:00' %
                        (t.year, t.month, t.day, t.hour, t.minute, t.second)),
            'userid': 123456,
            'username': 'username',
            'tenant': 'this-is-the-tenant',
        }

        if request.method == 'GET':
            # validating a token
            if '/' in request.path:
                index = request.path.rfind('/')
                token = request.path[index+1:]
            else:
                token = request.path

            params['token'] = token

            if request.path.startswith('/tokens'):
                self.validations += 1

            if self.ok:
                code = 200
                message = 'OK'
                if xml:
                    template = identity_success_xml_template
                else:
                    template = identity_success_json_template
            else:
                code = 404
                message = 'Not Found'
                if xml:
                    template = identity_failure_xml_template
                else:
                    template = identity_failure_json_template
        elif request.method == 'POST':
            # get admin token
            params['token'] = 'this-is-the-token'
            code = 200
            message = 'OK'
            if xml:
                template = identity_success_xml_template
            else:
                template = identity_success_json_template
            pass
        else:
            raise 'Unknown request: %r' % request

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


class TestIdentityFeedCacheClearing(unittest.TestCase):
    def setUp(self):
        logger.debug('setting up')

        repose_port = get_next_open_port()
        stop_port = get_next_open_port()
        identity_port = get_next_open_port()
        atom_port = get_next_open_port()
        deproxy_port = get_next_open_port()

        self.identity_service = FakeIdentityService()
        self.deproxy = deproxy.Deproxy()
        self.origin_endpoint = self.deproxy.add_endpoint(deproxy_port,
                                                         'origin service')
        self.identity_endpoint = self.deproxy.add_endpoint(identity_port,
                                                           'identity service',
                                                           default_handler=self.identity_service.handler)
        self.atom_endpoint = self.deproxy.add_endpoint(atom_port,
                                                       'atom service')

        params = {
            'target_hostname': 'localhost',
            'target_port': deproxy_port,
            'port': repose_port,
            'repose_port': repose_port,
            'identity_port': identity_port,
            'atom_port': atom_port
        }

        self.url = 'http://localhost:{0}/resource'.format(repose_port)

        # configure Repose
        conf.process_folder_contents(folder='configs', dest_path='etc/repose',
                                     params=params)

        # start Valve
        self.valve = valve.Valve(config_dir='etc/repose', stop_port=stop_port,
                                 wait_on_start=True, port=repose_port)

        self.token = 'tokentokentoken'

    def test_something(self):

        # request 1 - Repose should validate the token and then pass the
        #   request to the origin service
        self.identity_service.validations = 0
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': self.token})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)
        #self.assertEqual(len(mc.orphaned_handlings), 1)
        # Repose is getting an admin token and groups, so the number of
        # orphaned handlings doesn't necessarily equal the number of times a
        # token gets validated
        self.assertEqual(self.identity_service.validations, 1)
        self.assertEqual(mc.handlings[0].endpoint, self.origin_endpoint)

        # request 2 - Repose should use the cache, and therefore not call out
        #   to the fake identity service, and pass the request to the origin
        #   service
        self.identity_service.validations = 0
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': self.token})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)
        self.assertEqual(self.identity_service.validations, 0)
        self.assertEqual(mc.handlings[0].endpoint, self.origin_endpoint)

        # change identity atom feed

        # request 3 - Repose should not have the token in the cache any more,
        #   so it try to validate it, which will fail. Repose should then
        #   return a 401.

        pass

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
