
import string

setup_template = string.Template("""class Test${call2}${code}(unittest.TestCase):
    def setUp(self):
        logger.debug('setting up')

        globalvars.identity.${call}_broken = True
        globalvars.identity.error_code = ${code}

        (self.valve, self.url) = common_setup()
""")

test_template = string.Template("""    def test_${code}_on_${call}(self):
        mc = globalvars.deproxy.make_request(url=self.url,
                                       headers={'X-Auth-Token': globalvars.client_token})
        self.assertEqual('${response}', mc.received_response.code)
""")

teardown_template = string.Template("""    def tearDown(self):
        if self.valve is not None:
            self.valve.stop()

""")

codes = [400, 401, 402, 403, 404, 405, 413, 429, 500, 501, 502, 503]
calls = [
    ('get_admin_token', 'AdminToken'),
    ('get_groups', 'Groups'),
    ('get_endpoints', 'Endpoints'),
    ('validate_client_token', 'ValidateToken'),
]

special = {
    (404, 'validate_client_token'): 401,
}

for call in calls:
    (call1, call2) = call
    for code in codes:
        response = 500
        if (code, call1) in special:
            response = special[(code, call1)]
        params = {
            'code': code,
            'call': call1,
            'call2': call2,
            'response': response,
        }
        print setup_template.safe_substitute(params)
        print test_template.safe_substitute(params)
        print teardown_template.safe_substitute(params)
