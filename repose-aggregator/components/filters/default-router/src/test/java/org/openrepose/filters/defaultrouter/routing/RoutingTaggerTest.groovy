package org.openrepose.filters.defaultrouter.routing

import org.openrepose.commons.utils.http.HttpStatusCode
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.filter.logic.FilterAction
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.impl.SimplePassFilterDirector
import com.rackspace.papi.model.Destination
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.powermock.api.mockito.PowerMockito.when

class RoutingTaggerTest extends Specification {
    def tagger, request, response

    void setup() {
        tagger = new RoutingTagger()
        request = mock(HttpServletRequest.class)
        when(request.getRequestURI()).thenReturn("/")
        response = mock(ReadableHttpServletResponse.class)
    }

    void cleanup() {

    }

    def "SetDestination"() {
        when:
        tagger.destination = new Destination(id: "1", protocol: "http", rootPath: "/", default: true)

        then:
        tagger.defaultDest.default
        tagger.defaultDest.id == "1"
        tagger.defaultDest.protocol == "http"
        tagger.defaultDest.rootPath == "/"

    }

    def "HandleRequest - happy path"() {
        when:
        tagger.destination = new Destination(id: "1", protocol: "http", rootPath: "/", default: true)
        FilterDirector director = tagger.handleRequest(request, response)

        then:
        director.destinations.size() == 1
        director.destinations[0].uri == tagger.defaultDest.rootPath
        director.destinations[0].destinationId == tagger.defaultDest.id

        director.filterAction == FilterAction.PASS
        director.responseStatus == HttpStatusCode.INTERNAL_SERVER_ERROR
        director.responseStatusCode == 500
    }

    def "HandleRequest - destination not set"() {
        when:
        FilterDirector director = tagger.handleRequest(request, response)

        then:
        director.destinations.size() == 0

        director.filterAction == FilterAction.PASS
        director.responseStatus == HttpStatusCode.INTERNAL_SERVER_ERROR
        director.responseStatusCode == 500
    }

    def "HandleResponse"() {
        when:
        SimplePassFilterDirector director = tagger.handleResponse(request, response)

        then:
        director.responseStatus == HttpStatusCode.OK
        director.responseMessageBody == SimplePassFilterDirector.EMPTY_STRING
        director.filterAction == FilterAction.PASS
    }
}
