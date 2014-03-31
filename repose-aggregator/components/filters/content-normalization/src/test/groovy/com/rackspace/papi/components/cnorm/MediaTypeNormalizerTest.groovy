package com.rackspace.papi.components.cnorm

import com.rackspace.papi.components.cnorm.normalizer.MediaTypeNormalizer
import com.rackspace.papi.components.normalization.config.MediaType
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when


class MediaTypeNormalizerTest extends Specification {

    List<MediaType> configuredMediaTypes
    MediaTypeNormalizer normalizer

    HttpServletRequest request
    FilterDirector director

    def setup() {
        configuredMediaTypes = new LinkedList<MediaType>()
        final MediaType configuredMediaType = new MediaType()
        configuredMediaType.setName("application/xml")
        configuredMediaType.setVariantExtension("xml")
        configuredMediaType.setPreferred(Boolean.TRUE)

        configuredMediaTypes.add(configuredMediaType)

        normalizer = new MediaTypeNormalizer(configuredMediaTypes)

        //Spock mocks don't cooperate with the httpServletRequest class
        request = mock(HttpServletRequest.class)
        director = new FilterDirectorImpl()
    }

    @Unroll("When normalizing variant extensions, it correctly #desc")
    def "correct behavior"(String desc, String uri, String url) {
        given:
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));

        MediaType identifiedMediaType = normalizer.getMediaTypeForVariant(request, director)

        expect:
        identifiedMediaType != null
        identifiedMediaType.getVariantExtension() == "xml"
        //These assertions require it to be XML, and that the urls are simplistic enough to only have one .xml in them
        director.getRequestUri() == uri.replace(".xml", "")
        director.getRequestUrl().toString() == url.replace(".xml", "")

        where:
        desc | uri | url
        "captures variant extensions"                | "/a/request/uri.xml"                            | "http://localhost/a/request/uri.xml"
        "ignores query parameters"                   | "/a/request/uri.xml?name=name&value=1"          | "http://localhost/a/request/uri.xml?name=name&value=1"
        "ignores URI fragments"                      | "/a/request/uri.xml#fragment"                   | "http://localhost/a/request/uri.xml#fragment"
        "ignores uri fragments and query parameters" | "/a/request/uri.xml?name=name&value=1#fragment" | "http://localhost/a/request/uri.xml?name=name&value=1#fragment"
        "captures unusual variants extensions"       | "/a/request/uri/.xml"                           | "http://localhost/a/request/uri/.xml"
    }
}
