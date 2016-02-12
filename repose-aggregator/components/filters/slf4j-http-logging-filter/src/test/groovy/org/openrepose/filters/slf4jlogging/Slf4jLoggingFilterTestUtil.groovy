/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.slf4jlogging

import com.mockrunner.mock.web.MockFilterConfig
import groovy.xml.StreamingMarkupBuilder
import org.openrepose.commons.config.manager.ConfigurationUpdateManager
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.filters.slf4jlogging.config.FormatElement
import org.openrepose.filters.slf4jlogging.config.Slf4JHttpLog

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
                            if (le.formatElement.isCrush()) {
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
            if (replaceNewline) {
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

        def mockFilterConfig = new MockFilterConfig()

        def configService = null //TODO: this needs to be a mock configService

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
