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

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ConfigValidatorTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  describe("validating a config string") {
    it("should return normally if the config string is valid against the schema") {
      val config = """<?xml version="1.0" encoding="UTF-8"?>
                     |<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Type" new-name="rax-content-type" quality="0.9" splittable="true"/>
                     |    <header original-name="Content-Length" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      ConfigValidator("/schema-good.xsd").validateConfigString(config)
    }

    it("should throw an exception if the config string is NOT valid against the schema") {
      val config = """<?xml version="1.0" encoding="UTF-8"?>
                     |<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Length" new-name="rax-content-type" quality="0.9" splittable="true"/>
                     |    <header original-name="content-Length" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      val exception = intercept[SAXParseException] {
        ConfigValidator("/schema-good.xsd").validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }
  }

  describe("validating a config file") {
    it("should return normally if the config file is valid against the schema") {
      ConfigValidator("/schema-good.xsd").validateConfigFile("/config-good.xml")
    }

    it("should throw an exception if the config file is NOT valid against the schema due to an assertion") {
      val exception = intercept[SAXParseException] {
        ConfigValidator("/schema-good.xsd").validateConfigFile("/config-bad-assertion.xml")
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config file is NOT valid against the schema due to a type restriction") {
      val exception = intercept[SAXParseException] {
        ConfigValidator("/schema-good.xsd").validateConfigFile("/config-bad-type-restriction.xml")
      }
      exception.getLocalizedMessage should include ("Value '1.1' is not facet-valid with respect to maxInclusive '1.0E0' for type 'doubleBetweenZeroAndOne'")
    }
  }

  describe("bad schemas") {
    it("will not throw an exception if the schema has an invalid assertion") {
      ConfigValidator("/schema-bad.xsd").validateConfigFile("/config-bad-assertion.xml")
    }
  }
}
