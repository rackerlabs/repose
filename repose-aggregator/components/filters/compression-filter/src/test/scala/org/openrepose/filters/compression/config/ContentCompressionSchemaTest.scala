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

package org.openrepose.filters.compression.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ContentCompressionSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/content-compression-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/compression.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate if only include-content-types is specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024" include-content-types="application/xml text/html"/>
                     |</content-compression>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate if only exclude-content-types is specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024" exclude-content-types="image/jpeg image/png"/>
                     |</content-compression>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate if only include-user-agent-patterns is specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024" include-user-agent-patterns=".*Chrome/.*"/>
                     |</content-compression>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate if only exclude-user-agent-patterns is specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024" exclude-user-agent-patterns=".*MSIE 6.*"/>
                     |</content-compression>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if both include-content-types and exclude-content-types are specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024"
                     |        include-content-types="application/xml text/html"
                     |        exclude-content-types="image/jpeg image/png"/>
                     |</content-compression>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Enumerating include-content-type and exclude-content-type is not allowed.")
    }

    it("should reject config if both include-user-agent-patterns and exclude-user-agent-patterns are specified") {
      val config = """<content-compression xmlns="http://docs.openrepose.org/repose/content-compression/v1.0">
                     |    <compression compression-threshold="1024"
                     |        include-user-agent-patterns=".*Chrome/.*"
                     |        exclude-user-agent-patterns=".*MSIE 6.*"/>
                     |</content-compression>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Enumerating include-user-agent-patterns and exclude-user-agent-patterns is not allowed.")
    }
  }

}
