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
package org.openrepose.filters.uriuser.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class UriUserSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/uri-user-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/uri-user.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config if no mapping IDs are provided") {
      val config = """<uri-user xmlns='http://docs.openrepose.org/repose/uri-user/v1.0'>
                     |    <identification-mappings>
                     |        <mapping identification-regex="/foo*"/>
                     |        <mapping identification-regex="/bar"/>
                     |    </identification-mappings>
                     |</uri-user>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config if mapping IDs are unique") {
      val config = """<uri-user xmlns='http://docs.openrepose.org/repose/uri-user/v1.0'>
                     |    <identification-mappings>
                     |        <mapping id="foo" identification-regex="/foo*"/>
                     |        <mapping id="bar" identification-regex="/bar"/>
                     |    </identification-mappings>
                     |</uri-user>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if two mapping IDs are the same") {
      val config = """<uri-user xmlns='http://docs.openrepose.org/repose/uri-user/v1.0'>
                     |    <identification-mappings>
                     |        <mapping id="foo" identification-regex="/foo/one"/>
                     |        <mapping id="foo" identification-regex="/foo/two"/>
                     |    </identification-mappings>
                     |</uri-user>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Mappings ids must be unique if specified")
    }
  }
}
