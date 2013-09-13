package com.rackspace.papi.commons.util.http.header

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader
import com.rackspace.papi.commons.util.http.PowerApiHeader
import org.junit.Test

class SplitableHeaderUtilTest {

    @Test
    void testSplitable() {

        SplitableHeaderUtil splitable = new SplitableHeaderUtil();

        assert splitable.isSplitable("Via")
        assert splitable.isSplitable("VIA")
        assert splitable.isSplitable("via")

    }

    @Test
    void testUnSplitable(){

        SplitableHeaderUtil splitable = new SplitableHeaderUtil();

        assert !splitable.isSplitable("unsplitable")
    }

    @Test
    void testLargSplitableList(){

        SplitableHeaderUtil splitable = new SplitableHeaderUtil(PowerApiHeader.values())

        assert splitable.isSplitable("x-pp-user")

    }

    @Test
    void testSplitableList(){

        SplitableHeaderUtil splitable = new SplitableHeaderUtil(PowerApiHeader.USER)

        assert splitable.isSplitable("x-pp-user")

    }

        @Test
    void testLargerSplitableList(){

        SplitableHeaderUtil splitable = new SplitableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
        ExtendedHttpHeader.values())

        assert splitable.isSplitable("x-pp-user")
        assert splitable.isSplitable("X-Tenant-Name")
        assert splitable.isSplitable("x-tTl")
        assert !splitable.isSplitable("some-other-header")


    }
}
