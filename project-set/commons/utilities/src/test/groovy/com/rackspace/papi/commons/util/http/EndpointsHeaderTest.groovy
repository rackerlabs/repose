package com.rackspace.papi.commons.util.http

import spock.lang.Specification

/**
 * Some groovy tests as an experiment.
 */

class EndpointsHeaderTest extends Specification {

    def "matches should return true when comparing 2 strings and should ignore case"() {

        given:
        def eph = EndpointsHeader.X_CATALOG

        when:
        def matches = eph.matches(s)

        then:
        matches == true

        where:
        s << ["x-CaTaLoG", "x-CATALOG", "X-catalog"]
    }

    def "matches should return false when string does not match"() {

        given:
        def eph = EndpointsHeader.X_CATALOG

        when:
        def matches = eph.matches(s)

        then:
        matches == false

        where:
        s << ["asdf", "x-CTALOG", "X-catalogd", "xx-catalog"]
    }
}
