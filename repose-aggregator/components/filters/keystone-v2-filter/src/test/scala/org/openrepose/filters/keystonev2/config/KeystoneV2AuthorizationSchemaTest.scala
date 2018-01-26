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
package org.openrepose.filters.keystonev2.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class KeystoneV2AuthorizationSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/keystone-v2-authorization.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/keystone-v2-authorization.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config if a tenant URI extraction regex is provided") {
      val config =
        """<keystone-v2-authorization xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <tenant-handling>
          |        <validate-tenant>
          |            <uri-extraction-regex>[^\/]*\/?([^\/]+)</uri-extraction-regex>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2-authorization>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config if a tenant header extraction name is provided") {
      val config =
        """<keystone-v2-authorization xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <tenant-handling>
          |        <validate-tenant>
          |            <header-extraction-name>x-expected-tenant</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2-authorization>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config if both a tenant URI extraction regex and a tenant header extraction name are provided") {
      val config =
        """<keystone-v2-authorization xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <tenant-handling>
          |        <validate-tenant>
          |            <uri-extraction-regex>[^\/]*\/?([^\/]+)</uri-extraction-regex>
          |            <header-extraction-name>x-expected-tenant</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2-authorization>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if neither a tenant URI extraction regex nor a tenant header extraction name is provided") {
      val config =
        """<keystone-v2-authorization xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <tenant-handling>
          |        <validate-tenant/>
          |    </tenant-handling>
          |</keystone-v2-authorization>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("When validating tenants, at least one tenant extraction must be defined")
    }
  }
}
