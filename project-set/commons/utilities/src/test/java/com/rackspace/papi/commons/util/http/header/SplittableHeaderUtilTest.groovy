package com.rackspace.papi.commons.util.http.header

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader
import com.rackspace.papi.commons.util.http.PowerApiHeader
import org.junit.Test

class SplittableHeaderUtilTest {

    @Test
    void testSplitable() {

        SplittableHeaderUtil splitable = new SplittableHeaderUtil();

        assert splitable.isSplitable("Via")
        assert splitable.isSplitable("VIA")
        assert splitable.isSplitable("via")

    }

    @Test
    void testUnSplitable(){

        SplittableHeaderUtil splitable = new SplittableHeaderUtil();

        assert !splitable.isSplitable("unsplitable")
    }

    @Test
    void testLargSplitableList(){

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.values())

        assert splitable.isSplitable("x-pp-user")

    }

    @Test
    void testSplitableList(){

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.USER)

        assert splitable.isSplitable("x-pp-user")

    }

        @Test
    void testLargerSplitableList(){

        SplittableHeaderUtil splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
        ExtendedHttpHeader.values())

        assert splitable.isSplitable("x-pp-user")
        assert splitable.isSplitable("X-Tenant-Name")
        assert splitable.isSplitable("x-tTl")
        assert !splitable.isSplitable("some-other-header")


    }
}
