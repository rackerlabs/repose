package com.rackspace.papi.components.clientauth.common

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
