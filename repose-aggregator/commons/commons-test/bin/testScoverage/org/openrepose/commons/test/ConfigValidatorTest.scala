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

import java.io.InputStream
import java.net.URL

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ConfigValidatorTest extends FunSpec with Matchers {
  import ConfigValidatorTest._

  describe("validating a config string") {
    it("should return normally if the config string is valid against the schema") {
      val config = """<?xml version="1.0" encoding="UTF-8"?>
                     |<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Type" new-name="rax-content-type" quality="0.9" splittable="true"/>
                     |    <header original-name="Content-Length" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      ConfigValidator(GoodSchemaResourceName).validateConfigString(config)
    }

    it("should return normally if the config string is valid against the schema URL") {
      val config = """<?xml version="1.0" encoding="UTF-8"?>
                     |<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Type" new-name="rax-content-type" quality="0.9" splittable="true"/>
                     |    <header original-name="Content-Length" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      ConfigValidator(GoodSchemaUrl).validateConfigString(config)
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
        ConfigValidator(GoodSchemaResourceName).validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config string is NOT valid against the schema URL") {
      val config = """<?xml version="1.0" encoding="UTF-8"?>
                     |<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Length" new-name="rax-content-type" quality="0.9" splittable="true"/>
                     |    <header original-name="content-Length" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaUrl).validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }
  }

  describe("validating a config file") {
    it("should return normally if the config file is valid against the schema") {
      ConfigValidator(GoodSchemaResourceName).validateConfigFile("/config-good.xml")
    }

    it("should return normally if the config file is valid against the schema URL") {
      ConfigValidator(GoodSchemaUrl).validateConfigFile("/config-good.xml")
    }

    it("should throw an exception if the config file is NOT valid against the schema due to an assertion") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaResourceName).validateConfigFile("/config-bad-assertion.xml")
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config file is NOT valid against the schema URL due to an assertion") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaUrl).validateConfigFile("/config-bad-assertion.xml")
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config file is NOT valid against the schema due to a type restriction") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaResourceName).validateConfigFile("/config-bad-type-restriction.xml")
      }
      exception.getLocalizedMessage should include ("Value '1.1' is not facet-valid with respect to maxInclusive '1.0E0' for type 'doubleBetweenZeroAndOne'")
    }

    it("should throw an exception if the config file is NOT valid against the schema URL due to a type restriction") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaUrl).validateConfigFile("/config-bad-type-restriction.xml")
      }
      exception.getLocalizedMessage should include ("Value '1.1' is not facet-valid with respect to maxInclusive '1.0E0' for type 'doubleBetweenZeroAndOne'")
    }
  }

  describe("validating a config source") {
    it("should return normally if the config file is valid against the schema") {
      ConfigValidator(GoodSchemaResourceName).validateConfig(stringResourceToInputStream("/config-good.xml"))
    }

    it("should return normally if the config file is valid against the schema URL") {
      ConfigValidator(GoodSchemaUrl).validateConfig(stringResourceToInputStream("/config-good.xml"))
    }

    it("should throw an exception if the config file is NOT valid against the schema due to an assertion") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaResourceName).validateConfig(stringResourceToInputStream("/config-bad-assertion.xml"))
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config file is NOT valid against the schema URL due to an assertion") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaUrl).validateConfig(stringResourceToInputStream("/config-bad-assertion.xml"))
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should throw an exception if the config file is NOT valid against the schema due to a type restriction") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaResourceName).validateConfig(stringResourceToInputStream("/config-bad-type-restriction.xml"))
      }
      exception.getLocalizedMessage should include ("Value '1.1' is not facet-valid with respect to maxInclusive '1.0E0' for type 'doubleBetweenZeroAndOne'")
    }

    it("should throw an exception if the config file is NOT valid against the schema URL due to a type restriction") {
      val exception = intercept[SAXParseException] {
        ConfigValidator(GoodSchemaUrl).validateConfig(stringResourceToInputStream("/config-bad-type-restriction.xml"))
      }
      exception.getLocalizedMessage should include ("Value '1.1' is not facet-valid with respect to maxInclusive '1.0E0' for type 'doubleBetweenZeroAndOne'")
    }
  }

  describe("bad schemas") {
    it("will not throw an exception if the schema has an invalid assertion") {
      ConfigValidator("/schema-bad.xsd").validateConfigFile("/config-bad-assertion.xml")
    }

    it("will not throw an exception if the schema source has an invalid assertion") {
      ConfigValidator(classOf[ConfigValidatorTest].getResource("/schema-bad.xsd")).validateConfigFile("/config-bad-assertion.xml")
    }
  }
}

object ConfigValidatorTest {
  final val GoodSchemaResourceName: String = "/schema-good.xsd"
  final val GoodSchemaUrl: URL = classOf[ConfigValidatorTest].getResource(GoodSchemaResourceName)

  def stringResourceToInputStream(path: String): InputStream = classOf[ConfigValidatorTest].getResourceAsStream(path)
}
