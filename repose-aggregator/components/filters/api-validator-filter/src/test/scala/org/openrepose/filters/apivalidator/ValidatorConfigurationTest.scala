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
package org.openrepose.filters.apivalidator

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.filters.apivalidator.config.ObjectFactory
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ValidatorConfigurationTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/validator-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/validator.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it(s"should successfully validate when there is only one default and the validator-names, dot-output, & ... are uniqe") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |  <validator role="notDefault"
            |             default="false"
            |             validator-name="notDefaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/notDefault.dot"
            |  />
            |</validators>""".stripMargin
      validator.validateConfigString(config)
    }

    it(s"should fail to validate if the dot-output files are not unique") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |  <validator role="notDefault"
            |             default="false"
            |             validator-name="notDefaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Dot output files must be unique")
    }

    it(s"should fail to validate if the the validator-name's are not unique") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |  <validator role="notDefault"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/notDefault.dot"
            |  />
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Validator names must be unique")
    }

    it(s"should fail to validate if there is more than one default validator defined") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |  <validator role="notDefault"
            |             default="true"
            |             validator-name="notDefaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/notDefault.dot"
            |  />
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Only one default validator may be defined")
    }

    it(s"should fail to validate when role's are not unique") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  />
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Roles list must contain unique roles")
    }

    it(s"should fail to validate when both a wadl file and embedded wadl are defined") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             wadl="file://my/wadl/file.wadl"
            |             dot-output="/tmp/default.dot"
            |  >
            |      <stuff>This is some bogus content that is presumed to be an embedded WADL.</stuff>
            |  </validator>
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Cannot define wadl file and embedded wadl")
    }

    it(s"should fail to validate when neither a wadl file or embedded wadl are defined") {
      val config =
        s"""<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
            |  <validator role="default"
            |             default="true"
            |             validator-name="defaultValidator"
            |             dot-output="/tmp/default.dot"
            |  />
            |</validators>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Must define a wadl file OR an embedded wadl")
    }
  }
}
