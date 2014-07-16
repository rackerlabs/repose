package com.rackspace.papi.components.slf4jlogging

import com.mockrunner.mock.web.MockFilterConfig
import com.mockrunner.mock.web.MockServletContext
import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager
import com.rackspace.papi.commons.config.resource.ConfigurationResource
import com.rackspace.papi.service.config.ConfigurationResourceResolver
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLog
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLoggingConfig
import com.rackspace.papi.service.context.ServletContextHelper
import com.rackspace.papi.spring.SpringConfiguration
import groovy.xml.StreamingMarkupBuilder
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout
import org.apache.log4j.WriterAppender
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import spock.lang.Specification

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when


class Slf4jLoggingFilterSpecification extends Specification{

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

    OutputStream prepLoggerOutputStream(String name) {
        def logger = Logger.getLogger(name)
        def outputStream = new ByteArrayOutputStream()
        logger.addAppender(new WriterAppender(new SimpleLayout(), outputStream))

        outputStream
    }

    def logLines(OutputStream os) {
        new String(os.toByteArray()).split("\n").toList()
    }
}
