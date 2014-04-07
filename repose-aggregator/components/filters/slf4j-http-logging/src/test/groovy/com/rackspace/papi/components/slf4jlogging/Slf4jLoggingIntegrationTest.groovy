package com.rackspace.papi.components.slf4jlogging

import com.mockrunner.mock.web.MockFilterChain
import com.mockrunner.mock.web.MockFilterConfig
import com.mockrunner.mock.web.MockHttpServletRequest
import com.mockrunner.mock.web.MockHttpServletResponse
import com.mockrunner.mock.web.MockServletContext
import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager
import com.rackspace.papi.commons.config.resource.ConfigurationResource
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLog
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLoggingConfig
import com.rackspace.papi.service.context.ServletContextHelper
import com.rackspace.papi.spring.SpringConfiguration
import groovy.xml.StreamingMarkupBuilder
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class Slf4jLoggingIntegrationTest extends Specification {

    def buildFakeConfigXml(List<Slf4JHttpLog> logEntries) {
        def xml = new StreamingMarkupBuilder().bind() {
            mkp.xmlDeclaration()
            "slf4j-http-logging"(
                    "xmlns": "http://docs.rackspacecloud.com/repose/slf4j-http-logging/v1.0"
            ) {
                logEntries.each { le ->
                    "slf4j-http-log"(
                            id: le.getId(),
                            format: le.getFormat()
                    )
                }
            }

        }
        return xml.toString()
    }

    Slf4JHttpLog logConfig(String id, String format) {
        def hl = new Slf4JHttpLog()
        hl.setFormat(format)
        hl.setId(id)

        hl
    }

    //TODO: look into refactoring this up a level for use by multiple filters
    Slf4jHttpLoggingFilter configureFilter(List<Slf4JHttpLog> logEntries) {
        Slf4jHttpLoggingFilter filter = new Slf4jHttpLoggingFilter()

        def mockServletContext = new MockServletContext()
        def mockFilterConfig = new MockFilterConfig()
        mockFilterConfig.setupServletContext(mockServletContext)

        ServletContextHelper.configureInstance(
                mockServletContext,
                new AnnotationConfigApplicationContext(SpringConfiguration.class)
        )

        def configService = ServletContextHelper.getInstance(mockFilterConfig.getServletContext()).getPowerApiContext().configurationService()

        //Decouple the coupled configs, since I can't replace it
        def mockResourceResolver = mock(ConfigurationResourceResolver.class)
        configService.setResourceResolver(mockResourceResolver)
        def cfgUpdateManager = mock(ConfigurationUpdateManager.class)
        configService.setUpdateManager(cfgUpdateManager)

        def configString = buildFakeConfigXml(logEntries)

        //Create a config resource for the ContentNormalizationConfig
        def configResource = new ConfigurationResource<Slf4JHttpLoggingConfig>() {

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
                return "Slf4JHttpLoggingConfig"
            }

            @Override
            InputStream newInputStream() throws IOException {
                //Return the string of XML that we built earlier
                return new ByteArrayInputStream(configString.getBytes())
            }
        }

        //When someone asks for the config for this filter, give them the xml string we built
        when(mockResourceResolver.resolve("slf4j-http-logging.cfg.xml")).thenReturn(configResource)

        //Initialize the filter now
        filter.init(mockFilterConfig)

        return filter
    }

    @Shared
    Slf4jHttpLoggingFilter filter

    def setupSpec() {
        filter = configureFilter([
                logConfig("myLogger", "Response Time=%T seconds\tResponse Time=%D microseconds\tResponse Code Modifiers=%200,201U\tModifier Negation=%!401a\tRemote IP=%a\tLocal IP=%A\tResponse Size(bytes)=%b\tRemote Host=%hRequest Line=&quot;%r&quot;\tRequest Method=%m\tRequest Protocol=%H\tServer Port=%p\tQuery String=%q\tTime Request Received=%t\tStatus=%s\tRemote User=%u\tURL Path Requested=%U\n")
        ])
    }

    def "The SLF4j logging thingy should work"(){
        given:
        MockFilterChain chain = new MockFilterChain()
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()

        //Will need to set up the request and response to verify the log line
        request.setRequestURI("http://www.example.com/derp/derp?herp=derp")
        request.setRequestURL("http://www.example.com/derp/derp?herp=derp")
        request.setMethod("GET")
        request.setRemoteHost("10.10.220.221")
        request.setLocalAddr("10.10.220.220")
        request.setServerPort(8080)

        response.setStatus(200,"OK")
        response.getWriter().print("HEY A BODY")

        //Set up a logger target to get those bits of information
        //This implementation is log4j dependent because we're verifying the backend
        def logger = Logger.getLogger("myLogger")
        def outputStream = new ByteArrayOutputStream()
        logger.addAppender(new WriterAppender(new SimpleLayout(), outputStream))


        when:
        filter.doFilter(request, response, chain)
        List<HttpServletRequest> requestList = chain.getRequestList();

        then:
        requestList.size() == 1

        def allLogs = new String(outputStream.toByteArray()).split("\n").toList()

        allLogs.size() == 1

        //TODO: verify that the log output matches what we expect
    }

}
