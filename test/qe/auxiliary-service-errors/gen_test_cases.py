
import string

template = string.Template("""    def test_${code}_on_${call}(self):
        self.identity.${call}_broken = True
        self.identity.error_code = ${code}
        mc = self.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': client_token})
        self.assertEqual(${response}, mc.received_response.code)
""")

codes = [400, 401, 402, 403, 404, 405, 413, 429, 500, 501, 502, 503]
calls = [
    'get_admin_token',
    'get_groups',
    'get_endpoints',
    'validate_client_token',
]

special = {
    (404, 'validate_client_token'): 401,
}

for code in codes:
    for call in calls:
        response = 500
        if (code, call) in special:
            response = special[(code, call)]
        print template.safe_substitute({'code': code,
                                        'call': call,
                                        'response': response})
