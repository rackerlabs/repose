package com.rackspace.papi.components.uristripper

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.uristripper.config.UriStripperConfig
import com.rackspace.papi.filter.logic.FilterAction
import com.rackspace.papi.filter.logic.FilterDirector
import org.junit.Before
import org.junit.Test

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class UriStripperHandlerFactoryTest {

    UriStripperConfig conf
    UriStripperHandlerFactory factory;
    HttpServletRequest request
    ReadableHttpServletResponse response

    @Before
    void setUp() {

        conf = new UriStripperConfig()
        conf.setRewriteLocation(true)
        conf.setTokenIndex(1)

        factory = new UriStripperHandlerFactory();

        factory.configurationUpdated(conf);

        request = mock(HttpServletRequest.class)
        response = mock(ReadableHttpServletResponse.class)
    }


    @Test
    public void shouldStripOutItemInConfiguredPosition() {

        UriStripperHandler handler = factory.buildHandler()

        when(request.getRequestURI()).thenReturn("/v1/12345/path/to/resource")

        FilterDirector director = handler.handleRequest(request, response)

        assert handler.token == "12345"
        assert handler.prevToken == "v1"
        assert handler.nextToken == "path"
        assert director.getRequestUri() == "/v1/path/to/resource"
        assert director.getFilterAction() == FilterAction.PROCESS_RESPONSE


    }

    @Test
    public void shouldAddBackRemovedTokenToLocationHeader() {

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("http://service.com/v1/path/to/resource")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("http://service.com/v1/12345/path/to/resource")

    }

    @Test
    public void shouldAddBackRemovedTokenToLocationHeaderThatIsNottAnAbsoluteURL() {
        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("/v1/path/to/resource")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("/v1/12345/path/to/resource")

    }

    @Test
    public void shouldAddBackTokenAfterPreviousToken() {

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("http://service.com/v1/to/resource")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("http://service.com/v1/12345/to/resource")

    }

    @Test
    public void shouldAddBackTokenBeforeNextToken() {

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("http://service.com/v2/path/to/resource")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("http://service.com/v2/12345/path/to/resource")

    }

    @Test
    public void shouldAddBackRemovedTokenToLocationHeaderWithQueryparams() {

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("http://service.com/v1/path/to/resource?a=b&c=d")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("http://service.com/v1/12345/path/to/resource?a=b&c=d")

    }


    @Test
    public void shouldAddBackRemovedTokenToLocationHeaderThatIsNotAnAbsoluteURLWithQueryparams() {

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("/v1/path/to/resource?a=b&c=d")

        handler.token = "12345"
        handler.prevToken = "v1"
        handler.nextToken = "path"

        FilterDirector director = handler.handleResponse(request, response)

        assert director.responseHeaderManager().headersToAdd().containsKey("location")


        assert director.responseHeaderManager().headersToAdd().get("location").contains("/v1/12345/path/to/resource?a=b&c=d")

    }

    @Test
    public void shouldNotRewriteLocationHeaderOnBadURL(){

        UriStripperHandler handler = factory.buildHandler();

        when(response.getHeader("location")).thenReturn("htpp;//service.com/v1/path342(82323/\\to/resource?a=b&c=d")

        FilterDirector director = handler.handleResponse(request, response)

        assert !director.responseHeaderManager().headersToAdd().containsKey("location")

    }


}
