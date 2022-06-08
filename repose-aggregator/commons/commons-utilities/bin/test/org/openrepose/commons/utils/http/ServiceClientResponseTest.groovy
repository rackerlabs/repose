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
package org.openrepose.commons.utils.http

import org.apache.http.Header
import org.apache.http.message.BasicHeader
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static javax.servlet.http.HttpServletResponse.*

class ServiceClientResponseTest extends Specification {

    @Shared
    def headersGood = [
        new BasicHeader("hdrname", "hdrvalue"),
        new BasicHeader("hdrName", "hdrValue"),
        new BasicHeader("HdrName", "HdrValue"),
        new BasicHeader("HDRNAME", "HDRVALUE"),
    ] as Header[]

    @Shared
    def valuesGood = headersGood.collect { it.getValue() }

    @Shared
    def headersBad = [
        new BasicHeader("hdrbad", "badvalue"),
        new BasicHeader("hdrBad", "badValue"),
        new BasicHeader("HdrBad", "BadValue"),
        new BasicHeader("HDRBAD", "BADVALUE"),
    ] as Header[]

    @Shared
    def valuesBad = headersBad.collect { it.getValue() }

    @Unroll
    def 'getStatus should return #status'() {
        given:
        def serviceClientResponse = new ServiceClientResponse(status, new ByteArrayInputStream(new byte[0]))

        expect:
        status == serviceClientResponse.getStatus()

        where:
        status << [SC_OK, SC_MOVED_PERMANENTLY, SC_BAD_REQUEST, SC_UNAUTHORIZED, SC_FORBIDDEN, SC_INTERNAL_SERVER_ERROR]
    }

    @Unroll
    def 'getData should return #bytes'() {
        given:
        def serviceClientResponse = new ServiceClientResponse(SC_OK, new ByteArrayInputStream(bytes))

        expect:
        Arrays.equals(serviceClientResponse.getData().bytes, bytes)

        where:
        bytes << [
            [0x01, 0x02] as byte[],
            [0x03, 0x04] as byte[],
            [0x05, 0x06] as byte[]
        ]
    }

    @Unroll
    def 'getHeader should return #headerName'() {
        given:
        def serviceClientResponse = new ServiceClientResponse(SC_OK, (headersGood + headersBad) as Header[], new ByteArrayInputStream(new byte[0]))

        when:
        def headers = serviceClientResponse.getHeaders(headerName)

        then:
        headers.containsAll(valuesGood)

        and:
        headers.intersect(valuesBad as Collection<String>).empty

        where:
        headerName << headersGood.collect { it.getName() }
    }

    def 'getHeaderElements should return'() {
        given:
        def headerName = "hdrname"
        def elementA = "element1"
        def elementB = "element2"
        def elementC = "element3"
        def elementD = "elementD"
        def headerWithElements = [
            new BasicHeader(headerName, """$elementA,$elementB;q=0.2,$elementC;a=b"""),
            new BasicHeader("hdrbad", elementD)
        ] as Header[]

        def serviceClientResponse = new ServiceClientResponse(SC_OK, headerWithElements, new ByteArrayInputStream(new byte[0]))

        when:
        def headerElementNames = serviceClientResponse.getHeaderElements(headerName).collect { it.getName() }

        then:
        headerElementNames.contains(elementA)
        headerElementNames.contains(elementB)
        headerElementNames.contains(elementC)

        and:
        !(headerElementNames.contains(elementD))
    }
}
