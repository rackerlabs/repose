#!/usr/bin/env python

import argparse
import multimatch
import itertools


def count_true(*iterables):
    c = 0
    for x in itertools.ifilter(None, itertools.chain(*iterables)):
        c += 1
    return c


def count_false(*iterables):
    c = 0
    for x in itertools.ifilterfalse(None, itertools.chain(*iterables)):
        c += 1
    return c

parser = argparse.ArgumentParser()
parser.add_argument(metavar='target-addr', dest='target_addr',
                    help='Hostname or IP address of the target Repose node')
parser.add_argument(metavar='target-port', dest='target_port',
                    help='Port of the target Repose node', type=int,
                    default=8080, nargs='?')
parser.add_argument('protocol',
                    help='Protocol to use to connect to the Repose node',
                    choices=['http', 'https'], default='http', nargs='?')
parser.add_argument('--print-bad-response',
                    help='Print out the response if it fails.',
                    action='store_true')
parser.add_argument('--test', help='Select the test case to run',
                    choices=['all', 'sspnn', 'f', 'p', 'mssfsffpnn', 'mf',
                             'mp'],
                    default='all')

args = parser.parse_args()

protocol = args.protocol
host = args.target_addr
port = args.target_port
pbr = args.print_bad_response

test = args.test

res = []

if test == 'all' or test == 'sspnn':
    res.append(multimatch.check_sspnn(protocol, host, port, pbr))

if test == 'all' or test == 'p':
    res.append(multimatch.check_p(protocol, host, port, pbr))

if test == 'all' or test == 'f':
    res.append(multimatch.check_f(protocol, host, port, pbr))

if test == 'all' or test == 'mssfsffpnn':
    res.append(multimatch.check_mssfsffpnn(protocol, host, port, pbr))

if test == 'all' or test == 'mp':
    res.append(multimatch.check_mp(protocol, host, port, pbr))

if test == 'all' or test == 'mf':
    res.append(multimatch.check_mf(protocol, host, port, pbr))

total_correct = count_true(*res)
total_incorrect = count_false(*res)

print '%i correct' % total_correct
print '%i incorrect' % total_incorrect
