package org.openrepose.external.testing.mocks

import org.openrepose.commons.utils.test.mocks.HeaderList
import org.openrepose.commons.utils.test.mocks.NameValuePair
import org.openrepose.commons.utils.test.mocks.ObjectFactory
import org.openrepose.commons.utils.test.mocks.RequestInformation
import org.openrepose.commons.utils.test.mocks.util.RequestInfo
import org.junit.Test

class RequestInfoTest {

    @Test
    void testGettingAllHeaders(){

        ObjectFactory factory = new ObjectFactory();
        RequestInformation req = factory.createRequestInformation();

        req.uri = "http://test.openrepose.org/path/to/resource"
        req.path = "/path/to/resource"
        req.method = "PATCH"
        req.body = "<some><body/>blah</some>"
        HeaderList headerList = factory.createHeaderList();
        List<NameValuePair> headers = new ArrayList<NameValuePair>()
        NameValuePair h1 = factory.createNameValuePair()
        h1.name = "accept"
        h1.value = "application/xml"
        NameValuePair h2 = factory.createNameValuePair()
        h2.name = "Accept"
        h2.value = "application/json"
        headers.add(h1)
        headers.add(h2)
        headerList.header = headers
        req.headers = headerList

        RequestInfo info = new RequestInfo(req)

        assert info.getHeaders().get("accept").size() == 2
        assert info.getHeaders().get("AccePt").size() == 2

    }

}
