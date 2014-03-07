package com.rackspace.papi.components.uristripper

import com.rackspace.papi.commons.util.http.CommonHttpHeader
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.components.uristripper.config.UriStripperConfig
import com.rackspace.papi.filter.logic.HeaderManager
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest


class NewUriStripperHandlerFactoryTest extends Specification {


    static def conf
    static def factory

    def setupSpec() {
        conf = new UriStripperConfig();
        factory = new UriStripperHandlerFactory()
    }

    @Unroll("Stripping #value from #resourcePath at index #index")
    def "properly strips tokens from the sent URI"() {
        given:
        conf.setRewriteLocation(false)
        conf.setTokenIndex(index)

        factory.configurationUpdated(conf)

        def stripper = factory.buildHandler()
        def mockRequest = Mock(HttpServletRequest) {
            1 * getRequestURI() >> resourcePath
        }
        def mockResponse = Mock(ReadableHttpServletResponse)


        expect:
        def filterDirector = stripper.handleRequest(mockRequest, mockResponse)
        filterDirector.requestUri == strippedPath
        stripper.token == value

        where:
        resourcePath           | index | value   | strippedPath
        "/v1/12345/woot/butts" | 1     | "12345" | "/v1/woot/butts"
        "/path/to/your/mom"    | 3     | "mom"   | "/path/to/your"
        "/lol/butts"           | 0     | "lol"   | "/butts"
    }

    @Unroll("injecting #value into #originalLocation to get #newLocation")
    def "properly modifies the Location header when told to"() {
        given:
        conf.setRewriteLocation(true)
        conf.setTokenIndex(index)

        factory.configurationUpdated(conf)

        def stripper = factory.buildHandler()

        def locationHeader = CommonHttpHeader.LOCATION.toString()
        def mockRequest = Mock(HttpServletRequest) {
            1 * getRequestURI() >> resourcePath
        }

        def mockResponse = Mock(ReadableHttpServletResponse) {
            getHeader(locationHeader) >> originalLocation
        }

        expect:
        stripper.handleRequest(mockRequest, mockResponse)
        def filterDirector = stripper.handleResponse(mockRequest, mockResponse)

        stripper.token == value
        def addingHeaders = filterDirector.responseHeaderManager().headersToAdd()
        addingHeaders.keySet().contains(locationHeader)
        def valueSet = addingHeaders.get(locationHeader)
        valueSet.size() == 1
        valueSet.first() == newLocation

        where:
        resourcePath                 | index | value   | originalLocation                                 | newLocation
        "/v1/12345/some/resource"    | 1     | "12345" | "http://example.com/v1/some/resource"            | "http://example.com/v1/12345/some/resource"
        "/your/mom/is/a/classy/lady" | 3     | "a"     | "http://example.com/your/mom/is/classy/lady"     | "http://example.com/your/mom/is/a/classy/lady"
        "/product/123/item/123"      | 1     | "123"   | "http://example.com/product/item/123"            | "http://example.com/product/123/item/123"
        "/v1/12345/path/to/resource" | 1     | "12345" | "http://service.com/v1/path/to/resource?a=b&c=d" | "http://service.com/v1/12345/path/to/resource?a=b&c=d"
        "/v1/12345/path/to/resource" | 1     | "12345" | "/v1/path/to/resource?a=b&c=d"                   | "/v1/12345/path/to/resource?a=b&c=d"
    }

    def "doesn't touch location header when it's invalid"() {
        conf.setRewriteLocation(true)
        conf.setTokenIndex(index)

        factory.configurationUpdated(conf)

        def stripper = factory.buildHandler()

        def locationHeader = CommonHttpHeader.LOCATION.toString()
        def mockRequest = Mock(HttpServletRequest) {
            1 * getRequestURI() >> resourcePath
        }

        def mockResponse = Mock(ReadableHttpServletResponse) {
            getHeader(locationHeader) >> originalLocation
        }

        expect:
        stripper.handleRequest(mockRequest, mockResponse)
        def filterDirector = stripper.handleResponse(mockRequest, mockResponse)

        stripper.token == value
        def addingHeaders = filterDirector.responseHeaderManager().headersToAdd()

        !addingHeaders.keySet().contains(locationHeader)

        where:
        resourcePath              | index | value   | originalLocation
        "/v1/12345/some/resource" | 1     | "12345" | "http://example.com/v1/some(\\/resource"
    }
}
