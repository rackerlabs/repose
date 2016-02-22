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


  val expectedLogMessage = "DEPRECATION WARNING: One of your config files contains an old namespace"

  val LIST_APPENDER_REF = "List0"
  val oldXmlFiles = pathedFiles("unmarshallerValidator/oldXmlConfigs/")

  before {
    val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
    val cfg = ctx.getConfiguration
    app = cfg.getAppender(LIST_APPENDER_REF).asInstanceOf[ListAppender].clear()
  }

  val badNamespaceFiles = pathedFiles("unmarshallerValidator/badNamespace/")
  val correctNamespaceFiles = pathedFiles("unmarshallerValidator/correctNamespace/")
  //oldXmlFiles contains the largest list of the files, I should probably combine them.
  val xsdUrlMap: Map[String, URL] = oldXmlFiles.map { file =>
    val fileName = file.split("/").last

    val xsdSchemaName = fileName.replace(".cfg.xml", ".xsd")

    //Return a map of the config file to the schema location
    fileName -> this.getClass.getResource(s"/unmarshallerValidator/xsd/$xsdSchemaName")
  }.toMap
  //Since this test isn't doing any actual unmarshalling, we'll give it a fake JAXB context
  val mockContext = mock[JAXBContext]
  val uv = new UnmarshallerValidator(mockContext)
  var app: ListAppender = _

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
    val xsdURL = xsdUrlMap(configFile.split("/").last)
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
        events.count(_.contains(expectedLogMessage)) shouldBe 0

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
