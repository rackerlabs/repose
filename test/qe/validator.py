#!/usr/bin/env python

import requests
import re

def check_responses(host, path, roles_and_responses, responses=None, protocol='http', port='8080', print_bad_responses=False):
    url = '%s://%s:%i/%s' % (protocol, host, port, path)

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

        if not is_correct and print_bad_responses:
            print resp.content

        results.append(is_correct)
        if responses != None:
            responses.append(resp)

    return results

