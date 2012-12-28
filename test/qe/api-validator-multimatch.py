#!/usr/bin/env python

import argparse
import validator

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


def check_sspnn(protocol, host, port, path, pbr):
    roles_and_responses = {
        'role-0': 403,
        'role-1': 405,
        'role-2': 405,
        'role-3': 200,
        'role-4': 404,
        'role-5': 404,
        'role-2,role-3': 405,
        'role-3,role-4': 200
        }
    return validator.check_responses(host, path,
                                     roles_and_responses,
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)

res.append(check_sspnn(protocol, host, port, 'multimatch/sspnn', pbr))


def check_p(protocol, host, port, path, pbr):
    return validator.check_responses(host, path,
                                     {'role-0': 403, 'role-1': 200},
                                     protocol=protocol, port=port,
                                     print_bad_responses=pbr)

res.append(check_p(protocol, host, port, 'multimatch/p', pbr))

res.append(validator.check_responses(host, 'multimatch/f', { 'role-0':403, 'role-1':405 },
                                                                 protocol=protocol, port=port, print_bad_responses=pbr))

res.append(validator.check_responses(host, 'multimatch/mssfsffpnn', { 'role-0':403,
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
                                                                      'role-7,role-8':200 },
                                                                 protocol=protocol, port=port, print_bad_responses=pbr))

res.append(validator.check_responses(host, 'multimatch/mp', { 'role-0':403, 'role-1':200 },
                                                                 protocol=protocol, port=port, print_bad_responses=pbr))

res.append(validator.check_responses(host, 'multimatch/mf', { 'role-0':403, 'role-1':405 },
                                                                 protocol=protocol, port=port, print_bad_responses=pbr))

total_correct = validator.count_true(*res)
total_incorrect = validator.count_false(*res)

print '%i correct' % total_correct
print '%i incorrect' % total_incorrect

