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
package org.openrepose.core.services.opentracing.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.core.service.opentracing.config.ObjectFactory
import org.scalatestplus.junit.JUnitRunner

import scala.xml.SAXParseException

@RunWith(classOf[JUnitRunner])
class OpenTracingSchemaTest extends ConfigurationTest {
    override val schema: URL = getClass.getResource("/META-INF/schema/config/open-tracing.xsd")
    override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/open-tracing.cfg.xml")
    override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should validate with username and password") {
      val config = buildConfig(""" username="bob" password="butts" """)
      validator.
        validateConfigString(config)
    }

    it("should validate with a token") {
      val config = buildConfig(""" token="butts" """)
      validator.
        validateConfigString(config)
    }

    it("should validate with no auth") {
      val config = buildConfig("")
      validator.
        validateConfigString(config)
    }

    it("should fail for username without password") {
      val config = buildConfig(""" username="butts" """)
      intercept[SAXParseException] {
        validator.
          validateConfigString(config)
      }
    }

    it("should fail for password without username") {
      val config = buildConfig(""" password="butts" """)
      intercept[SAXParseException] {
        validator.
          validateConfigString(config)
      }
    }

    it("should fail for username/password and token") {
      val config = buildConfig(""" username="bob" password="butts" token="banana" """)
      intercept[SAXParseException] {
        validator.
          validateConfigString(config)
      }
    }
  }

  def buildConfig(settings: String): String =
    s"""<open-tracing xmlns="http://docs.openrepose.org/repose/open-tracing-service/v1.0"
       |              service-name="test-repose">
       |    <jaeger>
       |        <connection-http endpoint="http://localhost:14268/path"
       |                         $settings
       |                         />
       |        <sampling-rate-limiting max-traces-per-second="50"/>
       |    </jaeger>
       |</open-tracing>""".stripMargin
}
