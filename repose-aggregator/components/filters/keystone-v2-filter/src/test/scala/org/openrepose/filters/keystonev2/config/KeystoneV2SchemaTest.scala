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
package org.openrepose.filters.keystonev2.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class KeystoneV2SchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/keystone-v2.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config if both username and password are provided") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com" username="bob" password="GoAwayEve"/>
                     |</keystone-v2>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config if neither username and password are provided") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com"/>
                     |</keystone-v2>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config with only a username but no password is provided") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com" username="bob"/>
                     |</keystone-v2>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Must provide both a username and a password")
    }

    it("should reject config with only a password but no username is provided") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com" password="GoAwayEve"/>
                     |</keystone-v2>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Must provide both a username and a password")
    }

    it("should successfully validate config if all Atom Feed IDs are unique") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com"/>
                     |    <cache>
                     |        <atom-feed id="some-feed"/>
                     |        <atom-feed id="another-feed"/>
                     |    </cache>
                     |</keystone-v2>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if two Atom Feed IDs are the same") {
      val config = """<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
                     |    <identity-service uri="https://some.identity.com"/>
                     |    <cache>
                     |        <atom-feed id="some-feed"/>
                     |        <atom-feed id="some-feed"/>
                     |    </cache>
                     |</keystone-v2>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Atom Feed ID's must be unique")
    }
  }
}
