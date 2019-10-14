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

package org.openrepose.filters.slf4jlogging.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class Slf4jHttpLoggingSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/slf4j-http-logging-configuration.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/slf4j-http-logging.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate the config when the log format is either an attribute or an element but not both") {
      val config = """<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
                     |    <slf4j-http-log
                     |            id="my-special-log"
                     |            format="Response Time=%T seconds\tResponse Time=%D microseconds"/>
                     |    <slf4j-http-log id="my-other-log">
                     |        <format>
                     |            <![CDATA[
                     |            { "received": "%t", "duration": "%T"" }
                     |            ]]>
                     |        </format>
                     |    </slf4j-http-log>
                     |</slf4j-http-logging>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when a log has a format specified both by an attribute and an element") {
      val config = """<slf4j-http-logging xmlns="http://docs.openrepose.org/repose/slf4j-http-logging/v1.0">
                     |    <slf4j-http-log id="my-other-log" format="Response Time=%T seconds\tResponse Time=%D microseconds">
                     |        <format>
                     |            <![CDATA[
                     |            { "received": "%t", "duration": "%T"" }
                     |            ]]>
                     |        </format>
                     |    </slf4j-http-log>
                     |</slf4j-http-logging>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Cannot define format as both an attribute and as an element")
    }
  }
}
