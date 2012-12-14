#!/usr/bin/env python

import requests
import sys
import argparse
import re

parser = argparse.ArgumentParser()
parser.add_argument(metavar='target-addr', dest='target_addr', help='Hostname or IP address of the target Repose node')
parser.add_argument(metavar='target-port', dest='target_port', help='Port of the target Repose node', type=int, default=8080, nargs='?')
parser.add_argument('protocol', help='Protocol to use to connect to the Repose node', choices=['http','https'], default='http', nargs='?')
parser.add_argument('--print-bad-response', help='Print out the response if it fails.', action='store_true')

args = parser.parse_args()


def run_a_test(path, roles_and_responses):
  url = '%s://%s:%i/%s' % (args.protocol, args.target_addr, args.target_port, path)

  correct = 0
  incorrect = 0

  for role, code in sorted(roles_and_responses.items()):

    resp = requests.get(url, headers = { 'X-Roles': role })

    if re.match(str(code), str(resp.status_code)) == None:
      incorrect += 1
      c = 'INCORRECT'
    else:
      correct += 1
      c = 'CORRECT'

    print 'Get %s with role "%s": expected %s, got %i -> %s' % (url, role, code, resp.status_code, c)

    if c == 'INCORRECT' and args.print_bad_response:
      print resp.content

  return correct, incorrect

total_correct = 0
total_incorrect = 0

correct, incorrect = run_a_test('multimatch/sspnn', { 'role-0':403, 'role-1':405, 'role-2':405, 'role-3':200, 'role-4':404, 'role-5':404 })
total_correct += correct
total_incorrect += incorrect

print '%i correct' % total_correct
print '%i incorrect' % total_incorrect

