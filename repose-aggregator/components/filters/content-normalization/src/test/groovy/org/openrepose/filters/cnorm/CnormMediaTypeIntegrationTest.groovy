package org.openrepose.filters.cnorm

import com.mockrunner.mock.web.*
import groovy.xml.StreamingMarkupBuilder
import org.openrepose.commons.config.manager.ConfigurationUpdateManager
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.core.spring.SpringConfiguration
import org.openrepose.filters.cnorm.config.MediaType
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Created by dkowis on 4/4/14.
 */
class CnormMediaTypeIntegrationTest extends Specification {

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

    /**
     * Create an XML config that we can load in magically later
     */
    def buildFakeConfigXml(List<MediaType> mediaTypes) {
        def xml = new StreamingMarkupBuilder().bind() {
            mkp.xmlDeclaration()
            "content-normalization"(
                    "xmlns:xsi": 'http://www.w3.org/2001/XMLSchema-instance',
                    "xmlns": 'http://docs.openrepose.org/repose/content-normalization/v1.0',
                    "xsi:schemaLocation": 'http://docs.openrepose.org/repose/content-normalization/v1.0 ../config/normalization-configuration.xsd'
            ) {
                "media-types" {
                    mediaTypes.each { mt ->
                        "media-type"(name: mt.name, preferred: mt.preferred, "variant-extension": mt.variantExtension)
                    }
                }
            }
        }

        return xml.toString()
    }

    /**
     * Do all the stuff that we need to do to create/mock/prepare the ContentNormalizationFilter for the MockFilterChain
     * This will let me use the MockFilterChain to see the resultant request/response pairs through the entire filter chain
     */
    ContentNormalizationFilter configureContentNormalizationFilter(List<MediaType> mediaTypes) {
        ContentNormalizationFilter filter = new ContentNormalizationFilter()
        def mockServletContext = new MockServletContext()

        def mockFilterConfig = new MockFilterConfig()
        mockFilterConfig.setupServletContext(mockServletContext)

        ServletContextHelper.configureInstance(
                mockServletContext,
                new AnnotationConfigApplicationContext(SpringConfiguration.class)
        )

        //Get ahold of the configuration service, and inject a couple things.
        def configService = ServletContextHelper.getInstance(mockFilterConfig.getServletContext()).getPowerApiContext().configurationService()

        //Decouple the coupled configs, since I can't replace it
        def mockResourceResolver = mock(ConfigurationResourceResolver.class)
        configService.setResourceResolver(mockResourceResolver)
        def cfgUpdateManager = mock(ConfigurationUpdateManager.class)
        configService.setUpdateManager(cfgUpdateManager)

        def configString = buildFakeConfigXml(mediaTypes)

        //Create a config resource for the ContentNormalizationConfig
        def configResource = new ConfigurationResource() {

            @Override
            boolean updated() throws IOException {
                return true
            }

            @Override
            boolean exists() throws IOException {
                return true
            }

            @Override
            String name() {
                return "ContentNormalizationConfig"
            }

            @Override
            InputStream newInputStream() throws IOException {
                //Return the string of XML that we built earlier
                return new ByteArrayInputStream(configString.getBytes())
            }
        }
        //When someone asks for the config for this filter, give them the xml string we built
        when(mockResourceResolver.resolve("content-normalization.cfg.xml")).thenReturn(configResource)

        //Initialize the filter now
        filter.init(mockFilterConfig)

        return filter
    }

    @Shared
    ContentNormalizationFilter filter

    def setupSpec() {
        filter = configureContentNormalizationFilter([
                preferred("application/json"),
                mediaType("application/xml"),
                mediaType("application/other")
        ])
    }

    @Unroll("Additional cases when given #sendAcceptHeaders you get out #acceptHeaders")
    def "Covering cases from the integration test"() {
        given:
        MockFilterChain chain = new MockFilterChain()
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()

        //Set up the incoming request
        request.setRequestURI("http://www.example.com/derp/derp")
        if (sendAcceptHeaders != null) {
            sendAcceptHeaders.split(",").each {
                request.addHeader("accept", it)
            }
        }

        when:
        filter.doFilter(request, response, chain)
        List<HttpServletRequest> requestList = chain.getRequestList()

        then:
        //Need to operate on the filterList that the mockFilterChain has, it should only have one, and that one should be right
        requestList.size() == 1
        def firstRequest = requestList.first()
        firstRequest.getHeader(CommonHttpHeader.ACCEPT.toString()) == acceptHeaders

        where:
        sendAcceptHeaders                                    | acceptHeaders
        'application/xml'                                    | 'application/xml'
        'application/xml,application/json'                   | 'application/json'
        'application/other'                                  | 'application/other'
        'application/other,application/xml'                  | 'application/xml'
        'application/xml,application/other'                  | 'application/xml'
        'application/xml,application/other,application/json' | 'application/json'
        'html/text,application/xml'                          | 'application/xml'
        'application/xml,html/text'                          | 'application/xml'
        'application/xml,html/text,application/json'         | 'application/json'
        '*/*,application/json'                               | 'application/json'
        '*/*'                                                | 'application/json'
        null                                                 | 'application/json'
        'application/json;q=1,application/xml;q=0.5'         | 'application/json'
        'application/xml;q=1,application/json;q=0.5'         | 'application/json'
        'application/xml;q=1'                                | 'application/xml'
        '*/json'                                             | 'application/json'
        '*/other'                                            | 'application/json'

    }

}
