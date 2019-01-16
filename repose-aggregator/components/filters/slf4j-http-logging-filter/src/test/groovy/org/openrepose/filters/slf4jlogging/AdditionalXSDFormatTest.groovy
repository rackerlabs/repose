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

import org.intellij.lang.annotations.Language
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import org.openrepose.commons.config.resource.impl.ByteArrayConfigurationResource
import org.openrepose.filters.slf4jlogging.config.Slf4JHttpLoggingConfig
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AdditionalXSDFormatTest extends Specification {

    Slf4JHttpLoggingConfig marshalConfig(String xml) {
        URL xsdURL = getClass().getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd");
        def parser = new JaxbConfigurationParser(
                Slf4JHttpLoggingConfig.class,
                xsdURL,
                this.getClass().getClassLoader())
        def configResource = new ByteArrayConfigurationResource("slf4jConfig", xml.getBytes(StandardCharsets.UTF_8))
        parser.read(configResource)
    }


    def "original format line validates"() {
        given: "I have an http log with a format attribute"
        @Language("XML") def configXml = """<?xml version="1.0" encoding="UTF-8"?>
<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log
            id="my-special-log"
            format="Some kind of format "
            />
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should be a validated config"
        obj != null

    }

    def "new format element validates"() {
        given: "I have an http log with a format element (and no format attribute)"
        @Language("XML")
        def configXml = """<?xml version="1.0" encoding="UTF-8"?>

<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log id="my-special-log">
            <format>
            [<![CDATA[
            Some Output format!
            ]]>
            </format>
    </slf4j-http-log>
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should be a validated config"
        obj != null

    }

    def "both format elements do not validate"() {
        given: "I have a invalid config including both the format attribute and element)"
        @Language("XML")
        def configXml = """<?xml version="1.0" encoding="UTF-8"?>

<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log id="my-special-log" format="derpline">
    <format>
    <![CDATA[
        DERPLINETWO
    ]]>
    </format>
    </slf4j-http-log>
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should not be a valid config"
        thrown(ClassCastException)
        obj == null

    }


    def "missing both of the format items does not validate"() {
        given: "I have a invalid config missing both the format attribute and element)"
        @Language("XML")
        def configXml = """<?xml version="1.0" encoding="UTF-8"?>

<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log id="my-special-log">
    </slf4j-http-log>
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should not be a valid config"
        thrown(ClassCastException)
        obj == null
    }

    def "multiple loggers, one of each validates"() {
        given: "I have an http log config one with a format element and the other with a format attribute"
        @Language("XML")
        def configXml = """<?xml version="1.0" encoding="UTF-8"?>

<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log id="my-special-log">
            <format>
            [<![CDATA[
            Some Output format!
            ]]>
            </format>
    </slf4j-http-log>
    <slf4j-http-log id="otherLog" format="Some magical format line omg"/>
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should be a validated config"
        obj != null

    }

    def "new multi-line format element validates"() {
        given: "I have an http log with a format element (and no format attribute)"
        @Language("XML")
        def configXml = """<?xml version="1.0" encoding="UTF-8"?>
<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
    <slf4j-http-log id="my-special-log">
            <format crush="true">
            [<![CDATA[
            Some Output format
            with multiple lines!
            ]]>
            </format>
    </slf4j-http-log>
</slf4j-http-logging>
"""

        when: "I get the jaxb config for this"
        def obj = marshalConfig(configXml)

        then: "it should be a validated config"
        obj != null
    }
}
