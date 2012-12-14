#!/usr/bin/env python

import requests
import sys
import argparse
import re
import itertools

parser = argparse.ArgumentParser()
parser.add_argument(metavar='target-addr', dest='target_addr', help='Hostname or IP address of the target Repose node')
parser.add_argument(metavar='target-port', dest='target_port', help='Port of the target Repose node', type=int, default=8080, nargs='?')
parser.add_argument('protocol', help='Protocol to use to connect to the Repose node', choices=['http','https'], default='http', nargs='?')
parser.add_argument('--print-bad-response', help='Print out the response if it fails.', action='store_true')

args = parser.parse_args()


def run_a_test(path, roles_and_responses, responses=None):
    url = '%s://%s:%i/%s' % (args.protocol, args.target_addr, args.target_port, path)

    correct = 0
    incorrect = 0
    results = []

    for role, code in sorted(roles_and_responses.items()):

        resp = requests.get(url, headers = { 'X-Roles': role })

        if re.match(str(code), str(resp.status_code)) == None:
            incorrect += 1
            c = 'INCORRECT'
            is_correct = False
        else:
            correct += 1
            c = 'CORRECT'
            is_correct = True

        print 'Get %s with role "%s": expected %s, got %i -> %s' % (url, role, code, resp.status_code, c)

        if not is_correct and args.print_bad_response:
            print resp.content

        results.append(is_correct)
        if responses != None:
            responses.append(resp)

    return results

def count_true(*iterables):
    c = 0
    for x in itertools.ifilter(None, itertools.chain(*iterables)): c += 1
    return c

def count_false(*iterables):
    c = 0
    for x in itertools.ifilterfalse(None, itertools.chain(*iterables)): c += 1
    return c

total_correct = 0
total_incorrect = 0

results = run_a_test('multimatch/sspnn', { 'role-0':403,
                                           'role-1':405,
                                           'role-2':405,
                                           'role-3':200,
                                           'role-4':404,
                                           'role-5':404,
                                           'role-2,role-3': 405,
                                           'role-3,role-4': 200 })
total_correct += count_true(results)
total_incorrect += count_false(results)

results = run_a_test('multimatch/p', { 'role-0':403, 'role-1':200 })
total_correct += count_true(results)
total_incorrect += count_false(results)

results = run_a_test('multimatch/f', { 'role-0':403, 'role-1':405 })
total_correct += count_true(results)
total_incorrect += count_false(results)

results = run_a_test('multimatch/mssfsffpnn', { 'role-0':403,
                                                'role-1':405,
                                                'role-2':405,
                                                'role-3':405,
                                                'role-4':405,
                                                'role-5':405,
                                                'role-6':405,
                                                'role-7':200,
                                                'role-8':404,
                                                'role-9':404,
                                                'role-3,role-5,role-6,role-7':200,
                                                'role-3,role-5,role-6':405,
                                                'role-7,role-8':200 })
total_correct += count_true(results)
total_incorrect += count_false(results)

results = run_a_test('multimatch/mp', { 'role-0':403, 'role-1':200 })
total_correct += count_true(results)
total_incorrect += count_false(results)

results = run_a_test('multimatch/mf', { 'role-0':403, 'role-1':405 })
total_correct += count_true(results)
total_incorrect += count_false(results)

print '%i correct' % total_correct
print '%i incorrect' % total_incorrect

