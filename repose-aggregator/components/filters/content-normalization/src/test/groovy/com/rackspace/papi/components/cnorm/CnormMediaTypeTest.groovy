package com.rackspace.papi.components.cnorm

import com.rackspace.papi.commons.util.http.CommonHttpHeader
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig
import com.rackspace.papi.components.normalization.config.MediaType
import com.rackspace.papi.components.normalization.config.MediaTypeList
import com.rackspace.papi.filter.logic.FilterDirector
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.when
import static org.mockito.Mockito.mock


class CnormMediaTypeTest extends Specification {

    def "Defect D-17479"() {
        given:
        //create a config that prefers an accept type
        ContentNormalizationConfig config = new ContentNormalizationConfig()
        MediaType preferredType = new MediaType()
        preferredType.preferred = true
        preferredType.name = "application/json"
        def mediaTypesList = new MediaTypeList()
        mediaTypesList.getMediaType().add(preferredType)

        config.setMediaTypes(mediaTypesList)

        ContentNormalizationHandlerFactory factory = new ContentNormalizationHandlerFactory()
        factory.configurationUpdated(config)

        def handler = factory.buildHandler()

        HttpServletRequest request = mock(HttpServletRequest.class)
        ReadableHttpServletResponse response = mock(ReadableHttpServletResponse.class)

        FilterDirector director
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(["accept"]))
        when(request.getRequestURI()).thenReturn("http://www.example.com/derp/derp")
        when(request.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn("derp/herp")

        when:
        director = handler.handleRequest(request, response)

        then:
        director.requestHeaderManager().headersToRemove().contains("accept")
        director.requestHeaderManager().headersToAdd().containsKey("accept")
        director.requestHeaderManager().headersToAdd().get("accept") == ["application/json"].toSet()
    }
}
