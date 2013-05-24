#!/usr/bin/env python

"""
This is a test of the API Validator component, it's multimatch feature in
particular.

The treatment of each configured validator by the filter can be broken down
into the following hierarchy:

 -> Not considered (N)
 -> Considered (C)      -> Skipped (S)
 -> Considered (C)      -> Tried (T)    -> Passed (P)
 -> Considered (C)      -> Tried (T)    -> Failed (F, F4, or F5)

There are two kinds of failures:
    if the request is for a resource that isn't present in the wadl, a 404 will
      be returned (F4)
    if the request uses a method that is not allowed for the specified resouce,
      a 405 will be returned (F5)

If none of the roles match, then the response code will be 403. As a result,
this is denoted as 'F3', although no validator can be configured to return a
403.

If @multi-role-match is false and none of the roles match (all S), then the
default validator will be tried afterwards.
If @multi-role-match is true, then the default will be tried before any other
validators.

We define some notation:

Notation for validator configuration
    Sequence of symbols representing the validators in order, and what they
    would result in if tried. If multi-match is enabled, then the sequence is
    preceded by 'M'

    F4F4PF5F5
    MF4PF5P

Notation for test
    Validator configuration notation, followed by '\' and a number (or numbers)
    indicating which validators will be tried, followed by '->' and the
    expected result. Expected result is one of (P, F3, F4, F5).

    F4F4PF5F5\3 -> P
    P\0 -> F3

Notation for test with default
    Same as above, except it begins with a validator configuration with
    parentheses '(' and ')' around the symbol for the default validator. This
    is followed by an equals sign '=' and the equivalent test if we hadn't been
    using any default.

    F4(F5)\1 = F4F5F5\1,3 -> F4
    MF5(P)F4\3 = MPF5PF4\1,4 -> P

Notation for effective pattern
    A sequence of 'P', 'F', 'S', or 'N', each indicating how the filter should
    treat the validator in that position. If multi-match, preceded by 'M'.

    SSPNN
    MF4SSP

The test cases below are intended to cover all of the required behaviors.
Obviously, we can't comprehensively test the set of all possible configurations
of the filter, so we select a few which cover the required functionality. We
model the treatment of validators (see effective pattern above) as a state
machine. We then list the transitions between states that align with the
desired behavior. Here, 'O' represents the start of the list and 'X' the end.

Single-match
------------

    State transition table

          | P F S N X
        -------------
        O | Y Y Y N ?
        P | N N N Y Y
        F | N N N Y Y
        S | Y Y Y N Y
        N | N N N Y Y

    (The '?' denotes the case where the start is immediately followed by the
    end of the list. That is, no validators are defined in the configuration.
    The functional specification does not cover this case, so we do not test
    it.)

    From this, we determine that the valid transitions are:

        OP, OF, OS,
        PN, PX,
        FN, FX,
        SP, SF, SS, SX,
        NN, NX

    The following sequences cover all of the above transitions:

        SSPNN   OS, SS, SP, PN, NN, NX
        P       OP, PX
        F       OF, FX
        S       OS, SX
        SFN     OS, SF, FN, NX


Multi-match
-----------

    State transition table

          | P F S N X
        -------------
        O | Y Y Y N ?
        P | N N N Y Y
        F | Y Y Y N Y
        S | Y Y Y N Y
        N | N N N Y Y

    Valid transitions:

        OP, OF, OS,
        PN, PX,
        FP, FF, FS, FX,
        SP, SF, SS, SX,
        NN, NX

    Covering sequences:

        MSSFSFFPNN  OS, SS, SF, FS, FF, FP, PN, NN, NX
        MP          OP, PX
        MF          OF, FX
        MS          OS, SX
        MSP         OS, SP, PX


Test Cases
----------

    config'd pattern            effective pattern       exp. result

single-match:
    F4F4PF5F5\3                 SSPNN                   P
    P\0                         S                       F3
    P\1                         P                       P
    F4\1                        F4                      F4
    PF4F5\2                     SF4N                    F4

single-match with default:
    F4(F5)\1 = F4F5F5\1,3       F4NN                    F4
    F4(F5)\0 = F4F5F5\3         SSF5                    F5

multi-match:
    MF4F4F5F4F5F5PF4F4\3,5,6,7  MSSF5SF5F5PNN           P
    MF4F4F5F4F5F5PF4F4\3,5,6    MSSF5SF5F5SSS           F5
    MP\0                        MS                      F3
    MP\1                        MP                      P
    MF4\1                       MF4                     F4
    MF4P\2                      MSP                     P

multi-match with default:
    MF5(P)F4\3 = MPF5PF4\1,4    MPNNN                   P
    MF5(F4)P\3 = MF4F5F4P\1,4   MF4SSP                  P
    MP(F4)F5\3 = MF4PF4F5\1,4   MF4SSF5                 F5
    MP(F4)P\0 = MF4PF4P\1       MF4SSS                  F4



Future, outside-the-box considerations
--------------------------------------
  roles
    are leading and trailing spaces trimmed?
    can tabs work as well as spaces?
    are leading and trailing tabs trimmed?
    what about other unicode whitespace?
  qvalue
    make sure not specifying q actually translates to the default of 1
    what happens if q is < 0 or > 1?
    what happens if q is not a number?

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
        cls.repose = configure_and_start_repose(folder='configs/f4f4pf5f5')

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
        cls.repose = configure_and_start_repose(folder='configs/f4')

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
        cls.repose = configure_and_start_repose(folder='configs/pf4f5')

    def test_sfn(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestSingleMatchDefaults(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/s-default')

    def test_normal(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    def test_activate_default(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
        self.assertEqual(mc.received_response.code, '405')
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
        cls.repose = configure_and_start_repose(folder='configs/'
                                                'mf4f4f5f4f5f5pf4f4')

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
        cls.repose = configure_and_start_repose(folder='configs/mf4')

    def test_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-1'})
        self.assertEqual(mc.received_response.code, '404')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMsp(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/mf4p')

    def test_msp(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-2'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMultimatchMatchDefaults1(unittest.TestCase):
    """This TestCase checks that the default runs after skips and failures."""
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/m-default-1')

    def test_ssf_default_p(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMultimatchMatchDefaults2(unittest.TestCase):
    """This TestCase checks that the default doesn't overwrite a pass."""
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/m-default-2')

    def test_ssp_default_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertEqual(mc.received_response.code, '200')
        self.assertEqual(len(mc.handlings), 1)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMultimatchMatchDefaults3(unittest.TestCase):
    """This TestCase checks that the default is tried before anything else."""
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/m-default-3')

    def test_ssf_default_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-3'})
        self.assertEqual(mc.received_response.code, '405')
        self.assertEqual(len(mc.handlings), 0)

    @classmethod
    def tearDownClass(cls):
        logger.debug('stopping repose')
        cls.repose.stop()
        logger.debug('repose stopped')


class TestMultimatchMatchDefaults4(unittest.TestCase):
    """This TestCase checks that the default runs if none of the roles
    matched.
    """
    @classmethod
    def setUpClass(cls):
        logger.debug('')
        cls.repose = configure_and_start_repose(folder='configs/m-default-4')

    def test_sss_default_f(self):
        mc = d.make_request(url=url, headers={'X-Roles': 'role-0'})
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
