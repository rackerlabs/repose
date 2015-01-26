package org.openrepose.filters.slf4jlogging

import com.mockrunner.mock.web.MockFilterConfig
import com.mockrunner.mock.web.MockServletContext
import org.openrepose.commons.config.manager.ConfigurationUpdateManager
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.filters.slf4jlogging.config.FormatElement
import org.openrepose.filters.slf4jlogging.config.Slf4JHttpLog
import org.openrepose.core.services.context.ServletContextHelper
import org.openrepose.core.spring.SpringConfiguration
import groovy.xml.StreamingMarkupBuilder
import org.springframework.context.annotation.AnnotationConfigApplicationContext

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class Slf4jLoggingFilterTestUtil {

    def static buildFakeConfigXml(List<Slf4JHttpLog> logEntries) {
        def xml = new StreamingMarkupBuilder().bind() {
            mkp.xmlDeclaration()
            "slf4j-http-logging"(
                    "xmlns": "http://docs.openrepose.org/repose/slf4j-http-logging/v1.0"
            ) {
                logEntries.each { le ->
                    if (le.getFormat() != null) {
                        "slf4j-http-log"(
                                id: le.getId(),
                                format: le.getFormat()
                        )
                    } else {
                        "slf4j-http-log"(
                                id: le.getId()
                        ) {
                            //Using yieldUnescaped always wraps it in a CDATA tag, which matters for proving it works
                            if(le.formatElement.isCrush()) {
                                "format"(
                                        crush: le.formatElement.isCrush()
                                ) {
                                    mkp.yieldUnescaped le.getFormatElement().getValue()
                                }
                            } else {
                                "format" {
                                    mkp.yieldUnescaped le.getFormatElement().getValue()
                                }
                            }
                        }
                    }
                }
            }

        }
        return xml.toString()
    }

    static Slf4JHttpLog logConfig(String id, String format, boolean useElement = false, boolean replaceNewline = false) {
        def hl = new Slf4JHttpLog()
        if (useElement) {
            def formatElement = new FormatElement()
            formatElement.value = format
            if(replaceNewline) {
                formatElement.setCrush(replaceNewline)
            }
            hl.setFormatElement(formatElement)
        } else {
            hl.setFormat(format)
        }
        hl.setId(id)

        hl
    }

    static Slf4jHttpLoggingFilter configureFilter(List<Slf4JHttpLog> logEntries) {
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
}
