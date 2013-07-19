
import re

components_by_config = {
    'system-model.cfg.xml': 'System Model',
    'container.cfg.xml' : 'Container',
    'response-messaging.cfg.xml': 'Response Messaging Service',
    'rate-limiting.cfg.xml': 'Rate Limiting',
    'versioning.cfg.xml': 'Versioning',
    'translation.cfg.xml': 'Translation',
    'client-auth-n.cfg.xml': 'Client Authentication',
    'openstack-authorization.cfg.xml': 'Client Authorization',
    'dist-datastore.cfg.xml': 'Distributed Datastore',
    'http-logging.cfg.xml': 'Http Logging',
    'uri-identity.cfg.xml': 'Uri Identity',
    'header-identity.cfg.xml': 'Header Identity',
    'ip-identity.cfg.xml': 'Ip Identity',
}

cases = """system-model.cfg.xml,Start Good,200
system-model.cfg.xml,Start Bad,Can't connect to Repose
system-model.cfg.xml,Good to Bad,200
system-model.cfg.xml,Bad to Good,200
container.cfg.xml,Start Good,200
container.cfg.xml,Start Bad,Can't Connect to Repose
container.cfg.xml,Good to Bad,200
container.cfg.xml,Bad to Good,200
response-messaging.cfg.xml,Start Good,200
response-messaging.cfg.xml,Start Bad,503
response-messaging.cfg.xml,Good to Bad,200
response-messaging.cfg.xml,Bad to Good,200
rate-limiting.cfg.xml,Start Good,200 
rate-limiting.cfg.xml,Start Bad,503
rate-limiting.cfg.xml,Good to Bad,200
rate-limiting.cfg.xml,Bad to Good,200
versioning.cfg.xml,Start Good,200
versioning.cfg.xml,Start Bad,503
versioning.cfg.xml,Good to Bad,200
versioning.cfg.xml,Bad to Good,200
translation.cfg.xml,Start Good,200
translation.cfg.xml,Start Bad,503
translation.cfg.xml,Good to Bad,200
translation.cfg.xml,Bad to Good,200
client-auth-n.cfg.xml,Start Good,200
client-auth-n.cfg.xml,Start Bad,503
client-auth-n.cfg.xml,Good to Bad,200
client-auth-n.cfg.xml,Bad to Good,200
openstack-authorization.cfg.xml,Start Good,401
openstack-authorization.cfg.xml,Start Bad,503
openstack-authorization.cfg.xml,Good to Bad,401
openstack-authorization.cfg.xml,Bad to Good,401
dist-datastore.cfg.xml,Start Good,200
dist-datastore.cfg.xml,Start Bad,503
dist-datastore.cfg.xml,Good to Bad,200
dist-datastore.cfg.xml,Bad to Good,200
http-logging.cfg.xml,Start Good,200
http-logging.cfg.xml,Start Bad,503
http-logging.cfg.xml,Good to Bad,200
http-logging.cfg.xml,Bad to Good,200
uri-identity.cfg.xml,Start Good,200
uri-identity.cfg.xml,Start Bad,503
uri-identity.cfg.xml,Good to Bad,200
uri-identity.cfg.xml,Bad to Good,200
header-identity.cfg.xml,Start Good,200
header-identity.cfg.xml,Start Bad,503
header-identity.cfg.xml,Good to Bad,200
header-identity.cfg.xml,Bad to Good,200
ip-identity.cfg.xml,Start Good,200
ip-identity.cfg.xml,Start Bad,503
ip-identity.cfg.xml,Good to Bad,200
ip-identity.cfg.xml,Bad to Good,200
response-messaging.cfg.xml,Start Missing,200""".splitlines()

for case in cases:
    (config, transition, result) = case.split(',')

    transition = transition.replace(' to ', ' To ')

    classname = ('Test' + components_by_config[config].replace(' ', '') +
                 transition.replace(' ',''))

    transition = transition.lower()

    test_method_name = 'test_' + transition.replace(' ', '_')

    config_folder_base = re.sub('\\..*', '', config)

    starts_bad = transition in ['start bad', 'bad to good']

    is_start = transition.startswith('start')
    if is_start:
        config_folder_start = transition[6:]
    else:
        config_folder_start = transition.split()[0]
        config_folder_end = transition.split()[2]

    is_sysmod_or_container = (config in ['system-model.cfg.xml',
                                         'container.cfg.xml' ])
    wait_on_start = not is_sysmod_or_container

    print 'class {0}(unittest.TestCase):'.format(classname)
    print '    def setUp(self):'
    print '        self.repose_port = get_next_open_port()'
    print '        self.stop_port = get_next_open_port()'
    print '        self.url = \'http://localhost:{0}/\'.format(self.repose_port)'
    print '        params = {'
    print '            \'port\': self.repose_port,'
    print '            \'target_hostname\': \'localhost\','
    print '            \'target_port\': mock_port,'
    print '        }'
    print '        clear_folder(repose_config_folder)'
    print '        conf.process_folder_contents('
    print '            folder=\'configs/{0}-common\','.format(config_folder_base)
    print '            dest_path=repose_config_folder, params=params)'
    if transition != 'start missing':
        print '        conf.process_folder_contents('
        print '            folder=\'configs/{0}-{1}\','.format(config_folder_base,
                                                       config_folder_start)
        print '            dest_path=repose_config_folder, params=params)'
    print ''
    print '        self.valve = valve.Valve(repose_config_folder,'
    print '                                 stop_port=self.stop_port,'
    print '                                 port=repose_port,'
    print '                                 wait_timeout=30,'
    print '                                 wait_on_start={0})'.format(wait_on_start)
    if not wait_on_start:
        print '        time.sleep(sleep_time)'
    print ''

    print '    def {0}(self):'.format(test_method_name)

    if is_sysmod_or_container and starts_bad:
        print '        self.assertRaises(requests.ConnectionError, requests.get, self.url)'
    elif starts_bad:
        print '        self.assertEquals(503, get_status_code_from_url(self.url))'
    else:
        print '        self.assertEquals({0}, get_status_code_from_url(self.url))'.format(result)

    if not is_start:
        print ''
        print '        params = {'
        print '            \'port\': self.repose_port,'
        print '            \'target_hostname\': \'localhost\','
        print '            \'target_port\': mock_port,'
        print '        }'
        print '        conf.process_folder_contents('
        print '            folder=\'configs/{0}-{1}\','.format(config_folder_base,
                                                       config_folder_end)
        print '            dest_path=repose_config_folder, params=params)'
        print '        time.sleep(sleep_time)'
        print ''
        print '        self.assertEquals({0}, get_status_code_from_url(self.url))'.format(result)
    print ''

    print '    def tearDown(self):'
    print '        if self.valve:'
    print '            self.valve.stop()'
    print ''
    print ''



