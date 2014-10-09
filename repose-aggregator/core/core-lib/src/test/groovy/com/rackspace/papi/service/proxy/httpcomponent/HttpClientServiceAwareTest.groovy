package com.rackspace.papi.service.proxy.httpcomponent

import org.openrepose.services.httpclient.api.HttpClientService
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import static org.junit.Assert.assertNotNull

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring/repose-with-services-context.xml")
class HttpClientServiceAwareTest {

    @Autowired
    @Qualifier("httpConnectionPoolService")
    HttpClientService httpClientService;

    @Test
    public void isAvailableInSpring() {
        assertNotNull(httpClientService)
    }

}
