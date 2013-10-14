package com.rackspace.papi.commons.util.http

import org.junit.Test
/**
 * Some groovy tests as an experiment.
 */

class EndpointsHeaderTest {

    @Test
    void matchesShouldReturnTrueWhenComparing2StringsAndShouldIgnoreCase() {

        def eph = EndpointsHeader.X_CATALOG

        def headers = ["x-CaTaLoG", "x-CATALOG", "X-catalog"]

        for (String h : headers) {
            def matches = eph.matches(h)

            assert matches == true
        }

    }

    @Test
    void matchesShouldReturnFalseWhenStringDoesNotMatch() {

        def eph = EndpointsHeader.X_CATALOG
        def headers = ["asdf", "x-CTALOG", "X-catalogd", "xx-catalog"]

        for (String h : headers) {
            def matches = eph.matches(h)

            assert matches == false
        }

    }
}
