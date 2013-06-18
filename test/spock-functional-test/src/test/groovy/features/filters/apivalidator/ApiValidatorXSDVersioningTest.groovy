package features.filters.apivalidator

import spock.lang.Specification

import org.xml.sax.SAXException

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

class ApiValidatorXSDVersioningTest extends Specification {
    private Validator validator

    def setup() {
        // TODO fix path
        StreamSource schemaSource = new StreamSource(getClass().getResourceAsStream("/META-INF/schema/config/validator-configuration.xsd"))
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        Schema schema = schemaFactory.newSchema(schemaSource)
        validator = schema.newValidator()
    }

    def "should default to version 1 api-validator config and validate against validator-configuration.xsd"() {
        given:
        String xml =
            """<validators  xmlns="http://openrepose.org/repose/validator/v1.0" multi-role-match="true">
               <validator
                  role="default"
                  default="true"
                  wadl="file://my/wadl/file.wadl"
                  dot-output="/tmp/default.dot"
                  check-well-formed="false"
                  check-xsd-grammar="true"
                  check-elements="true"
                  check-plain-params="true"
                  do-xsd-grammar-transform="true"
                  enable-pre-process-extension="true"
                  remove-dups="true"
                  xpath-version="2"
                  xsl-engine="XalanC"
                  use-saxon="false"
                  enable-ignore-xsd-extension="false"
                  join-xpath-checks="false"
                  validator-name="testName"
                  check-headers="true"/>
            </validators>"""

        when:
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())))

        then:
        notThrown(SAXException)
    }

    def "version 1 api-validator config should validate against validator-configuration.xsd"() {
        given:
        String xml =
            """<validators  xmlns="http://openrepose.org/repose/validator/v1.0" multi-role-match="true" version="1">
               <validator
                  role="default"
                  default="true"
                  wadl="file://my/wadl/file.wadl"
                  dot-output="/tmp/default.dot"
                  check-well-formed="false"
                  check-xsd-grammar="true"
                  check-elements="true"
                  check-plain-params="true"
                  do-xsd-grammar-transform="true"
                  enable-pre-process-extension="true"
                  remove-dups="true"
                  xpath-version="2"
                  xsl-engine="XalanC"
                  use-saxon="false"
                  enable-ignore-xsd-extension="false"
                  join-xpath-checks="false"
                  validator-name="testName"
                  check-headers="true"/>
            </validators>"""

        when:
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())))

        then:
        notThrown(SAXException)
    }

    def "version 2 api-validator config should validate against validator-configuration.xsd"() {
        given:
        String xml =
            """<validators  xmlns="http://openrepose.org/repose/validator/v1.0" multi-role-match="true" version="2">
               <validator
                  role="default"
                  default="true"
                  wadl="file://my/wadl/file.wadl"
                  dot-output="/tmp/default.dot"
                  check-well-formed="false"
                  check-xsd-grammar="true"
                  check-elements="true"
                  check-plain-params="true"
                  do-xsd-grammar-transform="true"
                  enable-pre-process-extension="true"
                  remove-dups="true"
                  xpath-version="2"
                  xsl-engine="XalanC"
                  xsd-engine="Xerces"
                  enable-ignore-xsd-extension="false"
                  join-xpath-checks="false"
                  validator-name="testName"
                  check-headers="true"/>
            </validators>"""

        when:
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())))

        then:
        notThrown(SAXException)
    }
}
