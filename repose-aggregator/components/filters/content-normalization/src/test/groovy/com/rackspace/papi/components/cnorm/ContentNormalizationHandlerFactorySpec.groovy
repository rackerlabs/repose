package com.rackspace.papi.components.cnorm

import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig
import spock.lang.Specification


class ContentNormalizationHandlerFactorySpec extends Specification {

    ContentNormalizationHandlerFactory instance

    def setup() {
        instance = new ContentNormalizationHandlerFactory()
    }

    def "creates a new config listener"() {
        expect:
        instance.getListeners().size() == 1
    }

    def "creates new instance of ContentNormalizationHandler"() {
        given:
        def config = new ContentNormalizationConfig()
        instance.configurationUpdated(config)

        def handler

        when:
        handler = instance.buildHandler()

        then:
        handler != null
    }
}
