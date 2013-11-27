package com.rackspace.papi.external.testing.mocks

import org.junit.Test


class MocksUtilTest {

    String testXml = """<request-info
    xmlns="http://openrepose.org/repose/httpx/v1.0">
    <method>GET</method>
    <path>/blah</path>
    <uri>request-info.com/blah</uri>

    <headers>
        <header name="accept" value="application/xml"/>
    </headers>

    <query-params>
        <parameter name="q" value="r"/>
    </query-params>

    <body><blah/></body>

</request-info>"""

    @Test
    void testUnmarshallingFromStream() {

        RequestInformation req = MocksUtil.getRequestInfo(testXml)

        assert req.getHeaders().header.get(0).name == "accept"

    }

    @Test
    void testMarshallToXml(){

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
        h2.name = "x-pp-user"
        h2.value = "usertest1"
        headers.add(h1)
        headers.add(h2)
        headerList.header  = headers

        QueryParameters params = factory.createQueryParameters();
        List<NameValuePair> queryParams = new ArrayList<NameValuePair>()
        NameValuePair p1 = factory.createNameValuePair()
        p1.name = "p"
        p1.value = "q"
        queryParams.add(p1)
        params.parameter = queryParams
                                    
        req.headers = headerList
        req.queryParams = params

        String xml = MocksUtil.getRequestInfoXml(req)

        assert xml.length() != 0

        RequestInformation requestInformation = MocksUtil.getRequestInfo(xml)

        assert requestInformation.getBody() == req.body
    }

}
