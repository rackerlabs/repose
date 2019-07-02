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

package org.openrepose.filters.headertranslation.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HeaderTranslationSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/header-translation.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/header-translation.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should reject config with non-unique Original Names (case insensitive)") {
      val config = """<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Type" new-name="rax-content-type"/>
                     |    <header original-name="Content-Type" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }

    it("should reject config with non-unique Original Names (case sensitive)") {
      val config = """<header-translation xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     |                    xsi:schemaLocation="http://docs.openrepose.org/repose/header-translation/v1.0"
                     |                    xmlns="http://docs.openrepose.org/repose/header-translation/v1.0">
                     |    <header original-name="Content-Type" new-name="rax-content-type"/>
                     |    <header original-name="content-type" new-name="rax-content-length"/>
                     |</header-translation>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Original names must be unique. Evaluation is case insensitive.")
    }
  }
}
