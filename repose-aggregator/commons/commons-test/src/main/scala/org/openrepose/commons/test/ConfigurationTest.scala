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
package org.openrepose.commons.test

import java.net.URL
import javax.xml.bind.JAXBContext

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec, Matchers}

import scala.language.implicitConversions

/**
  * An abstract class which provides a standard set of tests on a provided configuration resource.
  *
  * Note that this class leverages [[BeforeAndAfterAll]] to register standard tests.
  * Therefore, any tests which extend this test should either not override the beforeAll() method,
  * or should call through to the beforeAll() method defined here (i.e., call super.beforeAll()).
  */
abstract class ConfigurationTest extends FunSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  /**
    * The XML schema to validate the example configuration against.
    */
  val schema: URL

  /**
    * Any auxiliary schemas necessary for validation.
    */
  val auxiliarySchemas: Seq[URL] = Seq.empty

  /**
    * The example configuration to be validated and unmarshalled.
    */
  val exampleConfig: URL

  /**
    * The package containing the JAXB generated classes.
    */
  val jaxbContextPath: String

  /**
    * The [[ConfigValidator]] used to validate the example configuration against the schema.
    */
  var validator: ConfigValidator = _

  /**
    * Overriding the value of this flag ensures that the tests defined in this class are always registered,
    * even if there are no other tests defined.
    */
  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  override def beforeAll(): Unit = {
    super.beforeAll()

    describe("unmarshalling") {
      it("should successfully unmarshal the sample config") {
        JAXBContext.newInstance(jaxbContextPath).createUnmarshaller().unmarshal(exampleConfig)
      }
    }

    describe("schema validation") {
      it("should successfully validate the sample config") {
        validator.validateConfig(exampleConfig.openStream())
      }
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    validator = ConfigValidator(auxiliarySchemas :+ schema: _*)
  }
}
