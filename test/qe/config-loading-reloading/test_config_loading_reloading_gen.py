

class TestSystemModelStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/system-model-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/system-model-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestSystemModelStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/system-model-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/system-model-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_start_bad(self):
        self.assertRaises(requests.ConnectionError, requests.get, self.url)

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestSystemModelGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/system-model-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/system-model-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/system-model-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestSystemModelBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/system-model-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/system-model-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_bad_to_good(self):
        self.assertRaises(requests.ConnectionError, requests.get, self.url)

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/system-model-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestContainerStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/container-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/container-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestContainerStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/container-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/container-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_start_bad(self):
        self.assertRaises(requests.ConnectionError, requests.get, self.url)

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestContainerGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/container-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/container-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/container-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestContainerBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/container-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/container-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=False)
        time.sleep(sleep_time)

    def test_bad_to_good(self):
        self.assertRaises(requests.ConnectionError, requests.get, self.url)

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/container-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestResponseMessagingServiceStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/response-messaging-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestResponseMessagingServiceStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/response-messaging-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestResponseMessagingServiceGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/response-messaging-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/response-messaging-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestResponseMessagingServiceBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/response-messaging-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/response-messaging-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestRateLimitingStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/rate-limiting-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/rate-limiting-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestRateLimitingStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/rate-limiting-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/rate-limiting-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestRateLimitingGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/rate-limiting-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/rate-limiting-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/rate-limiting-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestRateLimitingBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/rate-limiting-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/rate-limiting-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/rate-limiting-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestVersioningStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/versioning-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/versioning-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestVersioningStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/versioning-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/versioning-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestVersioningGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/versioning-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/versioning-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/versioning-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestVersioningBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/versioning-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/versioning-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/versioning-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestTranslationStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/translation-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/translation-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestTranslationStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/translation-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/translation-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestTranslationGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/translation-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/translation-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/translation-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestTranslationBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/translation-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/translation-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/translation-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthenticationStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/client-auth-n-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/client-auth-n-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthenticationStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/client-auth-n-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/client-auth-n-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthenticationGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/client-auth-n-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/client-auth-n-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/client-auth-n-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthenticationBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/client-auth-n-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/client-auth-n-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/client-auth-n-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthorizationStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(401, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthorizationStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthorizationGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(401, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/openstack-authorization-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(401, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestClientAuthorizationBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/openstack-authorization-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/openstack-authorization-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(401, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestDistributedDatastoreStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/dist-datastore-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/dist-datastore-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestDistributedDatastoreStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/dist-datastore-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/dist-datastore-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestDistributedDatastoreGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/dist-datastore-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/dist-datastore-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/dist-datastore-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestDistributedDatastoreBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/dist-datastore-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/dist-datastore-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/dist-datastore-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHttpLoggingStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/http-logging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/http-logging-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHttpLoggingStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/http-logging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/http-logging-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHttpLoggingGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/http-logging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/http-logging-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/http-logging-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHttpLoggingBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/http-logging-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/http-logging-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/http-logging-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestUriIdentityStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/uri-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/uri-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestUriIdentityStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/uri-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/uri-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestUriIdentityGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/uri-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/uri-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/uri-identity-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestUriIdentityBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/uri-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/uri-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/uri-identity-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHeaderIdentityStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/header-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/header-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHeaderIdentityStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/header-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/header-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHeaderIdentityGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/header-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/header-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/header-identity-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestHeaderIdentityBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/header-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/header-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/header-identity-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestIpIdentityStartGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/ip-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/ip-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_good(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestIpIdentityStartBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/ip-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/ip-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_bad(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestIpIdentityGoodToBad(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/ip-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/ip-identity-good',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_good_to_bad(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/ip-identity-bad',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestIpIdentityBadToGood(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/ip-identity-common',
            dest_path=repose_config_folder, params=params)
        conf.process_folder_contents(
            folder='configs/ip-identity-bad',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_bad_to_good(self):
        self.assertEquals(503, get_status_code_from_url(self.url))

        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        conf.process_folder_contents(
            folder='configs/ip-identity-good',
            dest_path=repose_config_folder, params=params)
        time.sleep(sleep_time)

        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()


class TestResponseMessagingServiceStartMissing(unittest.TestCase):
    def setUp(self):
        self.repose_port = get_next_open_port()
        self.stop_port = get_next_open_port()
        self.url = 'http://localhost:{0}/'.format(self.repose_port)
        params = {
            'port': self.repose_port,
            'target_hostname': 'localhost',
            'target_port': mock_port,
        }
        clear_folder(repose_config_folder)
        conf.process_folder_contents(
            folder='configs/response-messaging-common',
            dest_path=repose_config_folder, params=params)

        self.valve = valve.Valve(repose_config_folder,
                                 stop_port=self.stop_port,
                                 port=repose_port,
                                 wait_timeout=30,
                                 wait_on_start=True)

    def test_start_missing(self):
        self.assertEquals(200, get_status_code_from_url(self.url))

    def tearDown(self):
        if self.valve:
            self.valve.stop()
