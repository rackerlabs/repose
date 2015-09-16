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
    LoggerContext ctx = LogManager.getContext(ClientAuthenticationFilter.class.getClassLoader(), false) as LoggerContext
    ListAppender appender = ctx.getConfiguration().getAppender("List0") as ListAppender

    def setup() {
        appender.clear()
    }

    def 'logs a deprecation warning upon init'() {
        given:
        def filter = new ClientAuthenticationFilter(mock(DatastoreService.class), mock(ConfigurationService.class), null, null, "0.0.0.0")

        when:
        filter.init(new MockFilterConfig())

        then:
        appender.getMessages().find {
            it.contains('This filter is deprecated; use the keystone-v2 filter')
        }
    }
}
