package org.openrepose.external.testing.mocks

import org.openrepose.commons.utils.test.mocks.*
import org.openrepose.commons.utils.test.mocks.util.MocksUtil
import org.openrepose.commons.utils.test.mocks.util.RequestInfo
import org.junit.Test

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class MocksUtilTest {

    String testXml = """<request-info
    xmlns="http://docs.openrepose.org/repose/httpx/v1.0">
    <method>GET</method>
    <path>/blah</path>
    <uri>request-info.com/blah</uri>

    <headers>
        <header name="accept" value="application/xml"/>
    </headers>

    <query-params>
        <parameter name="q" value="r"/>
    </query-params>

    <body>request body</body>

</request-info>"""

    @Test
    void testUnmarshallingFromStream() {

        RequestInformation req = MocksUtil.xmlStringToRequestInformation(testXml)

        assert req.getHeaders().header.get(0).name == "accept"
        assert req.getBody() == "request body"
    }

    @Test
    void testMarshallToXml() {

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
        headerList.header = headers
        QueryParameters params = factory.createQueryParameters();
        List<NameValuePair> queryParams = new ArrayList<NameValuePair>()
        NameValuePair p1 = factory.createNameValuePair()
        p1.name = "p"
        p1.value = "q"
        queryParams.add(p1)
        params.parameter = queryParams
        req.headers = headerList
        req.queryParams = params

        String xml = MocksUtil.requestInformationToXml(req)

        assert xml.length() != 0
        assert xml.contains("http://test.openrepose.org/path/to/resource")

        RequestInformation requestInformation = MocksUtil.xmlStringToRequestInformation(xml)

        assert requestInformation.getBody() == req.body
        assert requestInformation.getUri() == "http://test.openrepose.org/path/to/resource"
    }

    @Test
    void testServletRequestToRequestInfo() {

        HttpServletRequest servletRequest = mock(HttpServletRequest.class)
        when(servletRequest.getMethod()).thenReturn("PATCH")
        when(servletRequest.getRequestURL()).thenReturn(new StringBuffer("http://test.openrepose.org/path/to/resource"))
        when(servletRequest.getRequestURI()).thenReturn("/path/to/resource")
        when(servletRequest.getInputStream()).thenReturn(null);
        when(servletRequest.getQueryString()).thenReturn("q=1&q=2&r=3&r=4")

        Enumeration<String> headerNames = createStringEnumeration("accept", "host", "x-pp-user")
        Enumeration<String> acceptValues = createStringEnumeration("application/xml")
        Enumeration<String> hostValues = createStringEnumeration("http://test.openrepose.org")
        Enumeration<String> userValues = createStringEnumeration("usertest1", "usertest2")

        when(servletRequest.getHeaderNames()).thenReturn(headerNames)
        when(servletRequest.getHeaders("accept")).thenReturn(acceptValues)
        when(servletRequest.getHeaders("host")).thenReturn(hostValues)
        when(servletRequest.getHeaders("x-pp-user")).thenReturn(userValues)

        Enumeration<String> queryNames = createStringEnumeration("q", "r")
        Map<String, String[]> queryValues = new HashMap<String, String[]>()
        String[] qValues = new String[2]
        String[] rValues = new String[2]

        qValues[0] = "1"
        qValues[1] = "2"

        rValues[0] = "3"
        rValues[1] = "4"

        queryValues.put("q", qValues)
        queryValues.put("r", rValues)
        when(servletRequest.getParameterNames()).thenReturn(queryNames)
        when(servletRequest.getParameterMap()).thenReturn(queryValues)

        RequestInformation requestInformation = MocksUtil.servletRequestToRequestInformation(servletRequest)

        assert requestInformation.method == "PATCH"
        assert requestInformation.path == "/path/to/resource"
        assert requestInformation.getQueryString() == "q=1&q=2&r=3&r=4"

        RequestInfo info = new RequestInfo(requestInformation);

        assert info.method == "PATCH"
        assert info.path == "/path/to/resource"
        assert info.queryString == "q=1&q=2&r=3&r=4"
        assert info.getHeaders().get("accept").get(0) == "application/xml"
    }

    @Test
    void testXmlToRequestInfo(){

        RequestInfo info = MocksUtil.xmlStringToRequestInfo(testXml)
        assert info.method == "GET"
        assert info.path == "/blah"
        assert info.getHeaders().get("accept").get(0) == "application/xml"
    }

    static Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length)
        namesCollection.addAll(Arrays.asList(names))
        return namesCollection.elements()
    }

    @Test
    void testRequestInformationToRequestInfoNoQueryParams() {

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
        headerList.header = headers

        req.headers = headerList

        RequestInfo info = new RequestInfo(req)

        assert info.getQueryParams() != null
        assert info.getQueryParams().size() == 0
    }
}
