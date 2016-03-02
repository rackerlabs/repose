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
package org.openrepose.commons.config.parser.jaxb

import java.net.URL
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

/**
  * TODO: Unfortunately we can't actually test all the Unmarshalling because of classpath problems.
  * Fortunately an integration test catches some of the unmarshalling problem
  */
@RunWith(classOf[JUnitRunner])
class UnmarshallerValidatorValidationOnlyTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar with LazyLogging {

  val LIST_APPENDER_REF = "List0"

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender].clear()
  }

  val badNamespaceFiles = pathedFiles("unmarshallerValidator/badNamespace/")
  val correctNamespaceFiles = pathedFiles("unmarshallerValidator/correctNamespace/")
  //Since this test isn't doing any actual unmarshalling, we'll give it a fake JAXB context
  val mockContext = mock[JAXBContext]
  val uv = new UnmarshallerValidator(mockContext)
  var app: ListAppender = _

  /**
    * Get the URL of the schema for a given filter configuration filename.
    *
    * @param filterConfig the filename of the filter configuration to find the schema URL for
    */
  def getFilterSchemaUrl(filterConfig: String): URL =
    this.getClass.getResource("/unmarshallerValidator/xsd/" + filterConfig.replace(".cfg.xml", ".xsd"))

  /**
    * Construct myself a list of files to do work on!
    */
  def pathedFiles(path: String): List[String] = {
    Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream(path)).getLines().toList.map { file =>
      path + file
    }
  }

  import scala.collection.JavaConversions._

  /**
    * Do the actual validation
    *
    * @param configFile
    */
  def validate(configFile: String): Unit = {
    uv.setSchema(getSchemaForFile(configFile))

    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)

    val db = dbf.newDocumentBuilder
    //It's nice that java doesn't reference things the same way always :(
    uv.validate(db.parse(this.getClass.getResourceAsStream("/" + configFile)))
  }

  /**
    * Get a schema based on the configuration file name. Much easier to deal with
    *
    * @param configFile
    * @return
    */
  def getSchemaForFile(configFile: String): Schema = {
    val xsdURL = getFilterSchemaUrl(configFile.split("/").last)
    //Build the schema thingy
    val factory: SchemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
    factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    factory.newSchema(xsdURL)
  }

  describe("Validating an already correct namespace") {
    correctNamespaceFiles.foreach { configFile =>
      it(s"should not log the a message for $configFile") {
        validate(configFile)

        val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)
      }
    }
  }

  describe("Validating invalid configurations") {
    badNamespaceFiles.foreach { configFile =>
      it(s"should throw an exception for $configFile") {
        intercept[Exception] {
          validate(configFile)
        }
      }
    }
  }
}
