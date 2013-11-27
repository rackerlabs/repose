package com.rackspace.papi.commons.config.resource.impl

import spock.lang.Specification

class FileDirectoryResourceResolverTest extends Specification {

    String fakeFullPath = "file:/something"
    String partialPath = "something"
    String root = "root"
    String filePrepended = "file:/"
    FileDirectoryResourceResolver fileDirectoryResourceResolver
    BufferedURLConfigurationResource bufferedURLConfigurationResource

    def "when resolving a resource, the resource should be passed on as is if a :/ is detected"() {
        when:
        fileDirectoryResourceResolver = new FileDirectoryResourceResolver(root)
        bufferedURLConfigurationResource = fileDirectoryResourceResolver.resolve(fakeFullPath)

        then:
        bufferedURLConfigurationResource.name() == fakeFullPath
    }

    def "when resolving a resource, the resource location should have file:/ prepended"() {
        when:
        fileDirectoryResourceResolver = new FileDirectoryResourceResolver(root)
        bufferedURLConfigurationResource = fileDirectoryResourceResolver.resolve(partialPath)

        then:
        bufferedURLConfigurationResource.name().startsWith(filePrepended)
    }
}
