package org.openrepose.commons.config.parser.jaxb

import java.io.ByteArrayInputStream
import java.util.{Calendar, Date, GregorianCalendar}
import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.openrepose.commons.config.parser.jaxb.test.UnmarshallerValidatorTestImpl
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.xml.sax.SAXParseException

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class UnmarshallerValidatorTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {

  val LIST_APPENDER_REF = "List0"
  val oldNamespaces = List(
    "http://docs.api.rackspacecloud.com/repose",
    "http://docs.rackspacecloud.com/repose",
    "http://openrepose.org/repose",
    "http://openrepose.org/components"
  )
  var app: ListAppender = _

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender].clear()
  }

  describe(s"The UnmarshallerValidator") {
    val schemaData =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
         |           xmlns="http://docs.openrepose.org/repose/unmarshaller-test/v0.0"
         |           targetNamespace="http://docs.openrepose.org/repose/unmarshaller-test/v0.0"
         |           elementFormDefault="qualified"
         |           attributeFormDefault="unqualified">
         |    <xs:element name="unmarshaller-test" type="UnmarshallerValidatorTestImpl"/>
         |    <xs:complexType name="UnmarshallerValidatorTestImpl">
         |        <xs:attribute name="unmarshaller-test-attribute" type="xs:string" use="required"/>
         |    </xs:complexType>
         |</xs:schema>
         """.stripMargin

    oldNamespaces.foreach { oldNamespace =>
      describe(s"with a config containing the deprecated namespace $oldNamespace") {
        val configData =
          s"""<?xml version="1.0" encoding="UTF-8"?>
           |<unmarshaller-test
           |        xmlns="$oldNamespace/unmarshaller-test/v0.0"
           |        unmarshaller-test-attribute="This is the attribute value."/>
         """.stripMargin

        it("should update the config namespace to http://docs.openrepose.org/repose/.") {
          /////////////////////////////////////////////////////////////////////////////////////
          // This is a Time-Bomb to remind us to remove the backwards compatible hack.       //
          new Date() should be < new GregorianCalendar(2015, Calendar.SEPTEMBER, 1).getTime()//
          /////////////////////////////////////////////////////////////////////////////////////
          val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
          factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
          val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
          val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
          val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
          unmarshallerValidator.setSchema(schema)
          unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
          val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
          events.count(_.contains(s"Contains old namespace  - $oldNamespace")) shouldBe 1
        }
      }
    }

    it("should not log the a message if the namespace is already correct.") {
      val configData =
        s"""<?xml version="1.0" encoding="UTF-8"?>
           |<unmarshaller-test
           |        xmlns="http://docs.openrepose.org/repose/unmarshaller-test/v0.0"
           |        unmarshaller-test-attribute="This is the attribute value."/>
       """.stripMargin

      val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
      factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
      val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
      val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
      val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
      unmarshallerValidator.setSchema(schema)
      unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
      val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains(s"Contains old namespace  - ")) shouldBe 0
    }

    it("should throw an exception when the config namespace is bad.") {
      val configData =
        s"""<?xml version="1.0" encoding="UTF-8"?>
         |<unmarshaller-test
         |        xmlns="http://test.something.bad/repose/unmarshaller-test/v0.0"
         |        unmarshaller-test-attribute="This is the attribute value."/>
       """.stripMargin

      val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
      factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
      val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
      val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
      val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
      unmarshallerValidator.setSchema(schema)
      intercept[SAXParseException] {
        unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
      }
    }
  }
}
