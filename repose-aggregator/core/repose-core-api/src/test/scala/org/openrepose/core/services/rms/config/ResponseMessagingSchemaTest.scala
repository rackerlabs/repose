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

package org.openrepose.core.services.rms.config

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class ResponseMessagingSchemaTest extends FunSpec with Matchers {
  val validator = ConfigValidator("/META-INF/schema/response-messaging/response-messaging.xsd")

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/response-messaging.cfg.xml")
    }

    it("should successfully validate config when the status code IDs are unique") {
      val config = """<response-messaging xmlns="http://docs.openrepose.org/repose/response-messaging/v1.0">
                     |    <status-code id="1" code-regex="404" overwrite="ALWAYS">
                     |        <message media-type="application/json" href="link_to_message.json"/>
                     |    </status-code>
                     |    <status-code id="2" code-regex="502" overwrite="ALWAYS">
                     |        <message media-type="application/json" href="link_to_message.json"/>
                     |    </status-code>
                     |</response-messaging>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when the status code IDs are not unique") {
      val config = """<response-messaging xmlns="http://docs.openrepose.org/repose/response-messaging/v1.0">
                     |    <status-code id="2" code-regex="404" overwrite="ALWAYS">
                     |        <message media-type="application/json" href="link_to_message.json"/>
                     |    </status-code>
                     |    <status-code id="2" code-regex="502" overwrite="ALWAYS">
                     |        <message media-type="application/json" href="link_to_message.json"/>
                     |    </status-code>
                     |</response-messaging>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Status code ids must be unique")
    }

    it("should successfully validate config when the message is either inline or specified using an href link") {
      val config = """<response-messaging xmlns="http://docs.openrepose.org/repose/response-messaging/v1.0">
                     |    <status-code id="1" code-regex="413">
                     |        <message media-type="*/*" content-type="application/json">
                     |            {
                     |            "overLimit" : {
                     |            "code" : 413,
                     |            "message" : "OverLimit Retry...",
                     |            "details" : "Error Details...",
                     |            "retryAfter" : "%{Retry-After DATE ISO_8601}o"
                     |            }
                     |            }
                     |        </message>
                     |    </status-code>
                     |    <status-code id="2" code-regex="404" overwrite="ALWAYS">
                     |        <message media-type="application/json" href="link_to_message.json"/>
                     |    </status-code>
                     |</response-messaging>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if a message is both inline and specified using an href link") {
      val config = """<response-messaging xmlns="http://docs.openrepose.org/repose/response-messaging/v1.0">
                     |    <status-code id="1" code-regex="413">
                     |        <message media-type="*/*" content-type="application/json" href="link_to_message.json">
                     |            {
                     |            "overLimit" : {
                     |            "code" : 413,
                     |            "message" : "OverLimit Retry...",
                     |            "details" : "Error Details...",
                     |            "retryAfter" : "%{Retry-After DATE ISO_8601}o"
                     |            }
                     |            }
                     |        </message>
                     |    </status-code>
                     |</response-messaging>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Cannot define message inline and reference to external message file")
    }
  }
}
