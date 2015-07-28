package org.openrepose.filters.clientauth

import com.mockrunner.mock.web.MockFilterConfig
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import spock.lang.Specification

import static org.mockito.Mockito.mock

class AuthNFilterDeprecationTest extends Specification {
    def 'logs a deprecation warning upon init'() {
        given:
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

        def ctx = LogManager.getContext(ClientAuthenticationFilter.class.getClassLoader(), false) as LoggerContext
        def appender = ctx.getConfiguration().getAppender("List0") as ListAppender

        def filter = new ClientAuthenticationFilter(mock(DatastoreService.class), mock(ConfigurationService.class), null, null)

        when:
        filter.init(new MockFilterConfig())

        then:
        appender.getMessages().find {
            it.contains('This filter is deprecated; use the keystone-v2 filter')
        }
    }
}
