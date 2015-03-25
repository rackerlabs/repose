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
package org.openrepose.filters.clientauth.common

import spock.lang.Specification

class EndpointsConfigurationTest extends Specification {

    def "when getting the format, it should return json or xml only"() {
        Long cacheTimeout
        Integer idContractversion
        EndpointsConfiguration configuration1, configuration2, configuration3

        given:
        cacheTimeout = 1000
        idContractversion = 2
        configuration1 = new EndpointsConfiguration("json", cacheTimeout, idContractversion)
        configuration2 = new EndpointsConfiguration("xml", cacheTimeout, idContractversion)
        configuration3 = new EndpointsConfiguration("foo", cacheTimeout, idContractversion)

        when:
        String a = configuration1.getFormat()
        String b = configuration2.getFormat()
        String c = configuration3.getFormat()

        then:
        a == "json"
        b == "xml"
        c == "json"
    }

    def "when getting cache timeout, it should return set value or 600000"() {
        String format
        Integer idContractversion
        EndpointsConfiguration configuration1, configuration2

        given:
        format = "json"
        idContractversion = 2
        configuration1 = new EndpointsConfiguration(format, null, idContractversion)
        configuration2 = new EndpointsConfiguration(format, 5000, idContractversion)

        when:
        Long a = configuration1.getCacheTimeout()
        Long b = configuration2.getCacheTimeout()

        then:
        a == 600000
        b == 5000
    }

    def "when getting identity contract version, only 2 should be returned"() {
        String format
        Long cacheTimeout
        Integer idContractversion1, idContractversion2
        EndpointsConfiguration configuration1, configuration2

        given:
        format = "json"
        cacheTimeout = 1000
        idContractversion1 = 1
        idContractversion2 = null
        configuration1 = new EndpointsConfiguration(format, cacheTimeout, idContractversion1)
        configuration2 = new EndpointsConfiguration(format, cacheTimeout, idContractversion2)

        when:
        Integer a = configuration1.getIdentityContractVersion()
        Integer b = configuration2.getIdentityContractVersion()

        then:
        a == 2
        b == 2
    }
}
