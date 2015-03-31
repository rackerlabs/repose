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
package org.openrepose.filters.cnorm

import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.filters.cnorm.config.MediaType
import org.openrepose.filters.cnorm.normalizer.MediaTypeNormalizer
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class MediaTypeNormalizerTest extends Specification {

    List<MediaType> configuredMediaTypes
    MediaTypeNormalizer normalizer

    HttpServletRequest request
    FilterDirector director

    def setup() {
        configuredMediaTypes = new LinkedList<MediaType>()
        final MediaType configuredMediaType = new MediaType()
        configuredMediaType.setName("application/xml")
        configuredMediaType.setVariantExtension("xml")
        configuredMediaType.setPreferred(Boolean.TRUE)

        configuredMediaTypes.add(configuredMediaType)

        normalizer = new MediaTypeNormalizer(configuredMediaTypes)

        //Spock mocks don't cooperate with the httpServletRequest class
        request = mock(HttpServletRequest.class)
        director = new FilterDirectorImpl()
    }

    @Unroll("When normalizing variant extensions, it correctly #desc")
    def "correct behavior"(String desc, String uri, String url) {
        given:
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));

        MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director)

        expect:
        identifiedMediaType != null
        identifiedMediaType.getVariantExtension() == "xml"
        //These assertions require it to be XML, and that the urls are simplistic enough to only have one .xml in them
        director.getRequestUri() == uri.replace(".xml", "")
        director.getRequestUrl().toString() == url.replace(".xml", "")

        where:
        desc                                         | uri                                             | url
        "captures variant extensions"                | "/a/request/uri.xml"                            | "http://localhost/a/request/uri.xml"
        "ignores query parameters"                   | "/a/request/uri.xml?name=name&value=1"          | "http://localhost/a/request/uri.xml?name=name&value=1"
        "ignores URI fragments"                      | "/a/request/uri.xml#fragment"                   | "http://localhost/a/request/uri.xml#fragment"
        "ignores uri fragments and query parameters" | "/a/request/uri.xml?name=name&value=1#fragment" | "http://localhost/a/request/uri.xml?name=name&value=1#fragment"
        "captures unusual variants extensions"       | "/a/request/uri/.xml"                           | "http://localhost/a/request/uri/.xml"
    }
}
