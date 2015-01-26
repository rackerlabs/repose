package org.openrepose.filters.slf4jlogging

import org.intellij.lang.annotations.Language
import org.openrepose.commons.config.parser.ConfigurationParserFactory
import org.openrepose.commons.config.resource.impl.ByteArrayConfigurationResource
import org.openrepose.filters.slf4jlogging.config.Slf4JHttpLoggingConfig
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AdditionalXSDFormatTest extends Specification {
    /**
     * This is needed to resolve XML parsing conflicts with log4j2 and stuff. Since the config marshalling calls a log
     * method, I need to have this so it doesn't make annoying ugly meaningless error messages.
     * @return
     */
    def setupSpec() {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    Slf4JHttpLoggingConfig marshalConfig(String xml) {
        URL xsdURL = getClass().getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd");
        def parser = ConfigurationParserFactory.getXmlConfigurationParser(
                Slf4JHttpLoggingConfig.class,
                xsdURL)
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
    <slf4j-http-log id="my-special-log" >
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
    <slf4j-http-log id="my-special-log" >
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
    <slf4j-http-log id="my-special-log" >
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
    <slf4j-http-log id="my-special-log" >
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
