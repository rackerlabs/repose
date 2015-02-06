package org.openrepose.commons.config.parser.jaxb

import java.net.URL
import java.util.{Calendar, Date, GregorianCalendar}
import javax.xml.bind.JAXBContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.validation.{Schema, SchemaFactory}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class UnmarshallerValidatorValidationOnlyTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {

  val LIST_APPENDER_REF = "List0"
  var app: ListAppender = _

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender].clear()
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // This is a Time-Bomb to remind us to remove the backwards compatible hack.       //
  new Date() should be < new GregorianCalendar(2015, Calendar.SEPTEMBER, 1).getTime()
  //
  /////////////////////////////////////////////////////////////////////////////////////

  val filesList: List[String] = Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream("/unmarshallerValidator/oldXmlConfigs/")).getLines().toList

  //I copied all the XSDs into a folder, since they won't be available on this class path :(
  // TODO: maybe this test should live somewhere else, possibly as an "integration" test, but much simpler.
  val xsdUrlMap:Map[String, URL] = filesList.map { file =>
    val xsdSchemaName = file.replace(".cfg.xml", ".xsd")

    //Return a map of the config file to the schema location
    file -> this.getClass.getResource(s"/unmarshallerValidator/xsd/$xsdSchemaName")
  }.toMap

  //Since this test isn't doing any actual unmarshalling, we'll give it a fake JAXB context
  val mockContext = mock[JAXBContext]
  val uv = new UnmarshallerValidator(mockContext)

  describe("Validating oldXmlConfigs") {
    filesList.foreach { configFile =>
      it(s"validates the old configuration for $configFile") {
        val xsdURL = xsdUrlMap(configFile)
        //Build the schema thingy
        val factory: SchemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
        val schema: Schema = factory.newSchema(xsdURL)

        uv.setSchema(schema)

        val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
        dbf.setNamespaceAware(true)

        val db = dbf.newDocumentBuilder

        uv.validate(db.parse(this.getClass.getResourceAsStream(s"/unmarshallerValidator/oldXmlConfigs/$configFile")))
      }
    }
  }



  describe(s"The UnmarshallerValidator") {

    val oldNamespace = "OOPS"

    describe(s"with a config containing the deprecated namespace $oldNamespace") {
      val configData =
        s"""<?xml version="1.0" encoding="UTF-8"?>
           |<unmarshaller-test
           |     xmlns="$oldNamespace/unmarshaller-test/v0.0"
                                       |     unmarshaller-test-attribute="This is the attribute value."/>
         """.stripMargin

      it("should update the config namespace to http://docs.openrepose.org/repose/.") {
        pending
        //          /////////////////////////////////////////////////////////////////////////////////////
        //          // This is a Time-Bomb to remind us to remove the backwards compatible hack.       //
        //          new Date() should be < new GregorianCalendar(2015, Calendar.SEPTEMBER, 1).getTime() //
        //          /////////////////////////////////////////////////////////////////////////////////////
        //          val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
        //          factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
        //          val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
        //          val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
        //          val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
        //          unmarshallerValidator.setSchema(schema)
        //          unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
        //          val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
        //          events.count(_.contains(s"Contains old namespace  - $oldNamespace")) shouldBe 1
      }
    }
  }

  it("should not log the a message if the namespace is already correct.") {
    pending
    //      val configData =
    //        s"""<?xml version="1.0" encoding="UTF-8"?>
    //           |<unmarshaller-test
    //           |        xmlns="http://docs.openrepose.org/repose/unmarshaller-test/v0.0"
    //           |        unmarshaller-test-attribute="This is the attribute value."/>
    //       """.stripMargin
    //
    //      val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
    //      factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    //      val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
    //      val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
    //      val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
    //      unmarshallerValidator.setSchema(schema)
    //      unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
    //      val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
    //      events.count(_.contains(s"Contains old namespace  - ")) shouldBe 0
  }

  it("should throw an exception when the config namespace is bad.") {
    pending
    //      val configData =
    //        s"""<?xml version="1.0" encoding="UTF-8"?>
    //         |<unmarshaller-test
    //         |        xmlns="http://test.something.bad/repose/unmarshaller-test/v0.0"
    //         |        unmarshaller-test-attribute="This is the attribute value."/>
    //       """.stripMargin
    //
    //      val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
    //      factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    //      val schema = factory.newSchema(new StreamSource(new ByteArrayInputStream(schemaData.getBytes())))
    //      val jaxbContext = JAXBContext.newInstance(classOf[UnmarshallerValidatorTestImpl].getPackage.getName)
    //      val unmarshallerValidator = new UnmarshallerValidator(jaxbContext)
    //      unmarshallerValidator.setSchema(schema)
    //      intercept[SAXParseException] {
    //        unmarshallerValidator.validateUnmarshal(new ByteArrayInputStream(configData.getBytes))
    //      }
  }
}
