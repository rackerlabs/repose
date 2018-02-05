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

import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.core.service.opentracing.config.ObjectFactory
import org.xml.sax.SAXParseException

class OpenTracingSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/opentracing.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/opentracing.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate the config with http sender and sampling set to rate limited") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             collector-endpoint="http://localhost:8081">
                     |    <jaeger-sampling-config sample-type="rate-limited">
                     |        <jaeger-sampling-rate-limiting max-traces-per-second="50"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config with http sender and sampling set to probabilistic") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             collector-endpoint="http://localhost:8081">
                     |    <jaeger-sampling-config sample-type="probabilistic">
                     |        <jaeger-sampling-probabilistic value="1.0"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config with http sender and sampling set to const") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             collector-endpoint="http://localhost:8081">
                     |    <jaeger-sampling-config sample-type="const">
                     |        <jaeger-sampling-const value="1"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config with udp sender and sampling set to const") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             sender-protocol="udp">
                     |    <jaeger-sampling-config sample-type="const">
                     |        <jaeger-sampling-const value="1"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config with explicit http sender and sampling set to const") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             sender-protocol="http">
                     |    <jaeger-sampling-config sample-type="const">
                     |        <jaeger-sampling-const value="1"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config with http sender and sampling set to rate limited but sample-type set to const") {
      val config = """<opentracing xmlns='http://docs.openrepose.org/repose/opentracing/v1.0'>
                     |    name="test-repose"
                     |             tracer="jaeger"
                     |             collector-endpoint="http://localhost:8081">
                     |    <jaeger-sampling-config sample-type="const">
                     |        <jaeger-sampling-rate-limiting max-traces-per-second="50"/>
                     |    </jaeger-sampling-config>
                     |</opentracing>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Limit groups must have unique ids")
    }
  }
}
