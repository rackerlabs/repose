package org.openrepose.core.filter

import groovy.xml.StreamingMarkupBuilder
import org.xml.sax.SAXParseException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

/**
 * This class tests the various asserts within the system model.
 */
class SystemModelConfigTest extends Specification {
    @Shared
    Schema schema

    Validator validator
    StreamingMarkupBuilder xmlBuilder

    def setupSpec() {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
        schema = factory.newSchema(new StreamSource(SystemModelConfigTest.class.getResourceAsStream("/META-INF/schema/system-model/system-model.xsd")))
    }

    def setup() {
        validator = schema.newValidator()
        xmlBuilder = new StreamingMarkupBuilder()
    }

    def "Validate Example Config"() {
        given:
        final StreamSource sampleSource = new StreamSource(SystemModelConfigTest.class.getResourceAsStream("/META-INF/schema/examples/system-model.cfg.xml"))

        expect:
        validator.validate(sampleSource)
    }

    @Unroll("Validate System-Model with only one Endpoint Destination that has default attribute set to #endpointOneDefault")
    def "Validate System-Model with only one Endpoint Destination"() {
        given:
        String xml = createXml(endpointOneDefault)

        expect:
        validator.validate(new StreamSource(new StringReader(xml)))

        where:
        endpointOneDefault << [null, true, false]
    }

    @Unroll("InValidate System-Model with Endpoint Destinations default1=#endpointOneDefault, default2=#endpointTwoDefault, and default3=#endpointThreeDefault")
    def "InValidate System-Model with three Endpoint Destinations."() {
        given:
        def xml = createXml(endpointOneDefault, endpointTwoDefault, endpointThreeDefault)

        when:
        validator.validate(new StreamSource(new StringReader(xml)))

        then:
        def caught = thrown(SAXParseException)
        caught.getLocalizedMessage().contains('There should only be one default destination')

        where:
        endpointOneDefault | endpointTwoDefault | endpointThreeDefault
        null               | null               | null
        null               | null               | false
        null               | false              | false
        true               | true               | true
        null               | true               | true
        null               | false              | null
        true               | null               | true
        true               | true               | null
        true               | true               | false
        true               | false              | true
        false              | null               | null
        false              | null               | false
        false              | true               | true
        false              | false              | null
    }

    @Unroll("Validate System-Model with Endpoint Destinations default1=#endpointOneDefault, default2=#endpointTwoDefault, and default3=#endpointThreeDefault.")
    def "Validate System-Model with three Endpoint Destinations."() {
        given:
        def xml = createXml(endpointOneDefault, endpointTwoDefault, endpointThreeDefault)

        expect:
        validator.validate(new StreamSource(new StringReader(xml)))

        where:
        endpointOneDefault | endpointTwoDefault | endpointThreeDefault
        null               | null               | true
        null               | true               | null
        null               | true               | false
        null               | false              | true
        true               | null               | null
        true               | null               | false
        true               | false              | null
        true               | false              | false
        false              | null               | true
        false              | true               | null
        false              | true               | false
        false              | false              | true
    }

    private String createXml(Boolean... endpointDefaults) {
        if (endpointDefaults == null) {
            endpointDefaults = [null];
        }
        return xmlBuilder.bind {
            'system-model'('xmlns': 'http://docs.openrepose.org/repose/system-model/v2.0') {
                'repose-cluster'('id': 'repose') {
                    'nodes'() {
                        'node'('id': 'node1', 'hostname': 'localhost', 'http-port': '8080')
                    }
                    'services'() {
                        'service'('name': 'dist-datastore')
                    }
                    'destinations'() {
                        int counter = 0;
                        for (Boolean endpointDefault: endpointDefaults) {
                            if (endpointDefault == null) {
                                'endpoint'('id': 'openrepose' + counter++, 'protocol': 'http', 'hostname': '192.168.1.1', 'root-path': '/', 'port': '8080')
                            } else {
                                'endpoint'('id': 'openrepose' + counter++, 'protocol': 'http', 'hostname': '192.168.1.1', 'root-path': '/', 'port': '8080', 'default': endpointDefault.toString())
                            }
                        }
                    }
                }
            }
        }.toString()
    }
}
