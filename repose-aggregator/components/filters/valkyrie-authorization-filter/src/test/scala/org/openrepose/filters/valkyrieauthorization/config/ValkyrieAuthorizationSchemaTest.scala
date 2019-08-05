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
package org.openrepose.filters.valkyrieauthorization.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ValkyrieAuthorizationSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/valkyrie-authorization.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/valkyrie-authorization.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    val methods = List("GET", "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE", "ALL")
    (1 to methods.length flatMap (method => methods.combinations(method))).map(methodList => methodList.mkString(" ")).toList.foreach { httpMethods =>
      it(s"should successfully validate if the HTTP Methods list is not empty ($httpMethods)") {
        val config =
          s"""<valkyrie-authorization
              |        xmlns="http://docs.openrepose.org/repose/valkyrie-authorization/v1.0"
              |        cache-timeout-millis="3000">
              |    <valkyrie-server uri="http://localhost" username="UserName" password="Pa$$W0rd"/>
              |    <collection-resources>
              |        <resource>
              |            <path-regex http-methods="$httpMethods">
              |                /path/.*
              |            </path-regex>
              |            <collection>
              |                <json>
              |                    <path-to-collection>
              |                        /path/.*
              |                    </path-to-collection>
              |                    <path-to-device-id>
              |                        <path>/path/.*</path>
              |                        <regex>http://www.rackspace.com/path/</regex>
              |                    </path-to-device-id>
              |                </json>
              |            </collection>
              |        </resource>
              |    </collection-resources>
              |</valkyrie-authorization>""".stripMargin
        validator.
          validateConfigString(config)
      }
    }

    it(s"should fail to validate if the HTTP Methods list is empty") {
      val config =
        s"""<valkyrie-authorization
            |        xmlns="http://docs.openrepose.org/repose/valkyrie-authorization/v1.0"
            |        cache-timeout-millis="3000">
            |    <valkyrie-server uri="http://localhost" username="UserName" password="Pa$$W0rd"/>
            |    <collection-resources>
            |        <resource>
            |            <path-regex http-methods="">
            |                /path/.*
            |            </path-regex>
            |            <collection>
            |                <json>
            |                    <path-to-collection>
            |                        /path/.*
            |                    </path-to-collection>
            |                    <path-to-device-id>
            |                        <path>/path/.*</path>
            |                        <regex>http://www.rackspace.com/path/</regex>
            |                    </path-to-device-id>
            |                </json>
            |            </collection>
            |        </resource>
            |    </collection-resources>
            |</valkyrie-authorization>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("If the http-methods attribute is present, then it must not be empty.")
    }

    it("should fail if username is present and password isn't") {
      val config =
        s"""<valkyrie-authorization
            |        xmlns="http://docs.openrepose.org/repose/valkyrie-authorization/v1.0"
            |        cache-timeout-millis="3000">
            |    <valkyrie-server uri="http://localhost" password="Pa$$W0rd"/>
            |    <collection-resources>
            |        <resource>
            |            <path-regex http-methods="GET">
            |                /path/.*
            |            </path-regex>
            |            <collection>
            |                <json>
            |                    <path-to-collection>
            |                        /path/.*
            |                    </path-to-collection>
            |                    <path-to-device-id>
            |                        <path>/path/.*</path>
            |                        <regex>http://www.rackspace.com/path/</regex>
            |                    </path-to-device-id>
            |                </json>
            |            </collection>
            |        </resource>
            |    </collection-resources>
            |</valkyrie-authorization>""".stripMargin

      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Username and password must be specified or neither.")
    }

    it("should fail if password is present and username isn't") {
      val config =
        s"""<valkyrie-authorization
            |        xmlns="http://docs.openrepose.org/repose/valkyrie-authorization/v1.0"
            |        cache-timeout-millis="3000">
            |    <valkyrie-server uri="http://localhost" username="UserName"/>
            |    <collection-resources>
            |        <resource>
            |            <path-regex http-methods="GET">
            |                /path/.*
            |            </path-regex>
            |            <collection>
            |                <json>
            |                    <path-to-collection>
            |                        /path/.*
            |                    </path-to-collection>
            |                    <path-to-device-id>
            |                        <path>/path/.*</path>
            |                        <regex>http://www.rackspace.com/path/</regex>
            |                    </path-to-device-id>
            |                </json>
            |            </collection>
            |        </resource>
            |    </collection-resources>
            |</valkyrie-authorization>""".stripMargin

      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Username and password must be specified or neither.")
    }

    it("should validate if both username and password are absent") {
      val config =
        s"""<valkyrie-authorization
            |        xmlns="http://docs.openrepose.org/repose/valkyrie-authorization/v1.0"
            |        cache-timeout-millis="3000">
            |    <valkyrie-server uri="http://localhost"/>
            |    <collection-resources>
            |        <resource>
            |            <path-regex http-methods="GET">
            |                /path/.*
            |            </path-regex>
            |            <collection>
            |                <json>
            |                    <path-to-collection>
            |                        /path/.*
            |                    </path-to-collection>
            |                    <path-to-device-id>
            |                        <path>/path/.*</path>
            |                        <regex>http://www.rackspace.com/path/</regex>
            |                    </path-to-device-id>
            |                </json>
            |            </collection>
            |        </resource>
            |    </collection-resources>
            |</valkyrie-authorization>""".stripMargin

      validator.validateConfigString(config)
    }
  }
}
