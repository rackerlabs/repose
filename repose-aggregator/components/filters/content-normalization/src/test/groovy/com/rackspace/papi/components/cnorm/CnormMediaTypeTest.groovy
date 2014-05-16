package com.rackspace.papi.components.cnorm

import com.rackspace.papi.commons.util.http.CommonHttpHeader
import com.rackspace.papi.commons.util.http.header.HeaderName
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig
import com.rackspace.papi.components.normalization.config.MediaType
import com.rackspace.papi.components.normalization.config.MediaTypeList
import com.rackspace.papi.filter.logic.FilterDirector
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class CnormMediaTypeTest extends Specification {

    MediaType preferred(String type) {
        MediaType mt = new MediaType()
        mt.preferred = true
        mt.name = type
        mt
    }

    MediaType mediaType(String type) {
        MediaType mt = new MediaType()
        mt.name = type
        mt.preferred = false
        mt
    }

    HttpServletRequest request;
    ReadableHttpServletResponse response;

    def setup() {
        request = mock(HttpServletRequest.class)
        response = mock(ReadableHttpServletResponse.class)

        //Common mocks on all the tests
        when(request.getRequestURI()).thenReturn("http://www.example.com/derp/derp")

    }

    def buildHandler(List<MediaType> mediaTypes) {
        ContentNormalizationConfig config = new ContentNormalizationConfig()
        def mediaTypesList = new MediaTypeList()
        mediaTypesList.getMediaType().addAll(mediaTypes)

        config.setMediaTypes(mediaTypesList)

        ContentNormalizationHandlerFactory factory = new ContentNormalizationHandlerFactory()
        factory.configurationUpdated(config)

        factory.buildHandler()
    }

    def mockIncomingAccept(String acceptHeader) {
        if (acceptHeader != null) {
            //Only include the accept header in the list of headers if it's been set
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(["accept"]))
            when(request.getHeaders(CommonHttpHeader.ACCEPT.toString())).thenReturn(
                    Collections.enumeration(acceptHeader.split(",").toList())
            )
        } else {
            when(request.getHeaders(CommonHttpHeader.ACCEPT.toString())).thenReturn(
                    Collections.enumeration(Collections.emptyList())
            )
        }

    }

    @Unroll("Chooses the preferred accept type when accept is #incomingAccept")
    def "Chooses the preferred accept type with only a single preferred type"() {
        given:
        //create a config that prefers an accept type
        def handler = buildHandler([preferred("application/json")])

        FilterDirector director
        mockIncomingAccept(incomingAccept)

        when:
        director = handler.handleRequest(request, response)

        then:
        director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")) == ["application/json"].toSet()

        where:

        incomingAccept << [
                "derp/herp",
                "foo/bux,derp/herp,application/xml",
                "application/xml;q=123,application/json"
        ]
    }

    @Unroll("Chooses a type that is included in the list when accept is #incomingAccept")
    def "chooses a type that's not the preferred, when it's in the list"() {
        given:
        def handler = buildHandler([
                preferred("application/json"),
                mediaType("application/xml"),
                mediaType("application/yourMom")
        ])

        mockIncomingAccept(incomingAccept)
        FilterDirector director

        when:
        director = handler.handleRequest(request, response)

        then:
        //TODO: make copypasta assertion go away
        director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")) == ["application/xml"].toSet()

        where:
        incomingAccept << [
                "application/xml,derp/herp",
                "application/xml;q=123",
                "application/yourMom,application/xml",
                "application/yourMom,derp/herp,application/xml;q=123",
                "application/yourMom,application/xml;q=123;fkejf;3kj3kj"
        ]
    }

    @Unroll("Chooses preferred when none match the list when accept is #incomingAccept")
    def "chooses the preferred, when nothing in the list matches"() {
        given:
        def handler = buildHandler([
                preferred("application/json"),
                mediaType("application/xml"),
                mediaType("application/yourMom")
        ])

        mockIncomingAccept(incomingAccept)
        FilterDirector director

        when:
        director = handler.handleRequest(request, response)

        then:
        //TODO: make copypasta assertion go away
        director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")) == ["application/json"].toSet()

        where:
        incomingAccept << [
                "application/derp,derp/herp",
                "application/woooo;q=123",
                "application/yoooourMom,application/butts",
                "*/*",
                "*/json",
                "*/xml"
        ]
    }

    @Unroll("modifies the header for acceptable types #incomingAccept")
    def "Modifies the header for an accepted type"() {
        given:
        def handler = buildHandler([
                preferred("application/json"),
                mediaType("application/xml"),
                mediaType("application/yourMom")
        ])

        mockIncomingAccept(incomingAccept)
        FilterDirector director

        when:
        director = handler.handleRequest(request, response)

        then:
        //TODO: make copypasta assertion go away
        director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")) == [incomingAccept].toSet()

        where:
        incomingAccept << [
                "application/xml",
                "application/json",
                "application/yourMom"
        ]
    }


    def "Applies the preferred accept type if one is not specified"() {
        given:
        def handler = buildHandler([
                preferred("application/json"),
        ])

        mockIncomingAccept(null)
        FilterDirector director

        when:
        director = handler.handleRequest(request, response)

        then:
        //TODO: make copypasta assertion go away
        director.requestHeaderManager().headersToRemove().contains(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("accept"))
        director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("accept")) == ["application/json"].toSet()

    }
}
