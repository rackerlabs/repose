package org.openrepose.filters.authz

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.springframework.mock.web.MockFilterConfig
import spock.lang.Specification

import static org.mockito.Mockito.mock

class AuthZFilterDeprecationTest extends Specification {

    LoggerContext ctx = LogManager.getContext(RackspaceAuthorizationFilter.class.getClassLoader(), false) as LoggerContext
    ListAppender appender = ctx.getConfiguration().getAppender("List") as ListAppender

    def setup() {
        appender.clear()
    }

    def 'logs a deprecation warning upon init'() {
        given:
        def filter = new RackspaceAuthorizationFilter(mock(ConfigurationService.class), mock(DatastoreService.class), null, null)

        when:
        filter.init(new MockFilterConfig())

        then:
        appender.getEvents().find {
            it.getMessage().getFormattedMessage() == 'This filter is deprecated; use the keystone-v2 filter'
        }
    }
}
