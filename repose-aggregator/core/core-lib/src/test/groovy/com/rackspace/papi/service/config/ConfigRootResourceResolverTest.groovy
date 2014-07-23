package com.rackspace.papi.service.config

import com.rackspace.papi.service.config.resource.impl.BufferedURLConfigurationResource
import com.rackspace.papi.service.config.impl.ConfigRootResourceResolver
import spock.lang.Specification

/**
 * TODO: make this test more valuable or make it go away
 */
class ConfigRootResourceResolverTest extends Specification {

    String fakeFullPath = "file:/something"
    String partialPath = "something"
    String root = "root"
    String filePrepended = "file:/"
    ConfigRootResourceResolver fileDirectoryResourceResolver
    BufferedURLConfigurationResource bufferedURLConfigurationResource

    def "when resolving a resource, the resource should be passed on as is if a :/ is detected"() {
        when:
        fileDirectoryResourceResolver = new ConfigRootResourceResolver()
        bufferedURLConfigurationResource = fileDirectoryResourceResolver.resolve(fakeFullPath)

        then:
        bufferedURLConfigurationResource.name() == fakeFullPath
    }

    def "when resolving a resource, the resource location should have file:/ prepended"() {
        when:
        fileDirectoryResourceResolver = new ConfigRootResourceResolver()
        bufferedURLConfigurationResource = fileDirectoryResourceResolver.resolve(partialPath)

        then:
        bufferedURLConfigurationResource.name().startsWith(filePrepended)
    }
}
