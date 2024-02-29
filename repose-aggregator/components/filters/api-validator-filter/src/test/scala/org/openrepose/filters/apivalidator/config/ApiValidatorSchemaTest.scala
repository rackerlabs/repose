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
package org.openrepose.filters.apivalidator.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ApiValidatorSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/validator-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/validator.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should not require enable-rax-roles attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"/>
          |</validators>""".stripMargin
      validator.validateConfigString(xml)
    }

    it("should allow enable-rax-roles attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"
          |        enable-rax-roles="true"/>
          |</validators>
        """.stripMargin
      validator.validateConfigString(xml)
    }

    it("should allow xsd-engine attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"
          |        xsd-engine="Xerces"/>
          |</validators>""".stripMargin
      validator.validateConfigString(xml)
    }

    it("should not allow invalid enable-rax-roles attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" multi-role-match="true">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"
          |        enable-rax-roles="foo"/>
          |</validators>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("is not a valid value for")
    }

    it("should not allow use-saxon attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"
          |        use-saxon="true"/>
          |</validators>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("is not allowed to appear in element")
    }

    it("should not allow version attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0" version="1">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"/>
          |</validators>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("is not allowed to appear in element")
    }

    it("should allow disable-saxon-byte-code-gen attribute") {
      val xml =
        """<validators xmlns="http://docs.openrepose.org/repose/validator/v1.0">
          |    <validator
          |        role="default"
          |        default="true"
          |        wadl="file://my/wadl/file.wadl"
          |        xsd-engine="SaxonEE"
          |        disable-saxon-byte-code-gen="true"/>
          |</validators>""".stripMargin
      validator.validateConfigString(xml)
    }
  }
}
