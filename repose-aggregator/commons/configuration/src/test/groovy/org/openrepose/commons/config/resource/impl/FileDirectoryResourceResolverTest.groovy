/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.config.resource.impl

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
