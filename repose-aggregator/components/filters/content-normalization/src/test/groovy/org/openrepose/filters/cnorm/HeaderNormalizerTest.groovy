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

import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.filters.cnorm.config.HeaderFilterList
import org.openrepose.filters.cnorm.config.HttpHeader
import org.openrepose.filters.cnorm.config.HttpHeaderList
import org.openrepose.filters.cnorm.normalizer.HeaderNormalizer
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class HeaderNormalizerTest extends Specification {

    HttpServletRequest request
    FilterDirector director
    HttpHeader authHeader, userHeader, groupHeader, contentType
    HeaderNormalizer headerNormalizer

    HttpHeaderList blacklist = new HttpHeaderList()
    HttpHeaderList whitelist = new HttpHeaderList()
    HeaderFilterList headerFilterList = new HeaderFilterList()

    def setup() {
        request = mock(HttpServletRequest.class)

        director = new FilterDirectorImpl()

        authHeader = new HttpHeader()
        userHeader = new HttpHeader()
        groupHeader = new HttpHeader()
        contentType = new HttpHeader()

        authHeader.setId("X-Auth-Header")
        userHeader.setId("X-User-Header")

        blacklist.getHeader().add(authHeader)
        blacklist.getHeader().add(userHeader)

        groupHeader.setId("X-Group-Header")
        contentType.setId("Content-Type")

        whitelist.getHeader().add(groupHeader)
        whitelist.getHeader().add(contentType)

        def requestHeaders = ["X-Auth-Header", "Content-Type", "X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"]
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(requestHeaders))

        headerFilterList.setBlacklist(blacklist)
        headerFilterList.setWhitelist(whitelist)
        headerNormalizer = new HeaderNormalizer(headerFilterList, true)
    }

    def "properly handles whitelist and blacklist"() {
        when:
        headerNormalizer.normalizeHeaders(request, director)

        def headersToRemove = director.requestHeaderManager().headersToRemove()

        then:
        headersToRemove.contains(HeaderName.wrap(authHeader.getId()))
        headersToRemove.contains(HeaderName.wrap(userHeader.getId()))
        !headersToRemove.contains(HeaderName.wrap("accept"))
        !headersToRemove.contains(HeaderName.wrap(groupHeader.getId()))
        !headersToRemove.contains(HeaderName.wrap(contentType.getId()))
    }

}
