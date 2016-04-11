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

package org.openrepose.core.service.httpclient.config

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HttpConnectionPoolSchemaTest extends FunSpec with Matchers {
  val validator = ConfigValidator("/META-INF/schema/config/http-connection-pool.xsd")

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/http-connection-pool.cfg.xml")
    }

    it("should successfully validate config containing pools with unique IDs and one default pool") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if any of the pools have the same ID") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="apple"/>
                     |</http-connection-pools>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("Pools must have unique ids")
    }

    it("should reject config if there are no default pools") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="false"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("One and only one default pool must be defined")
    }

    it("should reject config if there is more than one default pool") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="true"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("One and only one default pool must be defined")
    }
  }
}
