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

package org.openrepose.core.services.ratelimit.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class RateLimitingSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/rate-limiting-configuration.xsd")
  override val auxiliarySchemas: Seq[URL] = Seq("/META-INF/schema/limits/limits.xsd").map(getClass.getResource)
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/rate-limiting.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate the config when the limit groups have unique IDs, groups are unique, and only one limit group is marked as default") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer foo' default='true'/>
                     |    <limit-group id='test-limits' groups='user' default='false'/>
                     |</rate-limiting>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config with limit groups that don't have unique IDs") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='test-limits' groups='customer foo' default='true'/>
                     |    <limit-group id='test-limits' groups='user'/>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Limit groups must have unique ids")
    }

    it("should reject the config when multiple limit groups specify the same group") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer' default='true'/>
                     |    <limit-group id='test-limits' groups='customer'/>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("The same group cannot be specified by multiple limit groups")
    }

    it("should reject the config when multiple limit groups are marked as default") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer foo' default='true'/>
                     |    <limit-group id='test-limits' groups='user' default='true'/>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Only one default limit group may be defined")
    }

    it("should successfully validate the config if limit IDs are unique across groups") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer foo'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>
                     |        <limit id='two' uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |    <limit-group id='customer-limits2' groups='customer2'>
                     |        <limit id='three' uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>
                     |        <limit id='four' uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |</rate-limiting>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when any two limits have the same ID") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>
                     |        <limit id='one' uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Limits must have unique ids")
    }

    it("should reject the config when any two limits across groups have the same ID") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='customer-limits' groups='customer foo'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>
                     |        <limit id='two' uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |    <limit-group id='customer-limits2' groups='customer2'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='ALL' value='1' unit='HOUR'/>
                     |        <limit id='two' uri='foo2' uri-regex='foo2' http-methods='ALL' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Limits must have unique ids")
    }

    val methods = List("GET", "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE", "ALL")
    (1 to methods.length flatMap methods.combinations).map(_.mkString(" ")) foreach { httpMethods =>
      it(s"should successfully validate the config with HTTP methods $httpMethods") {
        val config = s"""<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                        |    <limit-group id='test-limits' groups='customer foo' default='true'>
                        |        <limit id='one' uri='foo' uri-regex='foo' http-methods='$httpMethods' value='1' unit='HOUR'/>
                        |    </limit-group>
                        |    <limit-group id='customer-limits' groups='user'/>
                        |</rate-limiting>""".stripMargin
        validator.validateConfigString(config)
      }
    }

    it("should successfully validate the config with duplicate HTTP methods and different URI regexes") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='test-limits' groups='customer foo' default='true'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>
                     |        <limit id='two' uri='foo' uri-regex='bar' http-methods='GET PUT' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |    <limit-group id='customer-limits' groups='user'/>
                     |</rate-limiting>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config with duplicate URI regexes and different HTTP methods") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='test-limits' groups='customer foo' default='true'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='GET PUT' value='1' unit='HOUR'/>
                     |        <limit id='two' uri='foo' uri-regex='foo' http-methods='POST DELETE' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |    <limit-group id='customer-limits' groups='user'/>
                     |</rate-limiting>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when an invalid HTTP method is used") {
      val config = """<rate-limiting xmlns='http://docs.openrepose.org/repose/rate-limiting/v1.0'>
                     |    <limit-group id='test-limits' groups='customer foo' default='true'>
                     |        <limit id='one' uri='foo' uri-regex='foo' http-methods='FOO' value='1' unit='HOUR'/>
                     |    </limit-group>
                     |    <limit-group id='customer-limits' groups='user'/>
                     |</rate-limiting>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("It must be a value from the enumeration.")
    }

    it("should validate against live limits example") {
      validator.validateConfigFile("/META-INF/schema/examples/limits.xml")
    }
  }
}
