#!/usr/bin/env python

import argparse
import multimatch
import itertools

def count_true(*iterables):
    c = 0
    for x in itertools.ifilter(None, itertools.chain(*iterables)): c += 1
    return c

def count_false(*iterables):
    c = 0
    for x in itertools.ifilterfalse(None, itertools.chain(*iterables)): c += 1
    return c

parser = argparse.ArgumentParser()
parser.add_argument(metavar='target-addr', dest='target_addr', help='Hostname or IP address of the target Repose node')
parser.add_argument(metavar='target-port', dest='target_port', help='Port of the target Repose node', type=int, default=8080, nargs='?')
parser.add_argument('protocol', help='Protocol to use to connect to the Repose node', choices=['http','https'], default='http', nargs='?')
parser.add_argument('--print-bad-response', help='Print out the response if it fails.', action='store_true')

args = parser.parse_args()

protocol = args.protocol
host = args.target_addr
port = args.target_port
pbr = args.print_bad_response

res = []

res.append(multimatch.check_sspnn(protocol, host, port, 'multimatch/sspnn', pbr))

res.append(multimatch.check_p(protocol, host, port, 'multimatch/p', pbr))

res.append(multimatch.check_f(protocol, host, port, 'multimatch/f', pbr))

res.append(multimatch.check_mssfsffpnn(protocol, host, port, 'multimatch/mssfsffpnn', pbr))

res.append(multimatch.check_mp(protocol, host, port, 'multimatch/mp', pbr))

res.append(multimatch.check_mf(protocol, host, port, 'multimatch/mf', pbr))

total_correct = count_true(*res)
total_incorrect = count_false(*res)

print '%i correct' % total_correct
print '%i incorrect' % total_incorrect

