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
package org.openrepose.filters.regexrbac.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class RegexRbacSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/regex-rbac.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/regex-rbac.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate the config when resources are specified inline") {
      val config =
        """<regex-rbac xmlns="http://docs.openrepose.org/repose/regex-rbac/v1.0"
          |             mask-rax-roles-403="false">
          |    <resources>
          |/path/to/this   GET     role1,role2,role3,role4
          |/path/to/this   PUT     role1,role2,role3
          |/path/to/this   POST    role1,role2
          |/path/to/this   DELETE  role1
          |/path/to/that   GET,PUT ALL
          |/path/to/that   ALL     role1
          |/path/[^/]+/.*  GET     role1
          |    </resources>
          |</regex-rbac>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config when resources are specified using an href link") {
      val config =
        """<regex-rbac xmlns="http://docs.openrepose.org/repose/regex-rbac/v1.0"
          |             mask-rax-roles-403="false">
          |    <resources href="some_file"/>
          |</regex-rbac>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when resources are specified inline and using an href link") {
      val config =
        """<regex-rbac xmlns="http://docs.openrepose.org/repose/regex-rbac/v1.0"
          |             mask-rax-roles-403="false">
          |    <resources href="some_file">
          |/path/to/this   GET     role1,role2,role3,role4
          |/path/to/this   PUT     role1,role2,role3
          |/path/to/this   POST    role1,role2
          |/path/to/this   DELETE  role1
          |/path/to/that   GET,PUT ALL
          |/path/to/that   ALL     role1
          |/path/[^/]+/.*  GET     role1
          |    </resources>
          |</regex-rbac>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("Cannot define message inline and reference to external message file")
    }
  }
}
