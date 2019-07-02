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
package org.openrepose.filters.versioning.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class VersioningSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/versioning-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/versioning.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config if version mapping IDs are unique") {
      val config = """<versioning xmlns="http://docs.openrepose.org/repose/versioning/v2.0">
                     |    <version-mapping id="foo" pp-dest-id="foo" status="CURRENT"/>
                     |    <version-mapping id="bar" pp-dest-id="bar" status="CURRENT"/>
                     |</versioning>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if two version mapping IDs are the same") {
      val config = """<versioning xmlns="http://docs.openrepose.org/repose/versioning/v2.0">
                     |    <version-mapping id="foo" pp-dest-id="foo-one" status="DEPRECATED"/>
                     |    <version-mapping id="foo" pp-dest-id="foo-two" status="CURRENT"/>
                     |</versioning>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Version mapping must have ids unique within their containing filter list")
    }
  }
}
