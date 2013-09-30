package com.rackspace.papi.commons.util.servlet.http
import org.junit.Before
import org.junit.Test

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class RequestHeaderContainerTest {


    RequestHeaderContainer container;
    Enumeration<String> headerNames;
    Enumeration<String> headerValues1;
    Enumeration<String> headerValues2;
    HttpServletRequest originalRequest;
    String header1 = "val1.1"
    String header2 = "1.1 Proxy(blah , ), 1.1 Other(three,two,one)"


    @Before
    void setUp() {

        headerNames = createStringEnumeration("accept", "via");
        headerValues1 = createStringEnumeration(header1, "val1.2");
        headerValues2 = createStringEnumeration(header2);

        originalRequest = mock(HttpServletRequest.class);

        when(originalRequest.getHeaderNames()).thenReturn(headerNames);
        when(originalRequest.getHeaders("accept")).thenReturn(headerValues1);
        when(originalRequest.getHeaders("via")).thenReturn(headerValues2);
        when(originalRequest.getHeader("accept")).thenReturn(header1);
        when(originalRequest.getHeader("via")).thenReturn(header2);

        container = new RequestHeaderContainer(originalRequest)


    }

    @Test
    void testGetHeaderNames() {

        assert container.getHeaderNames().size() == 2
        assert container.getHeaderNames().contains("accept")
        assert container.getHeaderNames().contains("via")
    }

    @Test
    void testGetHeaderValues() {

        assert container.getHeaderValues("accept").size() == 2
        assert container.getHeaderValues("via").size() == 1

    }

    @Test
    void testContainsHeader() {

        assert container.containsHeader("accept")
        assert container.containsHeader("via")

    }

    @Test
    void testGetContainerType() {
         assert container.getContainerType() == HeaderContainerType.REQUEST
    }

    Enumeration<String> createStringEnumeration(String... names) {
        Vector<String> namesCollection = new Vector<String>(names.length);

        namesCollection.addAll(Arrays.asList(names));

        return namesCollection.elements();
    }

}
