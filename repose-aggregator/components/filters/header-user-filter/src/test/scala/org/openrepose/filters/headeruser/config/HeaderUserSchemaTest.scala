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

package org.openrepose.filters.headeruser.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HeaderUserSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/header-user-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/header-user.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config containing headers with unique IDs") {
      val config = """<header-user xmlns='http://docs.openrepose.org/repose/header-user/v1.0'>
                     |    <source-headers>
                     |        <header id="X-Auth-Token"/>
                     |        <header id="X-Forwarded-For" quality="0.2"/>
                     |    </source-headers>
                     |</header-user>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if any of the headers have the same ID") {
      val config = """<header-user xmlns='http://docs.openrepose.org/repose/header-user/v1.0'>
                     |    <source-headers>
                     |        <header id="X-Auth-Token"/>
                     |        <header id="X-Forwarded-For" quality="0.2"/>
                     |        <header id="X-Auth-Token" quality="0.3"/>
                     |    </source-headers>
                     |</header-user>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Headers must have ids unique within their containing header list")
    }
  }
}
